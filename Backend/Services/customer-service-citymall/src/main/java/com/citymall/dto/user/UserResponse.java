package com.citymall.dto.user;

import com.citymall.entity.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String userLoginId,
        String userName,
        String firstName,
        String lastName,
        String emailId,
        Long mobileNumber,
        LocalDate dateOfBirth,
        String houseDoorFlatNumber,
        String landmarkLocality,
        String areaNameNumber,
        String city,
        Integer postalPinCode,
        Long districtId,
        Long stateId,
        Long countryId,
        String profilePicturePath,
        String roleName,
        Long roleId,
        UserStatus status,
        Boolean activeStatus,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
