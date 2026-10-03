package com.citymall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 1:N delivery addresses for a user (ported from user-management
 * {@code user_address_details}, trimmed for e-commerce: no village/tehsil/court IDs).
 * The flat address columns on {@link User} stay as the legacy primary address.
 */
@Entity
@Table(
        name = "user_addresses",
        schema = "public",
        indexes = {@Index(name = "idx_user_addresses_user", columnList = "user_id")}
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_addresses_user"))
    private User user;

    /** e.g. HOME / WORK / OTHER. */
    @Column(name = "label", length = 20)
    @Builder.Default
    private String label = "HOME";

    @Column(name = "receiver_name", length = 150)
    private String receiverName;

    @Column(name = "receiver_mobile")
    private Long receiverMobile;

    @Column(name = "house_door_flat_number", length = 200)
    private String houseDoorFlatNumber;

    @Column(name = "landmark_locality", length = 200)
    private String landmarkLocality;

    @Column(name = "area_name_number", length = 200)
    private String areaNameNumber;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "postal_pin_code")
    private Integer postalPinCode;

    @Column(name = "district_id")
    private Long districtId;

    @Column(name = "state_id")
    private Long stateId;

    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "active_status", nullable = false)
    @Builder.Default
    private Boolean activeStatus = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (label == null) label = "HOME";
        if (isDefault == null) isDefault = false;
        if (activeStatus == null) activeStatus = true;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
