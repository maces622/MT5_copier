package com.zyc.copier_v0.modules.copy.engine.slippage;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DispatchSlippagePolicy {

    private final boolean enabled;
    private final InstrumentCategory instrumentCategory;
    private final DispatchSlippageMode mode;
    private final BigDecimal maxPips;
    private final BigDecimal maxPrice;
}
