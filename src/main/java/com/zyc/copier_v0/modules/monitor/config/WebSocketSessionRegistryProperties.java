package com.zyc.copier_v0.modules.monitor.config;

import java.time.Duration;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.monitor.session-registry")
@Getter
@Setter
public class WebSocketSessionRegistryProperties {

    @NotNull
    private WebSocketSessionRegistryBackend backend = WebSocketSessionRegistryBackend.MEMORY;

    @NotBlank
    private String keyPrefix = "copy:ws";

    @NotNull
    private Duration ttl = Duration.ofMinutes(2);
}
