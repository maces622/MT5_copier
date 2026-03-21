package com.zyc.copier_v0.modules.copy.engine.slippage;

import java.math.BigDecimal;

public class DispatchSlippagePolicy {

    private final boolean enabled;
    private final InstrumentCategory instrumentCategory;
    private final DispatchSlippageMode mode;
    private final BigDecimal maxPips;
    private final BigDecimal maxPrice;

    public DispatchSlippagePolicy(
            boolean enabled,
            InstrumentCategory instrumentCategory,
            DispatchSlippageMode mode,
            BigDecimal maxPips,
            BigDecimal maxPrice
    ) {
        this.enabled = enabled;
        this.instrumentCategory = instrumentCategory;
        this.mode = mode;
        this.maxPips = maxPips;
        this.maxPrice = maxPrice;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public InstrumentCategory getInstrumentCategory() {
        return instrumentCategory;
    }

    public DispatchSlippageMode getMode() {
        return mode;
    }

    public BigDecimal getMaxPips() {
        return maxPips;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }
}
