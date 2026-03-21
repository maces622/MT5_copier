package com.zyc.copier_v0.modules.copy.engine.slippage;

import java.math.BigDecimal;

public class DispatchSlippagePolicy {

    private final InstrumentCategory instrumentCategory;
    private final DispatchSlippageMode mode;
    private final BigDecimal maxPips;
    private final BigDecimal maxPrice;

    public DispatchSlippagePolicy(
            InstrumentCategory instrumentCategory,
            DispatchSlippageMode mode,
            BigDecimal maxPips,
            BigDecimal maxPrice
    ) {
        this.instrumentCategory = instrumentCategory;
        this.mode = mode;
        this.maxPips = maxPips;
        this.maxPrice = maxPrice;
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
