package com.zyc.copier_v0.modules.copy.engine.event;

public class FollowerDispatchCreatedEvent {

    private final Long dispatchId;
    private final Long followerAccountId;

    public FollowerDispatchCreatedEvent(Long dispatchId, Long followerAccountId) {
        this.dispatchId = dispatchId;
        this.followerAccountId = followerAccountId;
    }

    public Long getDispatchId() {
        return dispatchId;
    }

    public Long getFollowerAccountId() {
        return followerAccountId;
    }
}
