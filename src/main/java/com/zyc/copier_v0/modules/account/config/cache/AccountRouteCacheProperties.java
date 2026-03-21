package com.zyc.copier_v0.modules.account.config.cache;

import javax.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.account-config.route-cache")
public class AccountRouteCacheProperties {

    @NotBlank
    private String backend = "log";

    @NotBlank
    private String keyPrefix = "copy";

    private boolean warmupOnStartup = true;

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public boolean isWarmupOnStartup() {
        return warmupOnStartup;
    }

    public void setWarmupOnStartup(boolean warmupOnStartup) {
        this.warmupOnStartup = warmupOnStartup;
    }
}
