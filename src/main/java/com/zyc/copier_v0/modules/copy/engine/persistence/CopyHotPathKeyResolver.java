package com.zyc.copier_v0.modules.copy.engine.persistence;

import com.zyc.copier_v0.modules.copy.engine.config.CopyHotPathProperties;
import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import org.springframework.stereotype.Component;

@Component
public class CopyHotPathKeyResolver {

    private final CopyHotPathProperties properties;

    public CopyHotPathKeyResolver(CopyHotPathProperties properties) {
        this.properties = properties;
    }

    public String commandSequenceKey() {
        return properties.getKeyPrefix() + ":seq:command";
    }

    public String dispatchSequenceKey() {
        return properties.getKeyPrefix() + ":seq:dispatch";
    }

    public String signalSequenceKey() {
        return properties.getKeyPrefix() + ":seq:signal";
    }

    public String commandKey(Long commandId) {
        return properties.getKeyPrefix() + ":command:" + commandId;
    }

    public String commandDedupKey(String masterEventId, Long followerAccountId) {
        return properties.getKeyPrefix() + ":command:dedup:" + masterEventId + ":" + followerAccountId;
    }

    public String commandsByMasterEventKey(String masterEventId) {
        return properties.getKeyPrefix() + ":command:index:event:" + masterEventId;
    }

    public String commandsByFollowerKey(Long followerAccountId) {
        return properties.getKeyPrefix() + ":command:index:follower:" + followerAccountId;
    }

    public String commandsByMasterAccountKey(Long masterAccountId) {
        return properties.getKeyPrefix() + ":command:index:master-account:" + masterAccountId;
    }

    public String commandsByMasterOrderKey(Long masterAccountId, Long masterOrderId) {
        return properties.getKeyPrefix() + ":command:index:master-order:" + masterAccountId + ":" + masterOrderId;
    }

    public String commandsByMasterPositionKey(Long masterAccountId, Long masterPositionId) {
        return properties.getKeyPrefix() + ":command:index:master-position:" + masterAccountId + ":" + masterPositionId;
    }

    public String dispatchKey(Long dispatchId) {
        return properties.getKeyPrefix() + ":dispatch:" + dispatchId;
    }

    public String dispatchByCommandKey(Long commandId) {
        return properties.getKeyPrefix() + ":dispatch:index:command:" + commandId;
    }

    public String dispatchesByFollowerKey(Long followerAccountId) {
        return properties.getKeyPrefix() + ":dispatch:index:follower:" + followerAccountId;
    }

    public String dispatchesByFollowerStatusKey(Long followerAccountId, FollowerDispatchStatus status) {
        return properties.getKeyPrefix() + ":dispatch:index:follower-status:" + followerAccountId + ":" + status.name();
    }

    public String pendingDispatchesByFollowerKey(Long followerAccountId) {
        return properties.getKeyPrefix() + ":dispatch:index:follower-pending:" + followerAccountId;
    }

    public String dispatchesByMasterEventKey(String masterEventId) {
        return properties.getKeyPrefix() + ":dispatch:index:event:" + masterEventId;
    }

    public String persistenceQueueKey() {
        return properties.getKeyPrefix() + ":persist:queue";
    }

    public String persistenceDeadLetterKey() {
        return properties.getKeyPrefix() + ":persist:dead-letter";
    }
}
