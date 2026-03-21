package com.zyc.copier_v0.modules.copy.followerexec.domain;

import org.springframework.util.StringUtils;

public enum FollowerExecMessageType {
    HELLO,
    HELLO_ACK,
    HEARTBEAT,
    ACK,
    FAIL,
    DISPATCH,
    STATUS_ACK,
    ERROR;

    public static FollowerExecMessageType fromCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Follower exec message type is required");
        }
        try {
            return FollowerExecMessageType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported follower exec message type: " + code);
        }
    }
}
