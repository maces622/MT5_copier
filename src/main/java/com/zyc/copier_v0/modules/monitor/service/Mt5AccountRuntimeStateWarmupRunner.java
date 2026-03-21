package com.zyc.copier_v0.modules.monitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Mt5AccountRuntimeStateWarmupRunner implements ApplicationRunner {

    private final Mt5AccountRuntimeStateStore runtimeStateStore;

    public Mt5AccountRuntimeStateWarmupRunner(Mt5AccountRuntimeStateStore runtimeStateStore) {
        this.runtimeStateStore = runtimeStateStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        int warmed = runtimeStateStore.warmUpFromDatabase();
        if (warmed > 0) {
            log.info("Runtime-state cache warmup completed, states={}", warmed);
        }
    }
}
