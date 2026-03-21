package com.zyc.copier_v0.modules.monitor.config;

import java.time.Duration;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.monitor")
public class MonitorProperties {

    @NotNull
    private Duration heartbeatStaleAfter = Duration.ofSeconds(10);

    public Duration getHeartbeatStaleAfter() {
        return heartbeatStaleAfter;
    }

    public void setHeartbeatStaleAfter(Duration heartbeatStaleAfter) {
        this.heartbeatStaleAfter = heartbeatStaleAfter;
    }
}
