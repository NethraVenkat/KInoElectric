package com.backend.javaleap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Machine credential for the System actor (tile gateways, telemetry feeds).
 * Only the SHA-256 hash of the key is stored; requests with a valid
 * {@code X-API-KEY} header act as {@link #user}.
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@Entity
@Table(name = "api_client")
public class ApiClient extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "key_prefix", nullable = false, length = 12)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder.Default
    @Column(nullable = false)
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    /** Enabled and not expired. */
    public boolean isUsable() {
        return Boolean.TRUE.equals(enabled) && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }
}
