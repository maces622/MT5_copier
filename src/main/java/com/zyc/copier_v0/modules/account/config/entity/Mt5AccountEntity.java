package com.zyc.copier_v0.modules.account.config.entity;

import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "mt5_accounts",
        indexes = @Index(name = "idx_mt5_account_user", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_mt5_server_login", columnNames = {"server_name", "mt5_login"})
)
@Getter
@Setter
public class Mt5AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "broker_name", nullable = false, length = 128)
    private String brokerName;

    @Column(name = "server_name", nullable = false, length = 128)
    private String serverName;

    @Column(name = "mt5_login", nullable = false)
    private Long mt5Login;

    @Column(name = "credential_ciphertext", nullable = false, length = 2048)
    private String credentialCiphertext;

    @Column(name = "credential_version", nullable = false)
    private Integer credentialVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_role", nullable = false, length = 32)
    private Mt5AccountRole accountRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long rowVersion;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
