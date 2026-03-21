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

@Entity
@Table(
        name = "mt5_accounts",
        indexes = @Index(name = "idx_mt5_account_user", columnList = "user_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_mt5_server_login", columnNames = {"server_name", "mt5_login"})
)
public class Mt5AccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
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

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBrokerName() {
        return brokerName;
    }

    public void setBrokerName(String brokerName) {
        this.brokerName = brokerName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public Long getMt5Login() {
        return mt5Login;
    }

    public void setMt5Login(Long mt5Login) {
        this.mt5Login = mt5Login;
    }

    public String getCredentialCiphertext() {
        return credentialCiphertext;
    }

    public void setCredentialCiphertext(String credentialCiphertext) {
        this.credentialCiphertext = credentialCiphertext;
    }

    public Integer getCredentialVersion() {
        return credentialVersion;
    }

    public void setCredentialVersion(Integer credentialVersion) {
        this.credentialVersion = credentialVersion;
    }

    public Mt5AccountRole getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(Mt5AccountRole accountRole) {
        this.accountRole = accountRole;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
