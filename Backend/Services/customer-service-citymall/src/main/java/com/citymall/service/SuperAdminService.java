package com.citymall.service;

import com.citymall.dto.admin.CreateAdminRequest;
import com.citymall.dto.user.UserResponse;
import com.citymall.entity.LoginStatus;
import com.citymall.entity.Role;
import com.citymall.entity.User;
import com.citymall.entity.UserStatus;
import com.citymall.exception.BadRequestException;
import com.citymall.exception.BusinessException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.mapper.UserMapper;
import com.citymall.repository.RoleRepository;
import com.citymall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SUPERADMIN-only operations — full user directory, role assignment and
 * destructive deletes. Every route sits under {@code /api/v1/superadmin/**}
 * and requires {@code ROLE_SUPERADMIN}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;
    private final PasswordHistoryService passwordHistoryService;
    private final RedisSessionService sessions;
    private final LoginAuditService auditService;

    @Transactional
    public UserResponse createSuperAdmin(CreateAdminRequest req) {
        String loginId = req.userLoginId().trim();
        String email = req.emailId() != null ? req.emailId().trim().toLowerCase() : null;

        if (userRepository.existsByUserLoginId(loginId)) {
            throw new BusinessException("Login id already taken: " + loginId);
        }
        if (email != null && !email.isBlank() && userRepository.existsByEmailId(email)) {
            throw new BusinessException("Email already registered: " + email);
        }
        if (req.mobileNumber() != null && userRepository.existsByMobileNumber(req.mobileNumber())) {
            throw new BusinessException("Mobile number already registered: " + req.mobileNumber());
        }

        Role role = roleRepository.findByName("SUPERADMIN")
                .orElseThrow(() -> new ResourceNotFound("Role not found: SUPERADMIN"));
        User superAdmin = User.builder()
                .userLoginId(loginId)
                .password(passwordEncoder.encode(req.password()))
                .userName(req.userName().trim())
                .firstName(req.firstName())
                .lastName(req.lastName())
                .emailId(email)
                .mobileNumber(req.mobileNumber())
                .role(role)
                .status(UserStatus.ACTIVE)
                .activeStatus(true)
                .failedLoginAttempts(0)
                .remarks("Created via POST /api/v1/superadmin/create-superadmin")
                .build();
        User saved = userRepository.save(superAdmin);
        passwordHistoryService.record(saved.getUserId(), saved.getPassword());
        log.info("SuperAdmin created: {}", loginId);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String query, String roleName, Pageable pageable) {
        String role = (roleName == null || roleName.isBlank()) ? null : roleName.trim().toUpperCase();
        String q = (query == null || query.isBlank()) ? null : query.trim();
        return userRepository.search(q, role, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    /** Change any user's role (except the last remaining SUPERADMIN). */
    @Transactional
    public UserResponse updateUserRole(Long id, String roleName) {
        User user = findOrThrow(id);
        String target = roleName.trim().toUpperCase();
        Role role = roleRepository.findByName(target)
                .orElseThrow(() -> new ResourceNotFound("Role not found: " + target));
        if (Boolean.FALSE.equals(role.getActiveStatus())) {
            throw new BadRequestException("Role is inactive: " + target);
        }
        if ("SUPERADMIN".equalsIgnoreCase(user.getRole().getName()) && !"SUPERADMIN".equals(target)) {
            long superAdmins = userRepository.countByRole_Name("SUPERADMIN");
            if (superAdmins <= 1) {
                throw new BadRequestException("Cannot demote the last SUPERADMIN");
            }
        }
        user.setRole(role);
        User saved = userRepository.save(user);
        sessions.revokeSession(id);
        auditService.audit(id, LoginStatus.SUCCESS, "ROLE_CHANGED_TO_" + target, null);
        log.info("User {} role changed to {}", id, target);
        return mapper.toResponse(saved);
    }

    /** Block or unblock any account (customers, sellers and admins alike). */
    @Transactional
    public UserResponse setBlocked(Long id, boolean blocked, String reason) {
        User user = findOrThrow(id);
        if ("SUPERADMIN".equalsIgnoreCase(user.getRole().getName()) && blocked) {
            throw new BadRequestException("SUPERADMIN accounts cannot be blocked");
        }
        if (blocked) {
            user.setStatus(UserStatus.INACTIVE);
            user.setActiveStatus(false);
            if (reason != null && !reason.isBlank()) user.setRemarks(reason.trim());
            sessions.revokeSession(id);
            auditService.audit(id, LoginStatus.LOCKED, "BLOCKED_BY_SUPERADMIN", null);
        } else {
            user.setStatus(UserStatus.ACTIVE);
            user.setActiveStatus(true);
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            auditService.audit(id, LoginStatus.SUCCESS, "UNBLOCKED_BY_SUPERADMIN", null);
        }
        return mapper.toResponse(userRepository.save(user));
    }

    /** Soft delete + PII scrub. Refuses the last remaining SUPERADMIN. */
    @Transactional
    public void deleteUser(Long id) {
        User user = findOrThrow(id);
        if ("SUPERADMIN".equalsIgnoreCase(user.getRole().getName())
                && userRepository.countByRole_Name("SUPERADMIN") <= 1) {
            throw new BadRequestException("Cannot delete the last SUPERADMIN");
        }
        user.setActiveStatus(false);
        user.setStatus(UserStatus.DELETED);
        user.setEmailId(null);
        user.setMobileNumber(null);
        userRepository.save(user);
        sessions.revokeSession(id);
        auditService.audit(id, LoginStatus.LOGOUT, "DELETED_BY_SUPERADMIN", null);
        log.info("SuperAdmin soft-deleted user {}", id);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("User not found with id: " + id));
    }
}
