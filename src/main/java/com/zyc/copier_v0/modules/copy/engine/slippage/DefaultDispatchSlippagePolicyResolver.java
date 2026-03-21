package com.zyc.copier_v0.modules.copy.engine.slippage;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyc.copier_v0.modules.account.config.cache.FollowerRiskCacheSnapshot;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DefaultDispatchSlippagePolicyResolver implements DispatchSlippagePolicyResolver {

    private static final Set<String> FX_CURRENCY_CODES = Set.of(
            "USD", "EUR", "GBP", "JPY", "CHF", "AUD", "NZD", "CAD",
            "CNH", "HKD", "SGD", "SEK", "NOK", "DKK", "TRY", "ZAR",
            "MXN", "PLN", "CZK", "HUF"
    );

    private final CopyEngineSlippageProperties properties;

    public DefaultDispatchSlippagePolicyResolver(CopyEngineSlippageProperties properties) {
        this.properties = properties;
    }

    @Override
    public DispatchSlippagePolicy resolve(JsonNode signalPayload, FollowerRiskCacheSnapshot riskSnapshot) {
        InstrumentCategory category = classify(signalPayload);
        if (category == InstrumentCategory.FOREX || category == InstrumentCategory.GOLD) {
            BigDecimal maxPips = positiveOrDefault(
                    riskSnapshot == null ? null : riskSnapshot.getMaxSlippagePips(),
                    properties.getFxAndGoldMaxPips()
            );
            return new DispatchSlippagePolicy(category, DispatchSlippageMode.PIPS, maxPips, null);
        }

        BigDecimal maxPrice = positiveOrDefault(
                riskSnapshot == null ? null : riskSnapshot.getMaxSlippagePrice(),
                properties.getOtherSymbolMaxPrice()
        );
        return new DispatchSlippagePolicy(category, DispatchSlippageMode.PRICE, null, maxPrice);
    }

    private InstrumentCategory classify(JsonNode signalPayload) {
        String symbol = normalize(readText(signalPayload, "symbol"));
        String baseCurrency = normalize(readText(signalPayload, "symbol_currency_base"));
        String profitCurrency = normalize(readText(signalPayload, "symbol_currency_profit"));

        if (looksLikeGold(symbol, baseCurrency)) {
            return InstrumentCategory.GOLD;
        }
        if (looksLikeForex(symbol, baseCurrency, profitCurrency)) {
            return InstrumentCategory.FOREX;
        }
        return InstrumentCategory.OTHER;
    }

    private boolean looksLikeGold(String symbol, String baseCurrency) {
        return "XAU".equals(baseCurrency)
                || symbol.startsWith("XAU")
                || symbol.contains("GOLD");
    }

    private boolean looksLikeForex(String symbol, String baseCurrency, String profitCurrency) {
        if (FX_CURRENCY_CODES.contains(baseCurrency) && FX_CURRENCY_CODES.contains(profitCurrency)) {
            return true;
        }
        if (symbol.length() < 6) {
            return false;
        }
        String compact = symbol.replaceAll("[^A-Z]", "");
        if (compact.length() < 6) {
            return false;
        }
        String base = compact.substring(0, 3);
        String quote = compact.substring(3, 6);
        return FX_CURRENCY_CODES.contains(base) && FX_CURRENCY_CODES.contains(quote);
    }

    private String readText(JsonNode payload, String field) {
        if (payload == null || !payload.hasNonNull(field)) {
            return null;
        }
        return payload.path(field).asText();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    private BigDecimal positiveOrDefault(BigDecimal value, BigDecimal fallback) {
        if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
            return value;
        }
        return fallback;
    }
}
