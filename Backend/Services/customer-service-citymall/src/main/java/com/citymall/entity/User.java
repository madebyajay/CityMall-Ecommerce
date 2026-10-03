package com.citymall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Production-level user table for CityMall user-service (ex customer-service).
 * identity + credentials + contact + address + status/audit + role FK.
 */
@Entity
@Table(
        name = "users",
        schema = "public",
        indexes = {
                @Index(name = "idx_users_login_id", columnList = "user_login_id", unique = true),
                @Index(name = "idx_users_email", columnList = "email_id", unique = true),
                @Index(name = "idx_users_mobile", columnList = "mobile_number", unique = true),
                @Index(name = "idx_users_role", columnList = "role_id"),
                @Index(name = "idx_users_status", columnList = "user_status")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "users_seq")
    @SequenceGenerator(name = "users_seq", sequenceName = "users_seq", allocationSize = 1)
    private Long userId;

    /** Unique login id chosen by user (can also login via email/mobile). */
    @Column(name = "user_login_id", nullable = false, unique = true, length = 100)
    private String userLoginId;

    /** BCrypt-hashed password. Never expose in responses. */
    @Column(name = "user_login_password", nullable = false)
    private String password;

    /** Display name. */
    @Column(name = "user_name", nullable = false, length = 150)
    private String userName;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email_id", unique = true, length = 150)
    private String emailId;

    /** Nullable to allow PII scrub on soft-delete (unique constraint ignores nulls). */
    @Column(name = "mobile_number", unique = true)
    private Long mobileNumber;

    // ---------------- Address (flat, FK-ready for master tables later) ----------------
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

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "profile_picture_path")
    private String profilePicturePath;

    @Column(name = "remarks", length = 500)
    private String remarks;

    // ---------------- Role ----------------
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_users_role"))
    private Role role;

    // ---------------- Status / security ----------------
    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "active_status", nullable = false)
    @Builder.Default
    private Boolean activeStatus = true;

    @Column(name = "number_of_tries_for_logging_in", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "valid_from_date")
    private LocalDateTime validFromDate;

    @Column(name = "valid_to_date")
    private LocalDateTime validToDate;

    // ---------------- Audit ----------------
    @Column(name = "client_ip_address", length = 45)
    private String clientIpAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (validFromDate == null) validFromDate = now;
        if (status == null) status = UserStatus.ACTIVE;
        if (activeStatus == null) activeStatus = true;
        if (failedLoginAttempts == null) failedLoginAttempts = 0;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /** Account is usable only when active + not locked + within validity window. */
    public boolean isAccountUsable() {
        if (Boolean.FALSE.equals(activeStatus)) return false;
        if (status == UserStatus.DELETED || status == UserStatus.INACTIVE) return false;
        if (status == UserStatus.LOCKED) return false;
        if (accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now())) return false;
        LocalDateTime now = LocalDateTime.now();
        if (validFromDate != null && now.isBefore(validFromDate)) return false;
        if (validToDate != null && now.isAfter(validToDate)) return false;
        return true;
    }
}
