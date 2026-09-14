package com.citymall.service;

import com.citymall.dto.user.ChangePasswordRequest;
import com.citymall.dto.user.UpdateUserRequest;
import com.citymall.dto.user.UserResponse;
import com.citymall.entity.User;
import com.citymall.entity.UserStatus;
import com.citymall.exception.BadRequestException;
import com.citymall.exception.BusinessException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.mapper.UserMapper;
import com.citymall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refactored customer profile service — operates on {@link User}, no passwords in output.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    public UserResponse getByLoginId(String loginId) {
        User user = userRepository.findByUserLoginId(loginId)
                .orElseThrow(() -> new ResourceNotFound("User not found: " + loginId));
        return mapper.toResponse(user);
    }

    public Page<UserResponse> list(Pageable pageable) {
        return userRepository.findAll(pageable).map(mapper::toResponse);
    }

    public boolean exists(Long id) {
        return userRepository.existsById(id);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        User user = findOrThrow(id);

        if (req.emailId() != null && !req.emailId().isBlank()
                && !req.emailId().equalsIgnoreCase(user.getEmailId())
                && userRepository.existsByEmailId(req.emailId().trim().toLowerCase())) {
            throw new BusinessException("Email already in use: " + req.emailId());
        }
        if (req.mobileNumber() != null && !req.mobileNumber().equals(user.getMobileNumber())
                && userRepository.existsByMobileNumber(req.mobileNumber())) {
            throw new BusinessException("Mobile number already in use: " + req.mobileNumber());
        }
        mapper.applyUpdate(user, req);
        log.info("Updated user {}", id);
        return mapper.toResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(Long id, ChangePasswordRequest req) {
        User user = findOrThrow(id);
        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new BadRequestException("Old password is incorrect");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must differ from old password");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        log.info("Password changed for user {}", id);
        // TODO (Redis phase): revoke all active tokens for this user here.
    }

    /** Soft delete — keeps row for audit, blocks login. */
    @Transactional
    public void deactivate(Long id) {
        User user = findOrThrow(id);
        user.setActiveStatus(false);
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
        log.info("Deactivated user {}", id);
        // TODO (Redis phase): blacklist tokens for this user.
    }

    /** Hard delete — admin only, prefer deactivate(). */
    @Transactional
    public void delete(Long id) {
        User user = findOrThrow(id);
        userRepository.delete(user);
        log.info("Hard-deleted user {}", id);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("User not found with id: " + id));
    }
}
