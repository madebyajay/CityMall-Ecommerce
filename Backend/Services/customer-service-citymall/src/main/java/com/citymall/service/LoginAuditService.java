package com.citymall.service;

import com.citymall.entity.LoginAudit;
import com.citymall.entity.LoginStatus;
import com.citymall.repository.LoginAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Append-only login audit (ported from user-management
 * {@code saveUserLoggedAuditTrail} with {@code REQUIRES_NEW} so audit rows
 * survive outer-transaction rollbacks).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAuditService {

    private final LoginAuditRepository auditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void audit(Long userId, LoginStatus status, String remarks, String clientIp) {
        try {
            auditRepository.save(LoginAudit.builder()
                    .userId(userId)
                    .status(status)
                    .remarks(remarks)
                    .clientIpAddress(clientIp)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write login audit for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Counts consecutive FAILED attempts (most-recent-first, stops at the first
     * non-FAILED entry) — mirrors the reference top-N consecutive-failure count.
     */
    @Transactional(readOnly = true)
    public long consecutiveFailures(Long userId) {
        List<LoginAudit> recent = auditRepository.findTop10ByUserIdOrderByActionDateDesc(userId);
        long failures = 0;
        for (LoginAudit entry : recent) {
            if (entry.getStatus() == LoginStatus.FAILED) failures++;
            else break;
        }
        return failures;
    }
}
