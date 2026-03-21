package com.zyc.copier_v0.modules.copy.engine.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FollowerDispatchCreatedEvent {

    private final Long dispatchId;
    private final Long followerAccountId;
}
