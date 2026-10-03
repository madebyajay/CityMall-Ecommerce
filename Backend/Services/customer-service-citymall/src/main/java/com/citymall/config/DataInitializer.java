package com.citymall.config;

import com.citymall.entity.Permission;
import com.citymall.entity.Role;
import com.citymall.entity.RolePermission;
import com.citymall.entity.User;
import com.citymall.entity.UserStatus;
import com.citymall.repository.PermissionRepository;
import com.citymall.repository.RolePermissionRepository;
import com.citymall.repository.RoleRepository;
import com.citymall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds role master (SUPERADMIN/ADMIN/SELLER/CUSTOMER), bootstrap admin +
 * superadmin, and the base permission master with role mappings. Idempotent.
 *
 * <p>Override bootstrap credentials via env:
 * ADMIN_LOGIN_ID / ADMIN_PASSWORD / ADMIN_EMAIL / ADMIN_MOBILE and
 * SUPERADMIN_LOGIN_ID / SUPERADMIN_PASSWORD / SUPERADMIN_EMAIL / SUPERADMIN_MOBILE.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List.of(
                        new String[]{"SUPERADMIN", "Full system access, bypasses all checks"},
                        new String[]{"ADMIN", "Staff access: customers, sellers, catalog oversight"},
                        new String[]{"SELLER", "Seller / merchant access"},
                        new String[]{"CUSTOMER", "Default customer access"})
                .forEach(r -> roleRepository.findByName(r[0]).orElseGet(() -> {
                    log.info("Seeding role {}", r[0]);
                    return roleRepository.save(Role.builder()
                            .name(r[0]).description(r[1]).activeStatus(true).build());
                }));

        seedBootstrapUser("ADMIN", env("ADMIN_LOGIN_ID", "admin"),
                env("ADMIN_PASSWORD", "Admin@123"), "System Admin", "System", "Admin",
                env("ADMIN_EMAIL", "admin@citymall.com"), env("ADMIN_MOBILE", "9999999999"));
        seedBootstrapUser("SUPERADMIN", env("SUPERADMIN_LOGIN_ID", "superadmin"),
                env("SUPERADMIN_PASSWORD", "Super@123"), "Super Admin", "Super", "Admin",
                env("SUPERADMIN_EMAIL", "superadmin@citymall.com"), env("SUPERADMIN_MOBILE", "8888888888"));

        seedPermissions();
    }

    private void seedBootstrapUser(String roleName, String loginId, String rawPassword,
                                   String displayName, String firstName, String lastName,
                                   String email, String mobileRaw) {
        if (userRepository.existsByUserLoginId(loginId)) return;
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = User.builder()
                .userLoginId(loginId)
                .password(passwordEncoder.encode(rawPassword))
                .userName(displayName)
                .firstName(firstName)
                .lastName(lastName)
                .emailId(email)
                .mobileNumber(Long.parseLong(mobileRaw))
                .role(role)
                .status(UserStatus.ACTIVE)
                .activeStatus(true)
                .failedLoginAttempts(0)
                .remarks("Seeded bootstrap " + roleName.toLowerCase() + " — change password immediately")
                .build();
        userRepository.save(user);
        log.warn("Bootstrap {} '{}' created with default password. Change it immediately!", roleName, loginId);
    }

    /** Base endpoint master + ADMIN/SUPERADMIN grants (CUSTOMER needs no explicit rows). */
    private void seedPermissions() {
        List<String[]> base = List.of(
                new String[]{"IDENTITY", "/api/v1/admin/customers", "Manage customers"},
                new String[]{"IDENTITY", "/api/v1/admin/sellers", "Manage sellers"},
                new String[]{"IDENTITY", "/api/v1/admin/dashboard/summary", "View dashboard summary"},
                new String[]{"IDENTITY", "/api/v1/users", "User directory (admin view)"},
                new String[]{"IDENTITY", "/api/v1/roles", "Role master"},
                new String[]{"IDENTITY", "/api/v1/superadmin/users", "Full user directory"},
                new String[]{"IDENTITY", "/api/v1/superadmin/permissions", "Permission master"});

        for (String[] row : base) {
            Permission permission = permissionRepository.findByApiEndpoint(row[1])
                    .orElseGet(() -> permissionRepository.save(Permission.builder()
                            .module(row[0]).apiEndpoint(row[1]).description(row[2])
                            .activeStatus(true).build()));
            for (String roleName : List.of("ADMIN")) {
                Role role = roleRepository.findByName(roleName).orElseThrow();
                rolePermissionRepository.findByRole_NameAndPermission_ApiEndpoint(roleName, row[1])
                        .orElseGet(() -> rolePermissionRepository.save(RolePermission.builder()
                                .role(role).permission(permission).activeStatus(true).build()));
            }
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
