package com.citymall.mapper;

import com.citymall.dto.address.AddressRequest;
import com.citymall.dto.address.AddressResponse;
import com.citymall.dto.role.RoleResponse;
import com.citymall.dto.user.UserResponse;
import com.citymall.entity.Role;
import com.citymall.entity.User;
import com.citymall.entity.UserAddress;
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

    public AddressResponse toAddressResponse(UserAddress a) {
        if (a == null) return null;
        return new AddressResponse(
                a.getId(),
                a.getLabel(),
                a.getReceiverName(),
                a.getReceiverMobile(),
                a.getHouseDoorFlatNumber(),
                a.getLandmarkLocality(),
                a.getAreaNameNumber(),
                a.getCity(),
                a.getPostalPinCode(),
                a.getDistrictId(),
                a.getStateId(),
                a.getCountryId(),
                a.getIsDefault(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }

    /** Applies non-null/non-blank fields from UpdateUserRequest onto entity. */
    public void applyUpdate(User user, com.citymall.dto.user.UpdateUserRequest req) {
        if (isNotBlank(req.userName())) user.setUserName(req.userName().trim());
        if (isNotBlank(req.firstName())) user.setFirstName(req.firstName().trim());
        if (isNotBlank(req.lastName())) user.setLastName(req.lastName().trim());
        if (isNotBlank(req.emailId())) user.setEmailId(req.emailId().trim().toLowerCase());
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

    /** Applies non-null fields from AddressRequest onto entity. */
    public void applyAddressUpdate(UserAddress address, AddressRequest req) {
        if (isNotBlank(req.label())) address.setLabel(req.label().trim().toUpperCase());
        if (req.receiverName() != null) address.setReceiverName(req.receiverName());
        if (req.receiverMobile() != null) address.setReceiverMobile(req.receiverMobile());
        if (req.houseDoorFlatNumber() != null) address.setHouseDoorFlatNumber(req.houseDoorFlatNumber());
        if (req.landmarkLocality() != null) address.setLandmarkLocality(req.landmarkLocality());
        if (req.areaNameNumber() != null) address.setAreaNameNumber(req.areaNameNumber());
        if (req.city() != null) address.setCity(req.city());
        if (req.postalPinCode() != null) address.setPostalPinCode(req.postalPinCode());
        if (req.districtId() != null) address.setDistrictId(req.districtId());
        if (req.stateId() != null) address.setStateId(req.stateId());
        if (req.countryId() != null) address.setCountryId(req.countryId());
        if (req.isDefault() != null) address.setIsDefault(req.isDefault());
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }
}
