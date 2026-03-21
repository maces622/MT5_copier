package com.zyc.copier_v0.modules.copy.engine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.signal.ingest.service.Mt5SignalIngestService;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:copyengine-slippage;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "copier.account-config.route-cache.backend=log",
        "copier.mt5.signal-ingest.bearer-token=test-token",
        "copier.mt5.signal-ingest.dedup-backend=memory",
        "copier.monitor.runtime-state.backend=database",
        "copier.monitor.session-registry.backend=memory",
        "copier.mt5.follower-exec.realtime-dispatch.backend=local",
        "copier.copy-engine.slippage.enabled=true"
})
class CopyEngineSlippageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Mt5SignalIngestService mt5SignalIngestService;

    @Test
    void shouldEnableSlippagePolicyForOpenPositionOnly() throws Exception {
        long masterLogin = 991001L;
        long followerLogin = 991002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9910L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9910L, "FOLLOWER", server, followerLogin);

        saveRiskRule(followerAccountId, 0.10, 1.00, 12.0);
        createRelation(masterAccountId, followerAccountId, "FIXED_LOT");

        mt5SignalIngestService.registerConnection("copy-open-slippage", "trace-open-slippage");
        mt5SignalIngestService.ingest("copy-open-slippage", "trace-open-slippage",
                "{\"type\":\"DEAL\",\"event_id\":\"991001-DEAL-10001\",\"login\":991001,\"server\":\"Broker-Live\",\"deal\":10001,\"order\":10002,\"position\":10003,\"symbol\":\"XAUUSD\",\"action\":\"BUY OPEN\",\"volume\":1.0,\"price\":3025.12,\"deal_type\":0,\"entry\":0,\"magic\":0,\"comment\":\"\",\"time\":\"2026.03.21 15:10:01\",\"symbol_digits\":2,\"symbol_point\":0.01,\"symbol_tick_size\":0.01,\"symbol_tick_value\":1.0,\"symbol_contract_size\":100.0,\"symbol_volume_step\":0.01,\"symbol_volume_min\":0.01,\"symbol_volume_max\":100.0,\"symbol_currency_base\":\"XAU\",\"symbol_currency_profit\":\"USD\",\"symbol_currency_margin\":\"USD\"}");

        JsonNode payload = readDispatchPayload("991001-DEAL-10001");
        org.assertj.core.api.Assertions.assertThat(payload.path("commandType").asText()).isEqualTo("OPEN_POSITION");
        org.assertj.core.api.Assertions.assertThat(payload.path("requestedPrice").decimalValue())
                .isEqualByComparingTo("3025.12");
        org.assertj.core.api.Assertions.assertThat(payload.path("slippagePolicy").path("enabled").asBoolean()).isTrue();
        org.assertj.core.api.Assertions.assertThat(payload.path("slippagePolicy").path("maxPips").decimalValue())
                .isEqualByComparingTo("12");
    }

    @Test
    void shouldDisableSlippagePolicyForClosePositionEvenWhenGlobalSwitchIsOn() throws Exception {
        long masterLogin = 992001L;
        long followerLogin = 992002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9920L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9920L, "FOLLOWER", server, followerLogin);

        saveRiskRule(followerAccountId, 0.10, 1.00, 8.0);
        createRelation(masterAccountId, followerAccountId, "FIXED_LOT");

        mt5SignalIngestService.registerConnection("copy-close-slippage", "trace-close-slippage");
        mt5SignalIngestService.ingest("copy-close-slippage", "trace-close-slippage",
                "{\"type\":\"DEAL\",\"event_id\":\"992001-DEAL-20001\",\"login\":992001,\"server\":\"Broker-Live\",\"deal\":20001,\"order\":20002,\"position\":20003,\"symbol\":\"XAUUSD\",\"action\":\"BUY CLOSE\",\"volume\":1.0,\"price\":3024.88,\"deal_type\":0,\"entry\":1,\"magic\":0,\"comment\":\"\",\"time\":\"2026.03.21 15:11:01\",\"symbol_digits\":2,\"symbol_point\":0.01,\"symbol_tick_size\":0.01,\"symbol_tick_value\":1.0,\"symbol_contract_size\":100.0,\"symbol_volume_step\":0.01,\"symbol_volume_min\":0.01,\"symbol_volume_max\":100.0,\"symbol_currency_base\":\"XAU\",\"symbol_currency_profit\":\"USD\",\"symbol_currency_margin\":\"USD\"}");

        JsonNode payload = readDispatchPayload("992001-DEAL-20001");
        org.assertj.core.api.Assertions.assertThat(payload.path("commandType").asText()).isEqualTo("CLOSE_POSITION");
        org.assertj.core.api.Assertions.assertThat(payload.path("requestedPrice").decimalValue())
                .isEqualByComparingTo("3024.88");
        org.assertj.core.api.Assertions.assertThat(payload.path("slippagePolicy").path("enabled").asBoolean()).isFalse();
    }

    private JsonNode readDispatchPayload(String masterEventId) throws Exception {
        MvcResult dispatchResult = mockMvc.perform(get("/api/execution-commands/dispatches")
                        .param("masterEventId", masterEventId))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode dispatchPayload = objectMapper.readTree(dispatchResult.getResponse().getContentAsString())
                .path(0)
                .path("payloadJson");
        return objectMapper.readTree(dispatchPayload.asText());
    }

    private Long bindAccount(Long userId, String role, String server, long login) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("brokerName", "Broker-" + userId);
        payload.put("serverName", server);
        payload.put("mt5Login", login);
        payload.put("accountRole", role);

        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private void saveRiskRule(Long accountId, Double fixedLot, Double maxLot, Double maxSlippagePips) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", accountId);
        payload.put("fixedLot", fixedLot);
        payload.put("maxLot", maxLot);
        payload.put("maxSlippagePips", maxSlippagePips);

        mockMvc.perform(post("/api/risk-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    private void createRelation(Long masterAccountId, Long followerAccountId, String copyMode) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("masterAccountId", masterAccountId);
        payload.put("followerAccountId", followerAccountId);
        payload.put("copyMode", copyMode);

        mockMvc.perform(post("/api/copy-relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }
}
