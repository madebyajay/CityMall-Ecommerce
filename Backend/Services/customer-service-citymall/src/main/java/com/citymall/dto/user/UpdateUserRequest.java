package com.citymall.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateUserRequest(

        @Size(min = 2, max = 150, message = "User name must be 2-150 characters")
        String userName,

        String firstName,

        String lastName,

        @Email(message = "Email should be valid")
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

        String remarks
) {
}
