package com.zyc.copier_v0.modules.copy.engine.entity;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionCommandType;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionCommandStatus;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionRejectReason;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SignalType;
import com.zyc.copier_v0.support.ManualIdGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
        name = "execution_commands",
        indexes = {
                @Index(name = "idx_execution_command_follower", columnList = "follower_account_id"),
                @Index(name = "idx_execution_command_master_order", columnList = "master_account_id,master_order_id"),
                @Index(name = "idx_execution_command_master_position", columnList = "master_account_id,master_position_id"),
                @Index(name = "idx_execution_command_status", columnList = "status")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_execution_command_master_event_follower",
                columnNames = {"master_event_id", "follower_account_id"}
        )
)
@Getter
@Setter
public class ExecutionCommandEntity {

    @Id
    private Long id;

    @Column(name = "master_event_id", nullable = false, length = 128)
    private String masterEventId;

    @Column(name = "master_account_id")
    private Long masterAccountId;

    @Column(name = "master_account_key", nullable = false, length = 255)
    private String masterAccountKey;

    @Column(name = "follower_account_id", nullable = false)
    private Long followerAccountId;

    @Column(name = "master_symbol", length = 64)
    private String masterSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 16)
    private Mt5SignalType signalType;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 32)
    private ExecutionCommandType commandType;

    @Column(name = "symbol", nullable = false, length = 64)
    private String symbol;

    @Column(name = "master_action", nullable = false, length = 64)
    private String masterAction;

    @Column(name = "follower_action", nullable = false, length = 64)
    private String followerAction;

    @Enumerated(EnumType.STRING)
    @Column(name = "copy_mode", nullable = false, length = 32)
    private CopyMode copyMode;

    @Column(name = "requested_volume", precision = 18, scale = 6)
    private BigDecimal requestedVolume;

    @Column(name = "requested_price", precision = 18, scale = 10)
    private BigDecimal requestedPrice;

    @Column(name = "requested_sl", precision = 18, scale = 10)
    private BigDecimal requestedSl;

    @Column(name = "requested_tp", precision = 18, scale = 10)
    private BigDecimal requestedTp;

    @Column(name = "master_deal_id")
    private Long masterDealId;

    @Column(name = "master_order_id")
    private Long masterOrderId;

    @Column(name = "master_position_id")
    private Long masterPositionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExecutionCommandStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_reason", length = 64)
    private ExecutionRejectReason rejectReason;

    @Column(name = "reject_message", length = 255)
    private String rejectMessage;

    @Column(name = "signal_time", length = 64)
    private String signalTime;

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
        if (id == null) {
            id = ManualIdGenerator.nextId();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
