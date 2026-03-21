package com.zyc.copier_v0.modules.copy.followerexec.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FollowerDispatchRealtimeMessage {

    private Long dispatchId;
    private Long followerAccountId;
    private String publisherNodeId;
}
