package com.citymall.service;

import com.citymall.entity.PasswordHistory;
import com.citymall.repository.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password reuse guard (ported from user-management
 * {@code user_password_history} + last-3 check).
 */
@Service
@RequiredArgsConstructor
public class PasswordHistoryService {

    private final PasswordHistoryRepository historyRepository;

    @Transactional
    public void record(Long userId, String passwordHash) {
        historyRepository.save(PasswordHistory.builder()
                .userId(userId)
                .passwordHash(passwordHash)
                .build());
    }

    @Transactional(readOnly = true)
    public boolean isReused(Long userId, String rawPassword, PasswordEncoder encoder) {
        return historyRepository.findTop3ByUserIdOrderByCreatedAtDesc(userId).stream()
                .anyMatch(h -> encoder.matches(rawPassword, h.getPasswordHash()));
    }
}
