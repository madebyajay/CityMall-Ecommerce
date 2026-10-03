package com.citymall.service;

import com.citymall.dto.admin.BlockCustomerRequest;
import com.citymall.dto.admin.CreateAdminRequest;
import com.citymall.dto.admin.DashboardSummaryResponse;
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
 * Admin operations — staff account creation (ADMIN/SELLER) plus CUSTOMER
 * lifecycle. SUPERADMIN-only work (role changes, deletes, permissions) lives
 * in {@link SuperAdminService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;
    private final PasswordHistoryService passwordHistoryService;
    private final RedisSessionService sessions;
    private final LoginAuditService auditService;

    /** Create an additional ADMIN. Never reachable without an ADMIN/SUPERADMIN JWT. */
    @Transactional
    public UserResponse createAdmin(CreateAdminRequest req) {
        return createStaff(req.userLoginId(), req.password(), req.userName(), req.firstName(),
                req.lastName(), req.emailId(), req.mobileNumber(), "ADMIN",
                "Created via POST /api/v1/admin/create-admin");
    }

    /** Create a SELLER (merchant) account. */
    @Transactional
    public UserResponse createSeller(CreateAdminRequest req) {
        return createStaff(req.userLoginId(), req.password(), req.userName(), req.firstName(),
                req.lastName(), req.emailId(), req.mobileNumber(), "SELLER",
                "Created via POST /api/v1/admin/create-seller");
    }

    private UserResponse createStaff(String loginIdRaw, String password, String userName,
                                     String firstName, String lastName, String emailRaw,
                                     Long mobile, String roleName, String remarks) {
        String loginId = loginIdRaw.trim();
        String email = emailRaw != null ? emailRaw.trim().toLowerCase() : null;

        if (userRepository.existsByUserLoginId(loginId)) {
            throw new BusinessException("Login id already taken: " + loginId);
        }
        if (email != null && !email.isBlank() && userRepository.existsByEmailId(email)) {
            throw new BusinessException("Email already registered: " + email);
        }
        if (mobile != null && userRepository.existsByMobileNumber(mobile)) {
            throw new BusinessException("Mobile number already registered: " + mobile);
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFound("Role not found: " + roleName));
        if (Boolean.FALSE.equals(role.getActiveStatus())) {
            throw new BadRequestException("Role is inactive: " + roleName);
        }

        User staff = User.builder()
                .userLoginId(loginId)
                .password(passwordEncoder.encode(password))
                .userName(userName.trim())
                .firstName(firstName)
                .lastName(lastName)
                .emailId(email)
                .mobileNumber(mobile)
                .role(role)
                .status(UserStatus.ACTIVE)
                .activeStatus(true)
                .failedLoginAttempts(0)
                .remarks(remarks)
                .build();

        User saved = userRepository.save(staff);
        passwordHistoryService.record(saved.getUserId(), saved.getPassword());
        log.info("{} created: {}", roleName, loginId);
        return mapper.toResponse(saved);
    }

    /** Paginated CUSTOMER-only listing (admins/sellers excluded). */
    public Page<UserResponse> listCustomers(Pageable pageable) {
        return userRepository.findByRole_Name("CUSTOMER", pageable).map(mapper::toResponse);
    }

    public Page<UserResponse> listSellers(Pageable pageable) {
        return userRepository.findByRole_Name("SELLER", pageable).map(mapper::toResponse);
    }

    public UserResponse getCustomer(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("User not found with id: " + id));
        if (user.getRole() == null || !"CUSTOMER".equalsIgnoreCase(user.getRole().getName())) {
            throw new ResourceNotFound("Customer not found with id: " + id);
        }
        return mapper.toResponse(user);
    }

    /** Toggle blocked state. Blocked = INACTIVE + activeStatus=false (login rejected). */
    @Transactional
    public UserResponse blockCustomer(Long id, BlockCustomerRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("User not found with id: " + id));
        if (user.getRole() == null || !"CUSTOMER".equalsIgnoreCase(user.getRole().getName())) {
            throw new BadRequestException("Only CUSTOMER accounts can be blocked here: " + id);
        }
        if (Boolean.TRUE.equals(req.blocked())) {
            user.setStatus(UserStatus.INACTIVE);
            user.setActiveStatus(false);
            if (req.reason() != null && !req.reason().isBlank()) {
                user.setRemarks(req.reason().trim());
            }
            sessions.revokeSession(id);
            auditService.audit(id, LoginStatus.LOCKED, "BLOCKED_BY_ADMIN", null);
            log.info("Blocked customer {}", id);
        } else {
            user.setStatus(UserStatus.ACTIVE);
            user.setActiveStatus(true);
            user.setFailedLoginAttempts(0);
            user.setAccountLockedUntil(null);
            auditService.audit(id, LoginStatus.SUCCESS, "UNBLOCKED_BY_ADMIN", null);
            log.info("Unblocked customer {}", id);
        }
        return mapper.toResponse(userRepository.save(user));
    }

    public DashboardSummaryResponse dashboardSummary() {
        long total = userRepository.countByRole_Name("CUSTOMER");
        long blocked = userRepository.countByRole_NameAndActiveStatus("CUSTOMER", false);
        // Order/product/revenue are stubbed until Feign clients land (see record javadoc).
        return DashboardSummaryResponse.stubbed(total, blocked);
    }
}
