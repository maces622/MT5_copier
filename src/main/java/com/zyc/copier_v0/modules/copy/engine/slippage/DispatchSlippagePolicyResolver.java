package com.zyc.copier_v0.modules.copy.engine.slippage;

import com.fasterxml.jackson.databind.JsonNode;
import com.zyc.copier_v0.modules.account.config.cache.FollowerRiskCacheSnapshot;

public interface DispatchSlippagePolicyResolver {

    DispatchSlippagePolicy resolve(JsonNode signalPayload, FollowerRiskCacheSnapshot riskSnapshot);
}
