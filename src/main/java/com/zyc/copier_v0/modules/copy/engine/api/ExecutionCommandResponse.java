package com.zyc.copier_v0.modules.copy.engine.api;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionCommandType;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionCommandStatus;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionRejectReason;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SignalType;
import java.math.BigDecimal;
import java.time.Instant;

public class ExecutionCommandResponse {

    private Long id;
    private String masterEventId;
    private Long masterAccountId;
    private String masterAccountKey;
    private Long followerAccountId;
    private String masterSymbol;
    private Mt5SignalType signalType;
    private ExecutionCommandType commandType;
    private String symbol;
    private String masterAction;
    private String followerAction;
    private CopyMode copyMode;
    private BigDecimal requestedVolume;
    private BigDecimal requestedPrice;
    private BigDecimal requestedSl;
    private BigDecimal requestedTp;
    private Long masterDealId;
    private Long masterOrderId;
    private Long masterPositionId;
    private ExecutionCommandStatus status;
    private ExecutionRejectReason rejectReason;
    private String rejectMessage;
    private String signalTime;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
