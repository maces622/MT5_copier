package com.zyc.copier_v0.modules.copy.engine.api;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionCommandType;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionCommandStatus;
import com.zyc.copier_v0.modules.copy.engine.domain.ExecutionRejectReason;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SignalType;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
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
}
