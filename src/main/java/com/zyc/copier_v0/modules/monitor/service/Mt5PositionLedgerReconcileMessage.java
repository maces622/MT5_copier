package com.zyc.copier_v0.modules.monitor.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Mt5PositionLedgerReconcileMessage {

    private Long accountId;
    private Long login;
    private String server;
    private String accountKey;
    private Instant observedAt;
    private List<Mt5OpenPositionSnapshot> positions = new ArrayList<>();
}
