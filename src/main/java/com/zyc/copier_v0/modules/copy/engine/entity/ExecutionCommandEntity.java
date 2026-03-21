package com.zyc.copier_v0.modules.copy.engine.entity;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionCommandType;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionCommandStatus;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionRejectReason;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SignalType;
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
public class ExecutionCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    public String getMasterEventId() {
        return masterEventId;
    }

    public void setMasterEventId(String masterEventId) {
        this.masterEventId = masterEventId;
    }

    public Long getMasterAccountId() {
        return masterAccountId;
    }

    public void setMasterAccountId(Long masterAccountId) {
        this.masterAccountId = masterAccountId;
    }

    public String getMasterAccountKey() {
        return masterAccountKey;
    }

    public void setMasterAccountKey(String masterAccountKey) {
        this.masterAccountKey = masterAccountKey;
    }

    public Long getFollowerAccountId() {
        return followerAccountId;
    }

    public void setFollowerAccountId(Long followerAccountId) {
        this.followerAccountId = followerAccountId;
    }

    public String getMasterSymbol() {
        return masterSymbol;
    }

    public void setMasterSymbol(String masterSymbol) {
        this.masterSymbol = masterSymbol;
    }

    public Mt5SignalType getSignalType() {
        return signalType;
    }

    public void setSignalType(Mt5SignalType signalType) {
        this.signalType = signalType;
    }

    public ExecutionCommandType getCommandType() {
        return commandType;
    }

    public void setCommandType(ExecutionCommandType commandType) {
        this.commandType = commandType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getMasterAction() {
        return masterAction;
    }

    public void setMasterAction(String masterAction) {
        this.masterAction = masterAction;
    }

    public String getFollowerAction() {
        return followerAction;
    }

    public void setFollowerAction(String followerAction) {
        this.followerAction = followerAction;
    }

    public CopyMode getCopyMode() {
        return copyMode;
    }

    public void setCopyMode(CopyMode copyMode) {
        this.copyMode = copyMode;
    }

    public BigDecimal getRequestedVolume() {
        return requestedVolume;
    }

    public void setRequestedVolume(BigDecimal requestedVolume) {
        this.requestedVolume = requestedVolume;
    }

    public BigDecimal getRequestedPrice() {
        return requestedPrice;
    }

    public void setRequestedPrice(BigDecimal requestedPrice) {
        this.requestedPrice = requestedPrice;
    }

    public BigDecimal getRequestedSl() {
        return requestedSl;
    }

    public void setRequestedSl(BigDecimal requestedSl) {
        this.requestedSl = requestedSl;
    }

    public BigDecimal getRequestedTp() {
        return requestedTp;
    }

    public void setRequestedTp(BigDecimal requestedTp) {
        this.requestedTp = requestedTp;
    }

    public Long getMasterDealId() {
        return masterDealId;
    }

    public void setMasterDealId(Long masterDealId) {
        this.masterDealId = masterDealId;
    }

    public Long getMasterOrderId() {
        return masterOrderId;
    }

    public void setMasterOrderId(Long masterOrderId) {
        this.masterOrderId = masterOrderId;
    }

    public Long getMasterPositionId() {
        return masterPositionId;
    }

    public void setMasterPositionId(Long masterPositionId) {
        this.masterPositionId = masterPositionId;
    }

    public ExecutionCommandStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionCommandStatus status) {
        this.status = status;
    }

    public ExecutionRejectReason getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(ExecutionRejectReason rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getRejectMessage() {
        return rejectMessage;
    }

    public void setRejectMessage(String rejectMessage) {
        this.rejectMessage = rejectMessage;
    }

    public String getSignalTime() {
        return signalTime;
    }

    public void setSignalTime(String signalTime) {
        this.signalTime = signalTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
