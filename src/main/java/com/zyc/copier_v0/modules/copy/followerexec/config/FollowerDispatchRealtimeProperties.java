package com.zyc.copier_v0.modules.copy.followerexec.config;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.mt5.follower-exec.realtime-dispatch")
@Getter
@Setter
public class FollowerDispatchRealtimeProperties {

    @NotNull
    private FollowerDispatchRealtimeBackend backend = FollowerDispatchRealtimeBackend.LOCAL;

    @NotBlank
    private String channel = "copy:follower:dispatch";

    @NotBlank
    private String nodeId = "copier-node";
}
