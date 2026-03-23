package com.zyc.copier_v0.support;

import java.util.concurrent.atomic.AtomicLong;

public final class ManualIdGenerator {

    private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis() * 1000L);

    private ManualIdGenerator() {
    }

    public static long nextId() {
        return SEQUENCE.incrementAndGet();
    }
}
