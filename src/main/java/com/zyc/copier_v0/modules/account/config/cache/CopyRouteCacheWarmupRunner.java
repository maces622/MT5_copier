package com.zyc.copier_v0.modules.account.config.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "copier.account-config.route-cache.backend", havingValue = "redis")
public class CopyRouteCacheWarmupRunner implements ApplicationRunner {

    private final CopyRouteCacheWarmupService warmupService;
    private final boolean warmupOnStartup;

    public CopyRouteCacheWarmupRunner(
            CopyRouteCacheWarmupService warmupService,
            @Value("${copier.account-config.route-cache.warmup-on-startup:true}") boolean warmupOnStartup
    ) {
        this.warmupService = warmupService;
        this.warmupOnStartup = warmupOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!warmupOnStartup) {
            return;
        }
        warmupService.warmUpAll();
    }
}
