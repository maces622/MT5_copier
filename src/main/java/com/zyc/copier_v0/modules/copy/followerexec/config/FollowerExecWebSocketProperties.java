package com.zyc.copier_v0.modules.copy.followerexec.config;

import java.time.Duration;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.mt5.follower-exec")
public class FollowerExecWebSocketProperties {

    @NotBlank
    private String path = "/ws/follower-exec";

    @NotBlank
    private String bearerToken = "dev-follower-token";

    private boolean allowQueryToken = true;

    @NotNull
    private Duration heartbeatStaleAfter = Duration.ofSeconds(15);

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBearerToken() {
        return bearerToken;
    }

    public void setBearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
    }

    public boolean isAllowQueryToken() {
        return allowQueryToken;
    }

    public void setAllowQueryToken(boolean allowQueryToken) {
        this.allowQueryToken = allowQueryToken;
    }

    public Duration getHeartbeatStaleAfter() {
        return heartbeatStaleAfter;
    }

    public void setHeartbeatStaleAfter(Duration heartbeatStaleAfter) {
        this.heartbeatStaleAfter = heartbeatStaleAfter;
    }
}
