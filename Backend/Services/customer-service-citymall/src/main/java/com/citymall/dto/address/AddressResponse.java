package com.citymall.dto.address;

import java.time.LocalDateTime;

public record AddressResponse(
        Long id,
        String label,
        String receiverName,
        Long receiverMobile,
        String houseDoorFlatNumber,
        String landmarkLocality,
        String areaNameNumber,
        String city,
        Integer postalPinCode,
        Long districtId,
        Long stateId,
        Long countryId,
        Boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
