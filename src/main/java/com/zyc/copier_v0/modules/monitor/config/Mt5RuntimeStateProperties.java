package com.zyc.copier_v0.modules.monitor.config;

import java.time.Duration;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.monitor.runtime-state")
@Getter
@Setter
public class Mt5RuntimeStateProperties {

    @NotNull
    private Mt5RuntimeStateBackend backend = Mt5RuntimeStateBackend.DATABASE;

    @NotBlank
    private String keyPrefix = "copy:runtime";

    @NotNull
    private Duration databaseSyncInterval = Duration.ofSeconds(30);

    @NotNull
    private Duration fundsStaleAfter = Duration.ofSeconds(30);

    private boolean requireFreshFundsForRatio = true;

    private boolean warmupOnStartup = true;
}
