package com.zyc.copier_v0.modules.monitor.entity;

import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
        name = "mt5_account_runtime_states",
        uniqueConstraints = @UniqueConstraint(name = "uk_runtime_server_login", columnNames = {"server_name", "login_no"})
)
public class Mt5AccountRuntimeStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "login_no", nullable = false)
    private Long login;

    @Column(name = "server_name", nullable = false, length = 128)
    private String server;

    @Column(name = "account_key", nullable = false, length = 255)
    private String accountKey;

    @Column(name = "last_session_id", length = 128)
    private String lastSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_status", nullable = false, length = 32)
    private Mt5ConnectionStatus connectionStatus = Mt5ConnectionStatus.UNKNOWN;

    @Column(name = "last_hello_at")
    private Instant lastHelloAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "last_signal_at")
    private Instant lastSignalAt;

    @Column(name = "last_signal_type", length = 32)
    private String lastSignalType;

    @Column(name = "last_event_id", length = 128)
    private String lastEventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getLogin() {
        return login;
    }

    public void setLogin(Long login) {
        this.login = login;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public String getLastSessionId() {
        return lastSessionId;
    }

    public void setLastSessionId(String lastSessionId) {
        this.lastSessionId = lastSessionId;
    }

    public Mt5ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(Mt5ConnectionStatus connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public Instant getLastHelloAt() {
        return lastHelloAt;
    }

    public void setLastHelloAt(Instant lastHelloAt) {
        this.lastHelloAt = lastHelloAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Instant getLastSignalAt() {
        return lastSignalAt;
    }

    public void setLastSignalAt(Instant lastSignalAt) {
        this.lastSignalAt = lastSignalAt;
    }

    public String getLastSignalType() {
        return lastSignalType;
    }

    public void setLastSignalType(String lastSignalType) {
        this.lastSignalType = lastSignalType;
    }

    public String getLastEventId() {
        return lastEventId;
    }

    public void setLastEventId(String lastEventId) {
        this.lastEventId = lastEventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
