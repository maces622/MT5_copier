package com.zyc.copier_v0.modules.account.config.cache;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.account-config.route-cache")
@Getter
@Setter
public class AccountRouteCacheProperties {

    @NotBlank
    private String backend = "log";

    @NotBlank
    private String keyPrefix = "copy";

    private boolean warmupOnStartup = true;
}
