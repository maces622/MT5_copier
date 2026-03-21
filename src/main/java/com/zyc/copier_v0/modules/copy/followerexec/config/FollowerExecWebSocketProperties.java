package com.zyc.copier_v0.modules.copy.followerexec.config;

import java.time.Duration;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.mt5.follower-exec")
@Getter
@Setter
public class FollowerExecWebSocketProperties {

    @NotBlank
    private String path = "/ws/follower-exec";

    @NotBlank
    private String bearerToken = "dev-follower-token";

    private boolean allowQueryToken = true;

    @NotNull
    private Duration heartbeatStaleAfter = Duration.ofSeconds(15);
}
