package com.zyc.copier_v0.modules.copy.engine.slippage;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "copier.copy-engine.slippage")
@Getter
@Setter
public class CopyEngineSlippageProperties {

    private boolean enabled = false;
    private BigDecimal fxAndGoldMaxPips = new BigDecimal("10");
    private BigDecimal otherSymbolMaxPrice = new BigDecimal("20.0");
}
