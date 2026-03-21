package com.zyc.copier_v0.modules.copy.engine.slippage;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "copier.copy-engine.slippage")
public class CopyEngineSlippageProperties {

    private BigDecimal fxAndGoldMaxPips = new BigDecimal("10");
    private BigDecimal otherSymbolMaxPrice = new BigDecimal("20.0");

    public BigDecimal getFxAndGoldMaxPips() {
        return fxAndGoldMaxPips;
    }

    public void setFxAndGoldMaxPips(BigDecimal fxAndGoldMaxPips) {
        this.fxAndGoldMaxPips = fxAndGoldMaxPips;
    }

    public BigDecimal getOtherSymbolMaxPrice() {
        return otherSymbolMaxPrice;
    }

    public void setOtherSymbolMaxPrice(BigDecimal otherSymbolMaxPrice) {
        this.otherSymbolMaxPrice = otherSymbolMaxPrice;
    }
}
