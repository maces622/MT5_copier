package com.zyc.copier_v0.modules.monitor.entity;

import java.math.BigDecimal;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "mt5_open_positions",
        indexes = {
                @Index(name = "idx_mt5_open_position_account", columnList = "account_key"),
                @Index(name = "idx_mt5_open_position_master_position", columnList = "master_position_id"),
                @Index(name = "idx_mt5_open_position_observed", columnList = "observed_at")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mt5_open_position_account_key_position_key",
                columnNames = {"account_key", "position_key"}
        )
)
@Getter
@Setter
public class Mt5OpenPositionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "login_no")
    private Long login;

    @Column(name = "server_name", length = 128)
    private String server;

    @Column(name = "account_key", nullable = false, length = 255)
    private String accountKey;

    @Column(name = "position_key", nullable = false, length = 128)
    private String positionKey;

    @Column(name = "source_position_id")
    private Long sourcePositionId;

    @Column(name = "source_order_id")
    private Long sourceOrderId;

    @Column(name = "master_position_id")
    private Long masterPositionId;

    @Column(name = "master_order_id")
    private Long masterOrderId;

    @Column(name = "symbol", length = 64)
    private String symbol;

    @Column(name = "volume", precision = 18, scale = 6)
    private BigDecimal volume;

    @Column(name = "price_open", precision = 18, scale = 10)
    private BigDecimal priceOpen;

    @Column(name = "sl", precision = 18, scale = 10)
    private BigDecimal sl;

    @Column(name = "tp", precision = 18, scale = 10)
    private BigDecimal tp;

    @Column(name = "comment_text", length = 255)
    private String commentText;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (observedAt == null) {
            observedAt = now;
        }
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        if (observedAt == null) {
            observedAt = updatedAt;
        }
    }
}
