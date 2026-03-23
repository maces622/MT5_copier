package com.zyc.copier_v0.modules.copy.engine.persistence;

public enum CopyHotPathPersistenceMessageType {
    SIGNAL_RECORD_UPSERT,
    EXECUTION_COMMAND_UPSERT,
    FOLLOWER_DISPATCH_UPSERT,
    POSITION_LEDGER_RECONCILE
}
