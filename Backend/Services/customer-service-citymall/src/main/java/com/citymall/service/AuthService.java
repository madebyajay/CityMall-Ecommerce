package com.citymall.service;

import com.citymall.config.JwtProperties;
import com.citymall.dto.auth.AuthResponse;
import com.citymall.dto.auth.LoginRequest;
import com.citymall.dto.auth.RefreshTokenRequest;
import com.citymall.dto.auth.RegisterRequest;
import com.citymall.dto.auth.ResetPasswordRequest;
import com.citymall.entity.LoginStatus;
import com.citymall.entity.Role;
import com.citymall.entity.User;
import com.citymall.entity.UserStatus;
import com.citymall.exception.BadRequestException;
import com.citymall.exception.BusinessException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.exception.UnauthorizedException;
import com.citymall.mapper.UserMapper;
import com.citymall.repository.RoleRepository;
import com.citymall.repository.UserRepository;
import com.citymall.security.CustomUserDetails;
import com.citymall.security.CustomUserDetailsService;
import com.citymall.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Auth = register + login + refresh + logout + OTP password reset.
 * Session handling (single-device LOGIN:/REFRESH: keys) and login audit are
 * ported from the reference user-management service; passwords stay BCrypt.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Roles that may never self-register — they are created by ADMIN/SUPERADMIN flows. */
    private static final Set<String> SELF_REGISTER_BLOCKED = Set.of("ADMIN", "SUPERADMIN");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserMapper mapper;
    private final JwtProperties jwtProps;
    private final RedisSessionService sessions;
    private final LoginAuditService auditService;
    private final PasswordHistoryService passwordHistoryService;
    private final OtpService otpService;

    @Transactional
    public AuthResponse register(RegisterRequest req, String clientIp) {
        String loginId = req.userLoginId().trim();
        String email = req.emailId() != null ? req.emailId().trim().toLowerCase() : null;

        if (userRepository.existsByUserLoginId(loginId)) {
            throw new BusinessException("Login id already taken: " + loginId);
        }
        if (email != null && !email.isBlank() && userRepository.existsByEmailId(email)) {
            throw new BusinessException("Email already registered: " + email);
        }
        if (userRepository.existsByMobileNumber(req.mobileNumber())) {
            throw new BusinessException("Mobile number already registered: " + req.mobileNumber());
        }

        String roleName = (req.roleName() == null || req.roleName().isBlank())
                ? "CUSTOMER" : req.roleName().trim().toUpperCase();
        if (SELF_REGISTER_BLOCKED.contains(roleName)) {
            throw new BadRequestException("Cannot self-register as " + roleName);
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFound("Role not found: " + roleName));
        if (Boolean.FALSE.equals(role.getActiveStatus())) {
            throw new BadRequestException("Role is inactive: " + roleName);
        }

        String displayName = req.userName() != null && !req.userName().isBlank()
                ? req.userName().trim()
                : (req.firstName() + " " + (req.lastName() != null ? req.lastName() : "")).trim();

        User user = User.builder()
                .userLoginId(loginId)
                .password(passwordEncoder.encode(req.password()))
                .userName(displayName)
                .firstName(req.firstName())
                .lastName(req.lastName())
                .emailId(email)
                .mobileNumber(req.mobileNumber())
                .role(role)
                .status(UserStatus.ACTIVE)
                .activeStatus(true)
                .failedLoginAttempts(0)
                .clientIpAddress(clientIp)
                .build();

        User saved = userRepository.save(user);
        passwordHistoryService.record(saved.getUserId(), saved.getPassword());
        auditService.audit(saved.getUserId(), LoginStatus.SUCCESS, "REGISTER", clientIp);
        log.info("Registered user {} with role {}", loginId, roleName);

        return issueSession(saved, false);
    }

    /** Shared login core — single-session guard, audit trail, lockout. */
    @Transactional
    public AuthResponse authenticate(LoginRequest req, String clientIp) {
        User user = userRepository.findByLoginIdentifier(req.identifier().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        assertNotLocked(user);

        if (!user.isAccountUsable()) {
            auditService.audit(user.getUserId(), LoginStatus.FAILED, "DISABLED_ACCOUNT", clientIp);
            throw new UnauthorizedException("Account is disabled. Contact support.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            handleFailedPassword(user, clientIp);
            throw new UnauthorizedException("Invalid credentials");
        }

        boolean forced = false;
        if (sessions.hasActiveSession(user.getUserId())
                && !Boolean.TRUE.equals(req.forcedLogin())) {
            auditService.audit(user.getUserId(), LoginStatus.FAILED, "ACTIVE_SESSION_EXISTS", clientIp);
            throw new BusinessException(
                    "Account is already logged in on another device. "
                            + "Retry with forcedLogin=true to continue here.");
        }
        if (Boolean.TRUE.equals(req.forcedLogin()) && sessions.hasActiveSession(user.getUserId())) {
            forced = true;
            sessions.revokeSession(user.getUserId());
            log.info("Forced login kicked previous session for user {}", user.getUserLoginId());
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(LocalDateTime.now());
        user.setClientIpAddress(clientIp);
        User saved = userRepository.save(user);
        auditService.audit(saved.getUserId(), LoginStatus.SUCCESS,
                forced ? "LOGIN_FORCED" : "LOGIN", clientIp);

        log.info("User logged in: {}", saved.getUserLoginId());
        return issueSession(saved, forced);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest req) {
        String token = req.refreshToken();
        if (!jwtService.isValid(token) || !jwtService.isRefreshToken(token)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        Long userId = jwtService.extractUserId(token);
        CustomUserDetails principal;
        User user;
        if (userId == null) {
            String loginId = jwtService.extractUsername(token);
            principal = (CustomUserDetails) userDetailsService.loadUserByUsername(loginId);
            user = userRepository.findByUserLoginId(loginId)
                    .orElseThrow(() -> new ResourceNotFound("User not found: " + loginId));
            userId = user.getUserId();
        } else {
            principal = userDetailsService.loadByUserId(userId);
            final Long uid = userId;
            user = userRepository.findById(uid)
                    .orElseThrow(() -> new ResourceNotFound("User not found with id: " + uid));
        }
        // Refresh-token allowlist: rotate only when the presented token is the stored one.
        if (!sessions.isRefreshTokenActive(userId, token)) {
            throw new UnauthorizedException("Refresh token revoked. Please login again.");
        }
        if (!user.isAccountUsable()) {
            throw new UnauthorizedException("Account is disabled");
        }
        return issueSession(user, false);
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                if (jwtService.isValid(token) && !jwtService.isRefreshToken(token)) {
                    Long userId = jwtService.extractUserId(token);
                    if (userId == null) {
                        String loginId = jwtService.extractUsername(token);
                        userId = userRepository.findByUserLoginId(loginId)
                                .map(User::getUserId).orElse(null);
                    }
                    if (userId != null) {
                        sessions.revokeSession(userId);
                        auditService.audit(userId, LoginStatus.LOGOUT, "LOGOUT", null);
                        log.info("Session revoked for user {}", userId);
                        return;
                    }
                }
            } catch (Exception e) {
                log.debug("Logout token parse failed: {}", e.getMessage());
            }
        }
        log.info("Logout requested without a revocable session (client should discard tokens)");
    }

    /** OTP-gated password reset — no login required. */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        User user = userRepository.findByLoginIdentifier(req.identifier().trim())
                .orElseThrow(() -> new ResourceNotFound("Account not found: " + req.identifier()));
        otpService.verifyOtp(req.identifier().trim(), req.otp());
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())
                || passwordHistoryService.isReused(user.getUserId(), req.newPassword(), passwordEncoder)) {
            throw new BadRequestException("New password must differ from the last 3 passwords");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        passwordHistoryService.record(user.getUserId(), user.getPassword());
        sessions.revokeSession(user.getUserId());
        auditService.audit(user.getUserId(), LoginStatus.SUCCESS, "PASSWORD_RESET", null);
        log.info("Password reset for user {}", user.getUserLoginId());
    }

    // ---------------- internals ----------------

    private AuthResponse issueSession(User user, boolean forced) {
        CustomUserDetails principal = new CustomUserDetails(user);
        String access = jwtService.generateAccessToken(principal);
        String refresh = jwtService.generateRefreshToken(principal);
        sessions.saveSession(user.getUserId(), access, refresh);
        return forced
                ? AuthResponse.forced(access, refresh, jwtProps.getAccessExpirationMs(), mapper.toResponse(user))
                : AuthResponse.bearer(access, refresh, jwtProps.getAccessExpirationMs(), mapper.toResponse(user));
    }

    private void handleFailedPassword(User user, String clientIp) {
        int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
        user.setFailedLoginAttempts(attempts);
        userRepository.save(user);
        auditService.audit(user.getUserId(), LoginStatus.FAILED, "BAD_CREDENTIALS", clientIp);

        long consecutive = auditService.consecutiveFailures(user.getUserId());
        if (consecutive + 1 >= jwtProps.getMaxFailedAttempts()) {
            user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(jwtProps.getLockDurationMinutes()));
            user.setStatus(UserStatus.LOCKED);
            userRepository.save(user);
            auditService.audit(user.getUserId(), LoginStatus.LOCKED, "LOCKED", clientIp);
            log.warn("Locking user {} after {} failed attempts", user.getUserLoginId(), attempts);
        }
    }

    private void assertNotLocked(User user) {
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException(
                    "Account temporarily locked until " + user.getAccountLockedUntil() + ". Try later.");
        }
        if (user.getStatus() == UserStatus.LOCKED
                && (user.getAccountLockedUntil() == null || user.getAccountLockedUntil().isBefore(LocalDateTime.now()))) {
            user.setStatus(UserStatus.ACTIVE);
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            userRepository.save(user);
        }
    }

    public static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
