package com.zyc.copier_v0.modules.monitor.config;

import java.time.Duration;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.monitor")
@Getter
@Setter
public class MonitorProperties {

    @NotNull
    private Duration heartbeatStaleAfter = Duration.ofSeconds(10);
}
