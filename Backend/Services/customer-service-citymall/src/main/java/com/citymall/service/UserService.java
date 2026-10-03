package com.citymall.service;

import com.citymall.dto.address.AddressRequest;
import com.citymall.dto.address.AddressResponse;
import com.citymall.dto.user.ChangePasswordRequest;
import com.citymall.dto.user.UpdateUserRequest;
import com.citymall.dto.user.UserResponse;
import com.citymall.entity.LoginStatus;
import com.citymall.entity.User;
import com.citymall.entity.UserAddress;
import com.citymall.entity.UserStatus;
import com.citymall.exception.BadRequestException;
import com.citymall.exception.BusinessException;
import com.citymall.exception.ResourceNotFound;
import com.citymall.mapper.UserMapper;
import com.citymall.repository.UserAddressRepository;
import com.citymall.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Customer profile service — operates on {@link User}, no passwords in output.
 * Password changes consult {@link PasswordHistoryService} and revoke sessions;
 * deletes are soft + PII-scrubbed (history kept), matching the reference
 * user-management delete flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository addressRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService passwordHistoryService;
    private final RedisSessionService sessions;
    private final LoginAuditService auditService;

    public UserResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    public UserResponse getByLoginId(String loginId) {
        User user = userRepository.findByUserLoginId(loginId)
                .orElseThrow(() -> new ResourceNotFound("User not found: " + loginId));
        return mapper.toResponse(user);
    }

    /** Lookup by loginId OR email OR mobile (string) — pre-login gates, no auth needed. */
    public UserResponse getByLoginIdentifier(String identifier) {
        User user = userRepository.findByLoginIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFound("Account not found: " + identifier));
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
        if (passwordEncoder.matches(req.newPassword(), user.getPassword())
                || passwordHistoryService.isReused(id, req.newPassword(), passwordEncoder)) {
            throw new BadRequestException("New password must differ from the last 3 passwords");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.save(user);
        passwordHistoryService.record(id, user.getPassword());
        sessions.revokeSession(id);
        auditService.audit(id, LoginStatus.SUCCESS, "PASSWORD_CHANGE", null);
        log.info("Password changed for user {}", id);
    }

    /** Soft delete + PII scrub — keeps the row for audit, blocks login, revokes sessions. */
    @Transactional
    public void deactivate(Long id) {
        User user = findOrThrow(id);
        user.setActiveStatus(false);
        user.setStatus(UserStatus.DELETED);
        user.setEmailId(null);
        user.setMobileNumber(null);
        userRepository.save(user);
        sessions.revokeSession(id);
        auditService.audit(id, LoginStatus.LOGOUT, "DEACTIVATED", null);
        log.info("Deactivated user {}", id);
    }

    // ---------------- addresses ----------------

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(Long userId) {
        findOrThrow(userId);
        return addressRepository.findByUser_UserIdAndActiveStatusTrueOrderByIsDefaultDescCreatedAtAsc(userId)
                .stream().map(mapper::toAddressResponse).toList();
    }

    @Transactional
    public AddressResponse addAddress(Long userId, AddressRequest req) {
        User user = findOrThrow(userId);
        boolean first = addressRepository.countByUser_UserIdAndActiveStatusTrue(userId) == 0;
        UserAddress address = UserAddress.builder()
                .user(user)
                .label(req.label() != null && !req.label().isBlank() ? req.label().trim().toUpperCase() : "HOME")
                .receiverName(req.receiverName())
                .receiverMobile(req.receiverMobile())
                .houseDoorFlatNumber(req.houseDoorFlatNumber())
                .landmarkLocality(req.landmarkLocality())
                .areaNameNumber(req.areaNameNumber())
                .city(req.city())
                .postalPinCode(req.postalPinCode())
                .districtId(req.districtId())
                .stateId(req.stateId())
                .countryId(req.countryId())
                .isDefault(first || Boolean.TRUE.equals(req.isDefault()))
                .activeStatus(true)
                .build();
        UserAddress saved = addressRepository.save(address);
        if (Boolean.TRUE.equals(saved.getIsDefault())) {
            clearOtherDefaults(userId, saved.getId());
        }
        return mapper.toAddressResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest req) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFound("Address not found: " + addressId));
        assertOwnsAddress(userId, address);
        mapper.applyAddressUpdate(address, req);
        UserAddress saved = addressRepository.save(address);
        if (Boolean.TRUE.equals(req.isDefault()) && Boolean.TRUE.equals(saved.getIsDefault())) {
            clearOtherDefaults(userId, saved.getId());
        }
        return mapper.toAddressResponse(saved);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFound("Address not found: " + addressId));
        assertOwnsAddress(userId, address);
        address.setActiveStatus(false);
        address.setIsDefault(false);
        addressRepository.save(address);
    }

    private static void assertOwnsAddress(Long userId, UserAddress address) {
        if (address.getUser() == null || !userId.equals(address.getUser().getUserId())) {
            throw new BadRequestException("Address does not belong to user: " + userId);
        }
    }

    private void clearOtherDefaults(Long userId, Long keepId) {
        addressRepository.findByUser_UserIdAndActiveStatusTrueOrderByIsDefaultDescCreatedAtAsc(userId)
                .stream()
                .filter(a -> !a.getId().equals(keepId) && Boolean.TRUE.equals(a.getIsDefault()))
                .forEach(a -> {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                });
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFound("User not found with id: " + id));
    }
}
