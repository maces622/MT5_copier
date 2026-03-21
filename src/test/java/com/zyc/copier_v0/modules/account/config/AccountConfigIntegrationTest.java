package com.zyc.copier_v0.modules.account.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
        "spring.datasource.url=jdbc:h2:mem:accountconfig;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "copier.account-config.route-cache.backend=log",
        "copier.mt5.signal-ingest.dedup-backend=memory",
        "copier.monitor.session-registry.backend=memory",
        "copier.mt5.follower-exec.realtime-dispatch.backend=local"
})
class AccountConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldBindAccountsCreateRiskRuleAndCreateRelation() throws Exception {
        Long masterAccountId = bindAccount(1001L, "MASTER");
        Long followerAccountId = bindAccount(1001L, "FOLLOWER");

        Map<String, Object> riskRulePayload = new HashMap<>();
        riskRulePayload.put("accountId", followerAccountId);
        riskRulePayload.put("fixedLot", 0.10);
        riskRulePayload.put("maxLot", 1.50);
        riskRulePayload.put("followTpSl", true);

        mockMvc.perform(post("/api/risk-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(riskRulePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(followerAccountId))
                .andExpect(jsonPath("$.fixedLot").value(0.10))
                .andExpect(jsonPath("$.followTpSl").value(true));

        Map<String, Object> relationPayload = new HashMap<>();
        relationPayload.put("masterAccountId", masterAccountId);
        relationPayload.put("followerAccountId", followerAccountId);
        relationPayload.put("copyMode", "FIXED_LOT");
        relationPayload.put("priority", 10);

        mockMvc.perform(post("/api/copy-relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relationPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masterAccountId").value(masterAccountId))
                .andExpect(jsonPath("$.followerAccountId").value(followerAccountId))
                .andExpect(jsonPath("$.copyMode").value("FIXED_LOT"))
                .andExpect(jsonPath("$.configVersion").value(1));

        mockMvc.perform(get("/api/copy-relations/master/{masterAccountId}", masterAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].followerAccountId").value(followerAccountId));
    }

    @Test
    void shouldAllowBindingWebsocketOnlyAccountWithoutCredential() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1100L);
        payload.put("brokerName", "EBC");
        payload.put("serverName", "EBCFinancialGroupKY-Demo");
        payload.put("mt5Login", 51629L);
        payload.put("accountRole", "FOLLOWER");
        payload.put("status", "ACTIVE");

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serverName").value("EBCFinancialGroupKY-Demo"))
                .andExpect(jsonPath("$.mt5Login").value(51629))
                .andExpect(jsonPath("$.credentialConfigured").value(false))
                .andExpect(jsonPath("$.credentialVersion").value(0));
    }

    @Test
    void shouldRejectCycleWhenCreatingReverseRelation() throws Exception {
        Long accountA = bindAccount(2002L, "BOTH");
        Long accountB = bindAccount(2002L, "BOTH");

        Map<String, Object> relationPayload = new HashMap<>();
        relationPayload.put("masterAccountId", accountA);
        relationPayload.put("followerAccountId", accountB);
        relationPayload.put("copyMode", "FOLLOW_MASTER");

        mockMvc.perform(post("/api/copy-relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relationPayload)))
                .andExpect(status().isOk());

        Map<String, Object> reversePayload = new HashMap<>();
        reversePayload.put("masterAccountId", accountB);
        reversePayload.put("followerAccountId", accountA);
        reversePayload.put("copyMode", "FOLLOW_MASTER");

        mockMvc.perform(post("/api/copy-relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reversePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Copy relation would create a cycle"));
    }

    @Test
    void shouldUpdateRelationAndIncreaseVersion() throws Exception {
        Long masterAccountId = bindAccount(3003L, "MASTER");
        Long followerAccountId = bindAccount(3003L, "FOLLOWER");

        Map<String, Object> relationPayload = new HashMap<>();
        relationPayload.put("masterAccountId", masterAccountId);
        relationPayload.put("followerAccountId", followerAccountId);
        relationPayload.put("copyMode", "FOLLOW_MASTER");

        MvcResult createResult = mockMvc.perform(post("/api/copy-relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relationPayload)))
                .andExpect(status().isOk())
                .andReturn();

        Long relationId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("copyMode", "BALANCE_RATIO");
        updatePayload.put("priority", 5);
        updatePayload.put("status", "PAUSED");

        mockMvc.perform(put("/api/copy-relations/{relationId}", relationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.copyMode").value("BALANCE_RATIO"))
                .andExpect(jsonPath("$.priority").value(5))
                .andExpect(jsonPath("$.status").value("PAUSED"))
                .andExpect(jsonPath("$.configVersion").value(2));
    }

    @Test
    void shouldSaveAndListFollowerSymbolMappings() throws Exception {
        Long followerAccountId = bindAccount(4004L, "FOLLOWER");

        Map<String, Object> createPayload = new HashMap<>();
        createPayload.put("followerAccountId", followerAccountId);
        createPayload.put("masterSymbol", "xauusd");
        createPayload.put("followerSymbol", "XAUUSDm");

        MvcResult createResult = mockMvc.perform(post("/api/symbol-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerAccountId").value(followerAccountId))
                .andExpect(jsonPath("$.masterSymbol").value("XAUUSD"))
                .andExpect(jsonPath("$.followerSymbol").value("XAUUSDm"))
                .andReturn();

        long mappingId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("id").asLong();

        Map<String, Object> updatePayload = new HashMap<>();
        updatePayload.put("followerAccountId", followerAccountId);
        updatePayload.put("masterSymbol", "XAUUSD");
        updatePayload.put("followerSymbol", "XAUUSD.pro");

        mockMvc.perform(post("/api/symbol-mappings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mappingId))
                .andExpect(jsonPath("$.masterSymbol").value("XAUUSD"))
                .andExpect(jsonPath("$.followerSymbol").value("XAUUSD.pro"));

        mockMvc.perform(get("/api/symbol-mappings/followers/{followerAccountId}", followerAccountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(mappingId))
                .andExpect(jsonPath("$[0].masterSymbol").value("XAUUSD"))
                .andExpect(jsonPath("$[0].followerSymbol").value("XAUUSD.pro"));
    }

    private Long bindAccount(Long userId, String accountRole) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("brokerName", "Broker-" + userId);
        payload.put("serverName", "Server-" + userId + "-" + accountRole);
        payload.put("mt5Login", System.nanoTime());
        payload.put("credential", "secret");
        payload.put("accountRole", accountRole);

        MvcResult result = mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }
}
