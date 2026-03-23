package com.zyc.copier_v0.modules.monitor.service;

import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
public class Mt5OpenPositionSnapshot {

    private String positionKey;
    private Long sourcePositionId;
    private Long sourceOrderId;
    private Long masterPositionId;
    private Long masterOrderId;
    private String symbol;
    private BigDecimal volume;
    private BigDecimal priceOpen;
    private BigDecimal sl;
    private BigDecimal tp;
    private String commentText;
    private Instant observedAt;
}
