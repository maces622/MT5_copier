package com.zyc.copier_v0.modules.copy.engine.domain;

public enum ExecutionCommandType {
    OPEN_POSITION,
    CLOSE_POSITION,
    SYNC_TP_SL,
    CREATE_PENDING_ORDER,
    UPDATE_PENDING_ORDER,
    CANCEL_PENDING_ORDER
}
