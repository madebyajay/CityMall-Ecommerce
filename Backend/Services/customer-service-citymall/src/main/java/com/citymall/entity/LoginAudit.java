package com.citymall.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Append-only login audit trail (ported from user-management
 * {@code user_logged_audit_trail}). Written in a separate transaction so audit
 * rows survive even when the outer login transaction rolls back.
 */
@Entity
@Table(
        name = "user_login_audit",
        schema = "public",
        indexes = {@Index(name = "idx_login_audit_user", columnList = "user_id")}
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private LoginStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "client_ip_address", length = 45)
    private String clientIpAddress;

    @Column(name = "action_date", nullable = false)
    private LocalDateTime actionDate;

    @PrePersist
    void onCreate() {
        if (actionDate == null) actionDate = LocalDateTime.now();
    }
}
