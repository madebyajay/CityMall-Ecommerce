package com.citymall.service;

import com.citymall.config.JwtProperties;
import com.citymall.dto.auth.AuthResponse;
import com.citymall.dto.auth.LoginRequest;
import com.citymall.dto.auth.RefreshTokenRequest;
import com.citymall.dto.auth.RegisterRequest;
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

/**
 * Auth = register + refresh. Login/logout live in {@link LoginService}
 * but share JWT issuance + lockout policy here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final UserMapper mapper;
    private final JwtProperties jwtProps;

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
        // Self-registration may never create ADMIN
        if ("ADMIN".equals(roleName)) {
            throw new BadRequestException("Cannot self-register as ADMIN");
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
        log.info("Registered user {} with role {}", loginId, roleName);

        CustomUserDetails principal = new CustomUserDetails(saved);
        return AuthResponse.bearer(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                jwtProps.getAccessExpirationMs(),
                mapper.toResponse(saved));
    }

    /** Shared login core used by LoginService (keeps lockout logic in one place). */
    @Transactional
    public AuthResponse authenticate(LoginRequest req, String clientIp) {
        User user = userRepository.findByLoginIdentifier(req.identifier().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        assertNotLocked(user);

        if (!user.isAccountUsable()) {
            throw new UnauthorizedException("Account is disabled. Contact support.");
        }

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= jwtProps.getMaxFailedAttempts()) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(jwtProps.getLockDurationMinutes()));
                user.setStatus(UserStatus.LOCKED);
                log.warn("Locking user {} after {} failed attempts", user.getUserLoginId(), attempts);
            }
            userRepository.save(user);
            throw new UnauthorizedException("Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        if (user.getStatus() == UserStatus.LOCKED) user.setStatus(UserStatus.ACTIVE);
        user.setLastLoginAt(LocalDateTime.now());
        user.setClientIpAddress(clientIp);
        User saved = userRepository.save(user);

        CustomUserDetails principal = new CustomUserDetails(saved);
        log.info("User logged in: {}", saved.getUserLoginId());
        return AuthResponse.bearer(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                jwtProps.getAccessExpirationMs(),
                mapper.toResponse(saved));
    }

    public AuthResponse refresh(RefreshTokenRequest req) {
        String token = req.refreshToken();
        if (!jwtService.isValid(token) || !jwtService.isRefreshToken(token)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        // TODO (Redis phase): check refresh-token allowlist here.
        Long userId = jwtService.extractUserId(token);
        if (userId == null) {
            // Fallback: resolve via subject (loginId)
            String loginId = jwtService.extractUsername(token);
            CustomUserDetails principal = (CustomUserDetails) userDetailsService.loadUserByUsername(loginId);
            return AuthResponse.bearer(
                    jwtService.generateAccessToken(principal),
                    jwtService.generateRefreshToken(principal),
                    jwtProps.getAccessExpirationMs(),
                    mapper.toResponse(userRepository.findByUserLoginId(loginId)
                            .orElseThrow(() -> new ResourceNotFound("User not found: " + loginId))));
        }
        CustomUserDetails principal = userDetailsService.loadByUserId(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFound("User not found with id: " + userId));
        if (!user.isAccountUsable()) {
            throw new UnauthorizedException("Account is disabled");
        }
        return AuthResponse.bearer(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                jwtProps.getAccessExpirationMs(),
                mapper.toResponse(user));
    }

    private void assertNotLocked(User user) {
        if (user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException(
                    "Account temporarily locked until " + user.getAccountLockedUntil() + ". Try later.");
        }
        // Auto-unlock expired locks
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
