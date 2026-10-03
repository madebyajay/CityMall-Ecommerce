package com.citymall.service;

import com.citymall.config.SessionProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Redis-backed session + permission cache (ported from user-management
 * {@code RedisCacheService} + {@code LOGIN:/REFRESH:/OTP:/ROLE_PERMISSION:} keys).
 *
 * <p>All calls degrade gracefully: if Redis is down the service logs a warning
 * and behaves stateless (sessions treated as active, permissions from DB).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSessionService {

    public static final String LOGIN_PREFIX = "LOGIN:";
    public static final String REFRESH_PREFIX = "REFRESH:";
    public static final String OTP_PREFIX = "OTP:";
    public static final String ROLE_PERMISSION_PREFIX = "ROLE_PERMISSION:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SessionProperties sessionProps;

    // ---------------- sessions ----------------

    public void saveSession(Long userId, String accessToken, String refreshToken) {
        safe(() -> {
            redisTemplate.opsForValue().set(LOGIN_PREFIX + userId, accessToken,
                    Duration.ofMinutes(sessionProps.getAccessTtlMinutes()));
            redisTemplate.opsForValue().set(REFRESH_PREFIX + userId, refreshToken,
                    Duration.ofDays(sessionProps.getRefreshTtlDays()));
        });
    }

    public boolean isAccessTokenActive(Long userId, String token) {
        Object cached = safeGet(LOGIN_PREFIX + userId);
        // No entry (logged out / expired / Redis down) — fail closed only when we
        // positively hold a *different* token; absent cache degrades to stateless.
        if (cached == null) return true;
        return token.equals(cached.toString());
    }

    public boolean isRefreshTokenActive(Long userId, String token) {
        Object cached = safeGet(REFRESH_PREFIX + userId);
        // No entry: revoked when Redis is reachable; JWT-only validation when Redis is down.
        if (cached == null) return !ping();
        return token.equals(cached.toString());
    }

    public boolean hasActiveSession(Long userId) {
        return safeGet(LOGIN_PREFIX + userId) != null;
    }

    public void revokeSession(Long userId) {
        safe(() -> {
            redisTemplate.delete(LOGIN_PREFIX + userId);
            redisTemplate.delete(REFRESH_PREFIX + userId);
        });
    }

    /** Cheap liveness probe — false when Redis is down (dev without redis). */
    public boolean ping() {
        try {
            return redisTemplate.getConnectionFactory() != null
                    && redisTemplate.getConnectionFactory().getConnection().ping() != null;
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------- generic OTP state ----------------
    public void putOtp(String key, Object value, long expiryMinutes) {
        safe(() -> redisTemplate.opsForValue()
                .set(OTP_PREFIX + key, value, Duration.ofMinutes(expiryMinutes)));
    }

    @SuppressWarnings("unchecked")
    public <T> T getOtp(String key, Class<T> type) {
        Object raw = safeGet(OTP_PREFIX + key);
        if (raw == null) return null;
        if (type.isInstance(raw)) return (T) raw;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper.convertValue(raw, type);
        } catch (Exception e) {
            log.warn("OTP cache deserialization failed for key {}", key);
            return null;
        }
    }

    public void evictOtp(String key) {
        safe(() -> redisTemplate.delete(OTP_PREFIX + key));
    }

    // ---------------- permission cache ----------------

    @SuppressWarnings("unchecked")
    public List<String> getCachedPermissions(String roleName) {
        Object raw = safeGet(ROLE_PERMISSION_PREFIX + roleName.toUpperCase());
        if (raw instanceof List<?> list && !list.isEmpty()) {
            return (List<String>) list;
        }
        return null;
    }

    public void cachePermissions(String roleName, List<String> endpoints) {
        safe(() -> redisTemplate.opsForValue()
                .set(ROLE_PERMISSION_PREFIX + roleName.toUpperCase(), endpoints, Duration.ofDays(1)));
    }

    public void evictPermissions(String roleName) {
        safe(() -> redisTemplate.delete(ROLE_PERMISSION_PREFIX + roleName.toUpperCase()));
    }

    // ---------------- resilience helpers ----------------

    private Object safeGet(String key) {
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis unavailable (GET {}), degrading to stateless: {}", key, e.getMessage());
            return null;
        }
    }

    private void safe(Runnable op) {
        try {
            op.run();
        } catch (Exception e) {
            log.warn("Redis unavailable, degrading to stateless: {}", e.getMessage());
        }
    }
}
