package com.citymall.service;

import com.citymall.config.OtpProperties;
import com.citymall.exception.BadRequestException;
import com.citymall.exception.BusinessException;
import com.citymall.exception.UnauthorizedException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * OTP issuance + verification (ported from user-management
 * {@code generateOtpAndStoreIntoCache}: 6-digit code, max 5 generations per key,
 * 5-minute TTL, single-use consume).
 *
 * <p>Delivery is a log stub today (MailDev on :1025 / SMS gateway plug in here).
 *
 * <p>When Redis is unavailable (plain local dev), OTP state falls back to an
 * in-memory map so the flow keeps working on a single instance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final RedisSessionService sessions;
    private final OtpProperties otpProps;
    private final SecureRandom random = new SecureRandom();
    /** Dev fallback when Redis is down — single-instance only. */
    private final java.util.concurrent.ConcurrentHashMap<String, OtpState> memoryFallback =
            new java.util.concurrent.ConcurrentHashMap<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtpState {
        private String otp;
        private int attempts;
        private LocalDateTime expiresAt;
    }

    /** Issue (or re-issue) an OTP for an arbitrary key (email, mobile or user id). */
    public String requestOtp(String key) {
        OtpState state = readState(key);
        int attempts = state != null ? state.getAttempts() : 0;
        if (attempts >= otpProps.getLimit()) {
            throw new BusinessException(
                    "OTP limit exceeded for this account. Try again after "
                            + otpProps.getExpiryMinutes() + " minutes.");
        }
        String code = generateCode(otpProps.getLength());
        OtpState next = OtpState.builder()
                .otp(code)
                .attempts(attempts + 1)
                .expiresAt(LocalDateTime.now().plusMinutes(otpProps.getExpiryMinutes()))
                .build();
        sessions.putOtp(key, next, otpProps.getExpiryMinutes());
        if (!sessions.ping()) memoryFallback.put(key, next);
        // TODO: send via MailDev/SMS gateway. Until then the code is logged for dev.
        log.info("OTP for key {}: {} (attempt {}/{})", key, code, attempts + 1, otpProps.getLimit());
        return "OTP sent successfully.";
    }

    /** Verify and consume an OTP; throws when missing/expired/mismatched. */
    public void verifyOtp(String key, String otp) {
        OtpState state = readState(key);
        if (state == null || state.getOtp() == null) {
            throw new UnauthorizedException("No OTP requested for this account. Request a new OTP.");
        }
        if (state.getExpiresAt() != null && LocalDateTime.now().isAfter(state.getExpiresAt())) {
            evictState(key);
            throw new UnauthorizedException("OTP expired. Request a new OTP.");
        }
        if (!state.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP.");
        }
        evictState(key);
    }

    private OtpState readState(String key) {
        OtpState state = sessions.getOtp(key, OtpState.class);
        if (state != null) return state;
        state = memoryFallback.get(key);
        if (state != null && state.getExpiresAt() != null
                && LocalDateTime.now().isAfter(state.getExpiresAt())) {
            memoryFallback.remove(key);
            return null;
        }
        return state;
    }

    private void evictState(String key) {
        sessions.evictOtp(key);
        memoryFallback.remove(key);
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(random.nextInt(10));
        return sb.toString();
    }
}
