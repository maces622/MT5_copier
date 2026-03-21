package com.zyc.copier_v0.modules.monitor.entity;

import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import java.math.BigDecimal;
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
        name = "mt5_account_runtime_states",
        indexes = {
                @Index(name = "idx_runtime_account_id", columnList = "account_id"),
                @Index(name = "idx_runtime_account_key", columnList = "account_key"),
                @Index(name = "idx_runtime_connection_status", columnList = "connection_status")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_runtime_server_login", columnNames = {"server_name", "login_no"})
)
@Getter
@Setter
public class Mt5AccountRuntimeStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
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

    @Column(name = "balance", precision = 18, scale = 6)
    private BigDecimal balance;

    @Column(name = "equity", precision = 18, scale = 6)
    private BigDecimal equity;

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
