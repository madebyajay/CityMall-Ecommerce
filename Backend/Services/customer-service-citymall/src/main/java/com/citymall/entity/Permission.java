package com.citymall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Minimal API-permission master (ported from user-management
 * {@code master_permissions}, trimmed: one row = one endpoint path).
 */
@Entity
@Table(
        name = "permissions",
        schema = "public",
        uniqueConstraints = @UniqueConstraint(name = "uk_permissions_endpoint", columnNames = "api_endpoint")
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Permission implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module", nullable = false, length = 100)
    private String module;

    /** e.g. {@code /api/v1/admin/customers}. */
    @Column(name = "api_endpoint", nullable = false, length = 250)
    private String apiEndpoint;

    @Column(name = "description", length = 500)
    private String description;

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
        if (activeStatus == null) activeStatus = true;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
