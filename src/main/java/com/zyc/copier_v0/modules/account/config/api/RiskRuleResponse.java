package com.zyc.copier_v0.modules.account.config.api;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
public class RiskRuleResponse {

    private Long id;
    private Long accountId;
    private BigDecimal maxLot;
    private BigDecimal fixedLot;
    private BigDecimal balanceRatio;
    private Integer maxSlippagePoints;
    private BigDecimal maxSlippagePips;
    private BigDecimal maxSlippagePrice;
    private BigDecimal maxDailyLoss;
    private BigDecimal maxDrawdownPct;
    private String allowedSymbols;
    private String blockedSymbols;
    private boolean followTpSl;
    private boolean reverseFollow;
    private Instant createdAt;
    private Instant updatedAt;
}
