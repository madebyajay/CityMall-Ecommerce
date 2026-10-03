package com.citymall.dto.address;

public record AddressRequest(
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
        Boolean isDefault
) {
}
