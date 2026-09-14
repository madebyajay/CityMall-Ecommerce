package com.citymall.config;

import com.citymall.entity.Role;
import com.citymall.entity.User;
import com.citymall.entity.UserStatus;
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
 * Seeds role master + a default admin (dev only). Idempotent.
 * Override admin bootstrap via env: ADMIN_LOGIN_ID / ADMIN_PASSWORD / ADMIN_EMAIL / ADMIN_MOBILE.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List.of(
                new String[]{"ADMIN", "Full system access"},
                new String[]{"SELLER", "Seller / merchant access"},
                new String[]{"CUSTOMER", "Default customer access"})
                .forEach(r -> roleRepository.findByName(r[0]).orElseGet(() -> {
                    log.info("Seeding role {}", r[0]);
                    return roleRepository.save(Role.builder()
                            .name(r[0]).description(r[1]).activeStatus(true).build());
                }));

        String adminLogin = env("ADMIN_LOGIN_ID", "admin");
        if (!userRepository.existsByUserLoginId(adminLogin)) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
            User admin = User.builder()
                    .userLoginId(adminLogin)
                    .password(passwordEncoder.encode(env("ADMIN_PASSWORD", "Admin@123")))
                    .userName("System Admin")
                    .firstName("System")
                    .lastName("Admin")
                    .emailId(env("ADMIN_EMAIL", "admin@citymall.com"))
                    .mobileNumber(Long.parseLong(env("ADMIN_MOBILE", "9999999999")))
                    .role(adminRole)
                    .status(UserStatus.ACTIVE)
                    .activeStatus(true)
                    .failedLoginAttempts(0)
                    .remarks("Seeded bootstrap admin — change password immediately")
                    .build();
            userRepository.save(admin);
            log.warn("Bootstrap admin '{}' created with default password. Change it immediately!", adminLogin);
        }
    }

    private static String env(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v;
    }
}
