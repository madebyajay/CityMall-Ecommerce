package com.citymall.mapper;

import com.citymall.dto.role.RoleResponse;
import com.citymall.dto.user.UserResponse;
import com.citymall.entity.Role;
import com.citymall.entity.User;
import io.micrometer.common.util.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(User u) {
        if (u == null) return null;
        return new UserResponse(
                u.getUserId(),
                u.getUserLoginId(),
                u.getUserName(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmailId(),
                u.getMobileNumber(),
                u.getDateOfBirth(),
                u.getHouseDoorFlatNumber(),
                u.getLandmarkLocality(),
                u.getAreaNameNumber(),
                u.getCity(),
                u.getPostalPinCode(),
                u.getDistrictId(),
                u.getStateId(),
                u.getCountryId(),
                u.getProfilePicturePath(),
                u.getRole() != null ? u.getRole().getName() : null,
                u.getRole() != null ? u.getRole().getId() : null,
                u.getStatus(),
                u.getActiveStatus(),
                u.getLastLoginAt(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }

    public RoleResponse toRoleResponse(Role r) {
        if (r == null) return null;
        return new RoleResponse(
                r.getId(), r.getName(), r.getDescription(),
                r.getActiveStatus(), r.getCreatedAt(), r.getUpdatedAt());
    }

    /** Applies non-null/non-blank fields from UpdateUserRequest onto entity. */
    public void applyUpdate(User user, com.citymall.dto.user.UpdateUserRequest req) {
        if (StringUtils.isNotBlank(req.userName())) user.setUserName(req.userName().trim());
        if (StringUtils.isNotBlank(req.firstName())) user.setFirstName(req.firstName().trim());
        if (StringUtils.isNotBlank(req.lastName())) user.setLastName(req.lastName().trim());
        if (StringUtils.isNotBlank(req.emailId())) user.setEmailId(req.emailId().trim().toLowerCase());
        if (req.mobileNumber() != null) user.setMobileNumber(req.mobileNumber());
        if (req.dateOfBirth() != null) user.setDateOfBirth(req.dateOfBirth());
        if (req.houseDoorFlatNumber() != null) user.setHouseDoorFlatNumber(req.houseDoorFlatNumber());
        if (req.landmarkLocality() != null) user.setLandmarkLocality(req.landmarkLocality());
        if (req.areaNameNumber() != null) user.setAreaNameNumber(req.areaNameNumber());
        if (req.city() != null) user.setCity(req.city());
        if (req.postalPinCode() != null) user.setPostalPinCode(req.postalPinCode());
        if (req.districtId() != null) user.setDistrictId(req.districtId());
        if (req.stateId() != null) user.setStateId(req.stateId());
        if (req.countryId() != null) user.setCountryId(req.countryId());
        if (req.profilePicturePath() != null) user.setProfilePicturePath(req.profilePicturePath());
        if (req.remarks() != null) user.setRemarks(req.remarks());
    }
}
