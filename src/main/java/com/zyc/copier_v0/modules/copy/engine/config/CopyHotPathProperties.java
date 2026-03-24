package com.zyc.copier_v0.modules.copy.engine.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "copier.copy-engine.hot-path")
@Getter
@Setter
public class CopyHotPathProperties {

    @NotNull
    private CopyHotPathBackend backend = CopyHotPathBackend.DATABASE;

    @NotBlank
    private String keyPrefix = "copy:hot";

    private boolean warmupPendingDispatchesOnStartup = true;

    @Min(1)
    private int queueWorkerBatchSize = 200;

    @Min(10)
    private long queueWorkerDelayMs = 200L;

    @Min(0)
    private int followerParallelism = 0;

    public int resolvedFollowerParallelism() {
        return followerParallelism > 0
                ? followerParallelism
                : Runtime.getRuntime().availableProcessors();
    }
}
