package com.zyc.copier_v0.modules.copy.engine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:copyengine;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "copier.account-config.route-cache.backend=log",
        "copier.mt5.signal-ingest.bearer-token=test-token"
})
class CopyEngineIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Mt5SignalIngestService mt5SignalIngestService;

    @Test
    void shouldGenerateReadyCommandForFixedLotFollower() throws Exception {
        long masterLogin = 910001L;
        long followerLogin = 910002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9001L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9001L, "FOLLOWER", server, followerLogin);

        saveRiskRule(followerAccountId, 0.10, 1.00, null, null);
        createRelation(masterAccountId, followerAccountId, "FIXED_LOT");

        mt5SignalIngestService.registerConnection("copy-test-session-1", "trace-1");
        mt5SignalIngestService.ingest("copy-test-session-1", "trace-1",
                "{\"type\":\"DEAL\",\"event_id\":\"910001-DEAL-10001\",\"login\":910001,\"server\":\"Broker-Live\",\"deal\":10001,\"order\":10002,\"position\":10003,\"symbol\":\"XAUUSD\",\"action\":\"BUY OPEN\",\"volume\":1.0,\"price\":3025.12,\"deal_type\":0,\"entry\":0,\"magic\":0,\"comment\":\"\",\"time\":\"2026.03.20 15:20:01\"}");
        mt5SignalIngestService.ingest("copy-test-session-1", "trace-1",
                "{\"type\":\"DEAL\",\"event_id\":\"910001-DEAL-10001\",\"login\":910001,\"server\":\"Broker-Live\",\"deal\":10001,\"order\":10002,\"position\":10003,\"symbol\":\"XAUUSD\",\"action\":\"BUY OPEN\",\"volume\":1.0,\"price\":3025.12,\"deal_type\":0,\"entry\":0,\"magic\":0,\"comment\":\"\",\"time\":\"2026.03.20 15:20:01\"}");

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "910001-DEAL-10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followerAccountId").value(followerAccountId))
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[0].followerAction").value("BUY OPEN"))
                .andExpect(jsonPath("$[0].requestedVolume").value(0.1000))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/execution-commands/dispatches").param("masterEventId", "910001-DEAL-10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followerAccountId").value(followerAccountId))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"symbol\":\"XAUUSD\"")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"followerAction\":\"BUY OPEN\"")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"masterOrderId\":10002")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"masterPositionId\":10003")));

        mockMvc.perform(get("/api/monitor/runtime-states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountKey").value("Broker-Live:910001"))
                .andExpect(jsonPath("$[0].connectionStatus").value("CONNECTED"));

        mockMvc.perform(get("/api/monitor/signals").param("accountKey", "Broker-Live:910001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("910001-DEAL-10001"));
    }

    @Test
    void shouldGenerateRejectedCommandWhenSymbolIsBlocked() throws Exception {
        long masterLogin = 920001L;
        long followerLogin = 920002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9002L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9002L, "FOLLOWER", server, followerLogin);

        saveRiskRule(followerAccountId, null, 5.00, null, "XAUUSD");
        createRelation(masterAccountId, followerAccountId, "FOLLOW_MASTER");

        mt5SignalIngestService.registerConnection("copy-test-session-2", "trace-2");
        mt5SignalIngestService.ingest("copy-test-session-2", "trace-2",
                "{\"type\":\"DEAL\",\"event_id\":\"920001-DEAL-20001\",\"login\":920001,\"server\":\"Broker-Live\",\"deal\":20001,\"order\":20002,\"position\":20003,\"symbol\":\"XAUUSD\",\"action\":\"SELL OPEN\",\"volume\":1.5,\"price\":3020.12,\"deal_type\":1,\"entry\":0,\"magic\":0,\"comment\":\"\",\"time\":\"2026.03.20 15:22:01\"}");

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "920001-DEAL-20001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followerAccountId").value(followerAccountId))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].rejectReason").value("SYMBOL_BLOCKED"));

        mockMvc.perform(get("/api/execution-commands/dispatches").param("masterEventId", "920001-DEAL-20001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldMapMasterSymbolToFollowerSymbolBeforeDispatch() throws Exception {
        long masterLogin = 925001L;
        long followerLogin = 925002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9008L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9008L, "FOLLOWER", server, followerLogin);

        saveRiskRuleWithAllowedSymbols(followerAccountId, 0.10, 1.00, "XAUUSDm");
        saveSymbolMapping(followerAccountId, "XAUUSD", "XAUUSDm");
        createRelation(masterAccountId, followerAccountId, "FIXED_LOT");

        mt5SignalIngestService.registerConnection("copy-test-session-map", "trace-map");
        mt5SignalIngestService.ingest("copy-test-session-map", "trace-map",
                "{\"type\":\"DEAL\",\"event_id\":\"925001-DEAL-25001\",\"login\":925001,\"server\":\"Broker-Live\",\"deal\":25001,\"order\":25002,\"position\":25003,\"symbol\":\"XAUUSD\",\"action\":\"BUY OPEN\",\"volume\":1.0,\"price\":3025.12,\"deal_type\":0,\"entry\":0,\"magic\":0,\"comment\":\"mapped\",\"time\":\"2026.03.20 15:21:01\"}");

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "925001-DEAL-25001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[0].masterSymbol").value("XAUUSD"))
                .andExpect(jsonPath("$[0].symbol").value("XAUUSDm"));

        mockMvc.perform(get("/api/execution-commands/dispatches").param("masterEventId", "925001-DEAL-25001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"symbol\":\"XAUUSDm\"")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"masterSymbol\":\"XAUUSD\"")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"masterSignal\":{\"login\":925001")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"server\":\"Broker-Live\",\"symbol\":\"XAUUSD\"")));
    }

    @Test
    void shouldExposeOverviewAndDispatchLifecycleForReverseFollower() throws Exception {
        long masterLogin = 930001L;
        long followerLogin = 930002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9003L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9003L, "FOLLOWER", server, followerLogin);

        saveRiskRule(followerAccountId, 0.20, 5.00, null, null, 25, true, true);
        createRelation(masterAccountId, followerAccountId, "FIXED_LOT");

        mt5SignalIngestService.registerConnection("copy-test-session-3", "trace-3");
        mt5SignalIngestService.ingest("copy-test-session-3", "trace-3",
                "{\"type\":\"HELLO\",\"login\":930001,\"server\":\"Broker-Live\",\"ts\":\"2026.03.20 15:24:00\"}");
        mt5SignalIngestService.ingest("copy-test-session-3", "trace-3",
                "{\"type\":\"HEARTBEAT\",\"login\":930001,\"server\":\"Broker-Live\",\"ts\":\"2026.03.20 15:24:01\"}");
        mt5SignalIngestService.ingest("copy-test-session-3", "trace-3",
                "{\"type\":\"DEAL\",\"event_id\":\"930001-DEAL-30001\",\"login\":930001,\"server\":\"Broker-Live\",\"deal\":30001,\"order\":30002,\"position\":30003,\"symbol\":\"EURUSD\",\"action\":\"BUY OPEN\",\"volume\":1.2,\"price\":1.08567,\"deal_type\":0,\"entry\":0,\"magic\":7,\"comment\":\"reverse\",\"time\":\"2026.03.20 15:24:02\"}");

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "930001-DEAL-30001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[0].requestedVolume").value(0.2000))
                .andExpect(jsonPath("$[0].masterAction").value("BUY OPEN"))
                .andExpect(jsonPath("$[0].followerAction").value("SELL OPEN"));

        MvcResult dispatchResult = mockMvc.perform(get("/api/execution-commands/dispatches/followers/" + followerAccountId)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"reverseFollow\":true")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"followerAction\":\"SELL OPEN\"")))
                .andReturn();

        long dispatchId = objectMapper.readTree(dispatchResult.getResponse().getContentAsString()).path(0).path("id").asLong();

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("status", "FAILED");
        updatePayload.put("statusMessage", "follower ws offline");

        mockMvc.perform(patch("/api/execution-commands/dispatches/" + dispatchId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.statusMessage").value("follower ws offline"));

        mockMvc.perform(get("/api/execution-commands/dispatches/followers/" + followerAccountId)
                        .param("status", "FAILED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("FAILED"))
                .andExpect(jsonPath("$[0].statusMessage").value("follower ws offline"));

        JsonNode overview = objectMapper.readTree(mockMvc.perform(get("/api/monitor/accounts/overview").param("userId", "9003"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());

        JsonNode masterOverview = findByAccountId(overview, masterAccountId);
        JsonNode followerOverview = findByAccountId(overview, followerAccountId);

        org.assertj.core.api.Assertions.assertThat(masterOverview).isNotNull();
        org.assertj.core.api.Assertions.assertThat(followerOverview).isNotNull();
        org.assertj.core.api.Assertions.assertThat(masterOverview.path("connectionStatus").asText()).isEqualTo("CONNECTED");
        org.assertj.core.api.Assertions.assertThat(masterOverview.path("activeFollowerCount").asLong()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(followerOverview.path("activeMasterCount").asLong()).isEqualTo(1L);
        org.assertj.core.api.Assertions.assertThat(followerOverview.path("pendingDispatchCount").asLong()).isEqualTo(0L);
        org.assertj.core.api.Assertions.assertThat(followerOverview.path("failedDispatchCount").asLong()).isEqualTo(1L);
    }

    @Test
    void shouldGenerateOrderSyncCommandWhenFollowTpSlEnabled() throws Exception {
        long masterLogin = 940001L;
        long followerLogin = 940002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9004L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9004L, "FOLLOWER", server, followerLogin);

        saveRiskRule(followerAccountId, null, null, null, null, 15, true, false);
        createRelation(masterAccountId, followerAccountId, "FOLLOW_MASTER");

        mt5SignalIngestService.registerConnection("copy-test-session-4", "trace-4");
        mt5SignalIngestService.ingest("copy-test-session-4", "trace-4",
                "{\"type\":\"ORDER\",\"event\":\"ORDER_UPDATE\",\"scope\":\"ACTIVE\",\"event_id\":\"940001-ORDER_UPDATE-40001\",\"login\":940001,\"server\":\"Broker-Live\",\"order\":40001,\"symbol\":\"EURUSD\",\"order_type\":0,\"order_state\":1,\"vol_init\":1.0000,\"vol_cur\":1.0000,\"price_open\":1.08567,\"sl\":1.08000,\"tp\":1.09500,\"magic\":9,\"comment\":\"sync\",\"time_setup\":\"2026.03.20 15:30:00\",\"time_done\":\"1970.01.01 00:00:00\"}");

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "940001-ORDER_UPDATE-40001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].signalType").value("ORDER"))
                .andExpect(jsonPath("$[0].commandType").value("SYNC_TP_SL"))
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[0].masterAction").value("ORDER_UPDATE"))
                .andExpect(jsonPath("$[0].followerAction").value("SYNC_TP_SL"))
                .andExpect(jsonPath("$[0].requestedSl").value(1.0800000000))
                .andExpect(jsonPath("$[0].requestedTp").value(1.0950000000))
                .andExpect(jsonPath("$[0].masterOrderId").value(40001));

        mockMvc.perform(get("/api/execution-commands/dispatches").param("masterEventId", "940001-ORDER_UPDATE-40001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"commandType\":\"SYNC_TP_SL\"")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"requestedSl\":1.08")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"requestedTp\":1.095")));
    }

    @Test
    void shouldRejectOrderSyncWhenFollowTpSlDisabled() throws Exception {
        long masterLogin = 950001L;
        long followerLogin = 950002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9005L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9005L, "FOLLOWER", server, followerLogin);

        saveRiskRule(followerAccountId, null, null, null, null, null, false, false);
        createRelation(masterAccountId, followerAccountId, "FOLLOW_MASTER");

        mt5SignalIngestService.registerConnection("copy-test-session-5", "trace-5");
        mt5SignalIngestService.ingest("copy-test-session-5", "trace-5",
                "{\"type\":\"ORDER\",\"event\":\"ORDER_UPDATE\",\"scope\":\"ACTIVE\",\"event_id\":\"950001-ORDER_UPDATE-50001\",\"login\":950001,\"server\":\"Broker-Live\",\"order\":50001,\"symbol\":\"EURUSD\",\"order_type\":0,\"order_state\":1,\"vol_init\":1.0000,\"vol_cur\":1.0000,\"price_open\":1.08567,\"sl\":1.08000,\"tp\":1.09500,\"magic\":9,\"comment\":\"disabled\",\"time_setup\":\"2026.03.20 15:32:00\",\"time_done\":\"1970.01.01 00:00:00\"}");

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "950001-ORDER_UPDATE-50001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].signalType").value("ORDER"))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].rejectReason").value("FOLLOW_TP_SL_DISABLED"));

        mockMvc.perform(get("/api/execution-commands/dispatches").param("masterEventId", "950001-ORDER_UPDATE-50001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldGeneratePendingOrderCommands() throws Exception {
        long masterLogin = 960001L;
        long followerLogin = 960002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9006L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9006L, "FOLLOWER", server, followerLogin);

        saveRiskRule(followerAccountId, null, 5.00, 0.50, null, null, false, false);
        createRelation(masterAccountId, followerAccountId, "BALANCE_RATIO");

        mt5SignalIngestService.registerConnection("copy-test-session-6", "trace-6");
        mt5SignalIngestService.ingest("copy-test-session-6", "trace-6",
                "{\"type\":\"ORDER\",\"event\":\"ORDER_ADD\",\"scope\":\"ACTIVE\",\"event_id\":\"960001-ORDER_ADD-60001\",\"login\":960001,\"server\":\"Broker-Live\",\"order\":60001,\"symbol\":\"EURUSD\",\"order_type\":2,\"order_state\":1,\"vol_init\":2.0000,\"vol_cur\":2.0000,\"price_open\":1.08100,\"sl\":1.07900,\"tp\":1.08900,\"magic\":11,\"comment\":\"pending-add\",\"time_setup\":\"2026.03.20 15:36:00\",\"time_done\":\"1970.01.01 00:00:00\"}");
        mt5SignalIngestService.ingest("copy-test-session-6", "trace-6",
                "{\"type\":\"ORDER\",\"event\":\"ORDER_UPDATE\",\"scope\":\"ACTIVE\",\"event_id\":\"960001-ORDER_UPDATE-60001\",\"login\":960001,\"server\":\"Broker-Live\",\"order\":60001,\"symbol\":\"EURUSD\",\"order_type\":2,\"order_state\":1,\"vol_init\":2.0000,\"vol_cur\":1.5000,\"price_open\":1.08200,\"sl\":1.08000,\"tp\":1.09000,\"magic\":11,\"comment\":\"pending-update\",\"time_setup\":\"2026.03.20 15:36:10\",\"time_done\":\"1970.01.01 00:00:00\"}");
        mt5SignalIngestService.ingest("copy-test-session-6", "trace-6",
                "{\"type\":\"ORDER\",\"event\":\"ORDER_DELETE\",\"scope\":\"HISTORY\",\"event_id\":\"960001-ORDER_DELETE-60001\",\"login\":960001,\"server\":\"Broker-Live\",\"order\":60001,\"symbol\":\"EURUSD\",\"order_type\":2,\"order_state\":4,\"vol_init\":2.0000,\"vol_cur\":0.0000,\"price_open\":1.08200,\"sl\":1.08000,\"tp\":1.09000,\"magic\":11,\"comment\":\"pending-delete\",\"time_setup\":\"2026.03.20 15:36:10\",\"time_done\":\"2026.03.20 15:37:00\"}");

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "960001-ORDER_ADD-60001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commandType").value("CREATE_PENDING_ORDER"))
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[0].requestedVolume").value(1.0000))
                .andExpect(jsonPath("$[0].requestedPrice").value(1.0810000000));

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "960001-ORDER_UPDATE-60001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commandType").value("UPDATE_PENDING_ORDER"))
                .andExpect(jsonPath("$[0].status").value("READY"))
                .andExpect(jsonPath("$[0].requestedVolume").value(0.7500))
                .andExpect(jsonPath("$[0].requestedPrice").value(1.0820000000))
                .andExpect(jsonPath("$[0].requestedSl").value(1.0800000000))
                .andExpect(jsonPath("$[0].requestedTp").value(1.0900000000));

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "960001-ORDER_DELETE-60001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commandType").value("CANCEL_PENDING_ORDER"))
                .andExpect(jsonPath("$[0].status").value("READY"));

        mockMvc.perform(get("/api/execution-commands/dispatches").param("masterEventId", "960001-ORDER_ADD-60001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"commandType\":\"CREATE_PENDING_ORDER\"")))
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"masterOrderType\":2")));

        mockMvc.perform(get("/api/execution-commands/dispatches").param("masterEventId", "960001-ORDER_DELETE-60001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payloadJson").value(org.hamcrest.Matchers.containsString("\"commandType\":\"CANCEL_PENDING_ORDER\"")));

        mockMvc.perform(get("/api/execution-commands/order-trace")
                        .param("masterAccountId", masterAccountId.toString())
                        .param("masterOrderId", "60001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masterAccountId").value(masterAccountId))
                .andExpect(jsonPath("$.masterOrderId").value(60001))
                .andExpect(jsonPath("$.commands.length()").value(3))
                .andExpect(jsonPath("$.dispatches.length()").value(3));
    }

    @Test
    void shouldIgnoreMarketOrderLifecycleEventsWhenDealAlsoExists() throws Exception {
        long masterLogin = 970001L;
        long followerLogin = 970002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9007L, "MASTER", server, masterLogin);
        Long followerAccountId = bindAccount(9007L, "FOLLOWER", server, followerLogin);

        saveRiskRule(followerAccountId, 0.10, 1.00, null, null);
        createRelation(masterAccountId, followerAccountId, "FIXED_LOT");

        mt5SignalIngestService.registerConnection("copy-test-session-7", "trace-7");
        mt5SignalIngestService.ingest("copy-test-session-7", "trace-7",
                "{\"type\":\"ORDER\",\"event\":\"ORDER_ADD\",\"scope\":\"ACTIVE\",\"event_id\":\"970001-ORDER_ADD-70001\",\"login\":970001,\"server\":\"Broker-Live\",\"order\":70001,\"symbol\":\"XAUUSD\",\"order_type\":0,\"order_state\":1,\"vol_init\":1.0000,\"vol_cur\":1.0000,\"price_open\":3022.10,\"sl\":3018.00,\"tp\":3030.00,\"magic\":0,\"comment\":\"market-open\",\"time_setup\":\"2026.03.20 15:38:00\",\"time_done\":\"1970.01.01 00:00:00\"}");
        mt5SignalIngestService.ingest("copy-test-session-7", "trace-7",
                "{\"type\":\"DEAL\",\"event_id\":\"970001-DEAL-70002\",\"login\":970001,\"server\":\"Broker-Live\",\"deal\":70002,\"order\":70001,\"position\":70003,\"symbol\":\"XAUUSD\",\"action\":\"BUY OPEN\",\"volume\":1.0,\"price\":3022.10,\"deal_type\":0,\"entry\":0,\"magic\":0,\"comment\":\"market-open\",\"time\":\"2026.03.20 15:38:01\"}");
        mt5SignalIngestService.ingest("copy-test-session-7", "trace-7",
                "{\"type\":\"ORDER\",\"event\":\"ORDER_DELETE\",\"scope\":\"HISTORY\",\"event_id\":\"970001-ORDER_DELETE-70001\",\"login\":970001,\"server\":\"Broker-Live\",\"order\":70001,\"symbol\":\"XAUUSD\",\"order_type\":0,\"order_state\":4,\"vol_init\":1.0000,\"vol_cur\":0.0000,\"price_open\":3022.10,\"sl\":3018.00,\"tp\":3030.00,\"magic\":0,\"comment\":\"market-open\",\"time_setup\":\"2026.03.20 15:38:00\",\"time_done\":\"2026.03.20 15:38:01\"}");

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "970001-DEAL-70002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].commandType").value("OPEN_POSITION"))
                .andExpect(jsonPath("$[0].status").value("READY"));

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "970001-ORDER_ADD-70001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].rejectReason").value("ORDER_EVENT_NOT_SUPPORTED"))
                .andExpect(jsonPath("$[0].followerAction").value("IGNORED"));

        mockMvc.perform(get("/api/execution-commands").param("masterEventId", "970001-ORDER_DELETE-70001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[0].rejectReason").value("ORDER_EVENT_NOT_SUPPORTED"));

        mockMvc.perform(get("/api/execution-commands/dispatches").param("masterEventId", "970001-ORDER_ADD-70001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/execution-commands/dispatches").param("masterEventId", "970001-ORDER_DELETE-70001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/api/execution-commands/order-trace")
                        .param("masterAccountId", masterAccountId.toString())
                        .param("masterOrderId", "70001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commands.length()").value(3))
                .andExpect(jsonPath("$.dispatches.length()").value(1));

        mockMvc.perform(get("/api/execution-commands/position-trace")
                        .param("masterAccountId", masterAccountId.toString())
                        .param("masterPositionId", "70003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commands.length()").value(1))
                .andExpect(jsonPath("$.dispatches.length()").value(1))
                .andExpect(jsonPath("$.commands[0].commandType").value("OPEN_POSITION"));
    }

    private Long bindAccount(Long userId, String role, String server, long login) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("brokerName", "Broker-" + userId);
        payload.put("serverName", server);
        payload.put("mt5Login", login);
        payload.put("credential", "secret");
        payload.put("accountRole", role);

        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private void saveRiskRule(Long accountId, Double fixedLot, Double maxLot, Double balanceRatio, String blockedSymbols) throws Exception {
        saveRiskRule(accountId, fixedLot, maxLot, balanceRatio, blockedSymbols, null, null, null);
    }

    private void saveRiskRuleWithAllowedSymbols(Long accountId, Double fixedLot, Double maxLot, String allowedSymbols) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", accountId);
        payload.put("fixedLot", fixedLot);
        payload.put("maxLot", maxLot);
        payload.put("allowedSymbols", allowedSymbols);

        mockMvc.perform(post("/api/risk-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    private void saveRiskRule(
            Long accountId,
            Double fixedLot,
            Double maxLot,
            Double balanceRatio,
            String blockedSymbols,
            Integer maxSlippagePoints,
            Boolean followTpSl,
            Boolean reverseFollow
    ) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("accountId", accountId);
        if (fixedLot != null) {
            payload.put("fixedLot", fixedLot);
        }
        if (maxLot != null) {
            payload.put("maxLot", maxLot);
        }
        if (balanceRatio != null) {
            payload.put("balanceRatio", balanceRatio);
        }
        if (blockedSymbols != null) {
            payload.put("blockedSymbols", blockedSymbols);
        }
        if (maxSlippagePoints != null) {
            payload.put("maxSlippagePoints", maxSlippagePoints);
        }
        if (followTpSl != null) {
            payload.put("followTpSl", followTpSl);
        }
        if (reverseFollow != null) {
            payload.put("reverseFollow", reverseFollow);
        }

        mockMvc.perform(post("/api/risk-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    private void saveSymbolMapping(Long followerAccountId, String masterSymbol, String followerSymbol) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("followerAccountId", followerAccountId);
        payload.put("masterSymbol", masterSymbol);
        payload.put("followerSymbol", followerSymbol);

        mockMvc.perform(post("/api/symbol-mappings")
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

    private JsonNode findByAccountId(JsonNode overview, Long accountId) {
        for (JsonNode item : overview) {
            if (item.path("accountId").asLong() == accountId) {
                return item;
            }
        }
        return null;
    }
}
