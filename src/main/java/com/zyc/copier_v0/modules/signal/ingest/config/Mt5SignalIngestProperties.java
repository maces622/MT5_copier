package com.zyc.copier_v0.modules.signal.ingest.config;

import java.time.Duration;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.mt5.signal-ingest")
@Getter
@Setter
public class Mt5SignalIngestProperties {

    @NotBlank
    private String path = "/ws/trade";

    @NotBlank
    private String bearerToken = "dev-mt5-token";

    private boolean allowQueryToken = true;

    @NotNull
    private Duration dedupTtl = Duration.ofMinutes(5);
}
