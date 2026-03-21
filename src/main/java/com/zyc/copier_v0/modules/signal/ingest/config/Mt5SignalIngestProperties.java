package com.zyc.copier_v0.modules.signal.ingest.config;

import java.time.Duration;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.mt5.signal-ingest")
public class Mt5SignalIngestProperties {

    @NotBlank
    private String path = "/ws/trade";

    @NotBlank
    private String bearerToken = "dev-mt5-token";

    private boolean allowQueryToken = true;

    @NotNull
    private Duration dedupTtl = Duration.ofMinutes(5);

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

    public Duration getDedupTtl() {
        return dedupTtl;
    }

    public void setDedupTtl(Duration dedupTtl) {
        this.dedupTtl = dedupTtl;
    }
}
