package com.zyc.copier_v0.modules.copy.followerexec;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.account.config.api.BindMt5AccountRequest;
import com.zyc.copier_v0.modules.account.config.api.CreateCopyRelationRequest;
import com.zyc.copier_v0.modules.account.config.api.Mt5AccountResponse;
import com.zyc.copier_v0.modules.account.config.api.SaveRiskRuleRequest;
import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import com.zyc.copier_v0.modules.account.config.service.AccountConfigService;
import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import com.zyc.copier_v0.modules.copy.engine.entity.FollowerDispatchOutboxEntity;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import com.zyc.copier_v0.modules.copy.followerexec.api.FollowerExecSessionResponse;
import com.zyc.copier_v0.modules.monitor.repository.Mt5AccountRuntimeStateRepository;
import com.zyc.copier_v0.modules.signal.ingest.service.Mt5SignalIngestService;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:followerexec;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "copier.account-config.route-cache.backend=log",
                "copier.mt5.signal-ingest.bearer-token=test-signal-token",
                "copier.mt5.signal-ingest.dedup-backend=memory",
                "copier.mt5.follower-exec.bearer-token=test-follower-token",
                "copier.monitor.session-registry.backend=memory",
                "copier.mt5.follower-exec.realtime-dispatch.backend=local",
                "copier.mt5.follower-exec.heartbeat-stale-after=PT15S"
        }
)
@ActiveProfiles("test")
class FollowerExecWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AccountConfigService accountConfigService;

    @Autowired
    private Mt5SignalIngestService mt5SignalIngestService;

    @Autowired
    private FollowerDispatchOutboxRepository followerDispatchOutboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private Mt5AccountRuntimeStateRepository runtimeStateRepository;

    private final StandardWebSocketClient client = new StandardWebSocketClient();

    @Test
    void shouldReplayPendingDispatchAfterFollowerHelloAndAckIt() throws Exception {
        long masterLogin = 981001L;
        long followerLogin = 981002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9801L, "Master", server, masterLogin, Mt5AccountRole.MASTER);
        Long followerAccountId = bindAccount(9801L, "Follower", server, followerLogin, Mt5AccountRole.FOLLOWER);
        saveRiskRule(followerAccountId, new BigDecimal("0.10"));
        createRelation(masterAccountId, followerAccountId);

        createMasterDealSignal("follower-backlog-session", "trace-backlog", masterLogin, server, "981001-DEAL-10001", 10001L);

        FollowerDispatchOutboxEntity pendingDispatch = followerDispatchOutboxRepository
                .findByFollowerAccountIdAndStatusOrderByIdAsc(followerAccountId, FollowerDispatchStatus.PENDING)
                .get(0);

        CapturingTextWebSocketHandler handler = new CapturingTextWebSocketHandler();
        WebSocketSession session = connect(handler);
        try {
            session.sendMessage(new TextMessage(
                    "{\"type\":\"HELLO\",\"followerAccountId\":" + followerAccountId
                            + ",\"balance\":5000.0,\"equity\":5000.0}"
            ));

            List<JsonNode> initialMessages = awaitMessages(handler, 2, Duration.ofSeconds(5));
            JsonNode helloAck = findByType(initialMessages, "HELLO_ACK");
            JsonNode dispatch = findByType(initialMessages, "DISPATCH");

            assertThat(helloAck).isNotNull();
            assertThat(helloAck.path("followerAccountId").asLong()).isEqualTo(followerAccountId);
            assertThat(helloAck.path("pendingDispatchCount").asInt()).isEqualTo(1);

            assertThat(dispatch).isNotNull();
            assertThat(dispatch.path("dispatchId").asLong()).isEqualTo(pendingDispatch.getId());
            assertThat(dispatch.path("payload").path("commandType").asText()).isEqualTo("OPEN_POSITION");
            assertThat(dispatch.path("payload").path("symbol").asText()).isEqualTo("XAUUSD");
            assertThat(dispatch.path("payload").path("slippagePolicy").path("mode").asText()).isEqualTo("PIPS");
            assertThat(dispatch.path("payload").path("slippagePolicy").path("maxPips").decimalValue()).isEqualByComparingTo("10");
            assertThat(dispatch.path("payload").path("instrumentMeta").path("contractSize").decimalValue()).isEqualByComparingTo("100");

            FollowerExecSessionResponse[] sessions = testRestTemplate.getForObject(
                    "http://localhost:" + port + "/api/follower-exec/sessions",
                    FollowerExecSessionResponse[].class
            );
            assertThat(sessions).isNotNull();
            assertThat(sessions).hasSize(1);
            assertThat(sessions[0].getFollowerAccountId()).isEqualTo(followerAccountId);
            assertThat(sessions[0].getLastDispatchId()).isEqualTo(pendingDispatch.getId());
            assertThat(runtimeStateRepository.findByAccountId(followerAccountId)).isPresent();
            assertThat(runtimeStateRepository.findByAccountId(followerAccountId).orElseThrow().getBalance())
                    .isEqualByComparingTo("5000");
            assertThat(runtimeStateRepository.findByAccountId(followerAccountId).orElseThrow().getEquity())
                    .isEqualByComparingTo("5000");

            session.sendMessage(new TextMessage(
                    "{\"type\":\"ACK\",\"dispatchId\":" + pendingDispatch.getId() + ",\"statusMessage\":\"submitted\"}"
            ));

            JsonNode statusAck = objectMapper.readTree(handler.await(Duration.ofSeconds(5)));
            assertThat(statusAck.path("type").asText()).isEqualTo("STATUS_ACK");
            assertThat(statusAck.path("dispatchId").asLong()).isEqualTo(pendingDispatch.getId());
            assertThat(statusAck.path("status").asText()).isEqualTo("ACKED");

            FollowerDispatchOutboxEntity ackedDispatch = followerDispatchOutboxRepository.findById(pendingDispatch.getId()).orElseThrow();
            assertThat(ackedDispatch.getStatus()).isEqualTo(FollowerDispatchStatus.ACKED);
            assertThat(ackedDispatch.getAckedAt()).isNotNull();
        } finally {
            session.close();
        }
    }

    @Test
    void shouldPushDispatchCreatedAfterFollowerSessionIsBound() throws Exception {
        long masterLogin = 982001L;
        long followerLogin = 982002L;
        String server = "Broker-Live";

        Long masterAccountId = bindAccount(9802L, "Master2", server, masterLogin, Mt5AccountRole.MASTER);
        Long followerAccountId = bindAccount(9802L, "Follower2", server, followerLogin, Mt5AccountRole.FOLLOWER);
        saveRiskRule(followerAccountId, new BigDecimal("0.20"));
        createRelation(masterAccountId, followerAccountId);

        CapturingTextWebSocketHandler handler = new CapturingTextWebSocketHandler();
        WebSocketSession session = connect(handler);
        try {
            session.sendMessage(new TextMessage(
                    "{\"type\":\"HELLO\",\"login\":" + followerLogin + ",\"server\":\"" + server + "\"}"
            ));

            JsonNode helloAck = objectMapper.readTree(handler.await(Duration.ofSeconds(5)));
            assertThat(helloAck.path("type").asText()).isEqualTo("HELLO_ACK");
            assertThat(helloAck.path("pendingDispatchCount").asInt()).isEqualTo(0);

            createMasterDealSignal("follower-live-session", "trace-live", masterLogin, server, "982001-DEAL-20001", 20001L);

            JsonNode dispatch = objectMapper.readTree(handler.await(Duration.ofSeconds(5)));
            assertThat(dispatch.path("type").asText()).isEqualTo("DISPATCH");
            assertThat(dispatch.path("payload").path("commandType").asText()).isEqualTo("OPEN_POSITION");
            assertThat(dispatch.path("payload").path("volume").decimalValue()).isEqualByComparingTo("0.2000");
            assertThat(dispatch.path("payload").path("slippagePolicy").path("mode").asText()).isEqualTo("PIPS");
            assertThat(dispatch.path("payload").path("instrumentMeta").path("currencyBase").asText()).isEqualTo("XAU");

            long dispatchId = dispatch.path("dispatchId").asLong();
            session.sendMessage(new TextMessage(
                    "{\"type\":\"FAIL\",\"dispatchId\":" + dispatchId + ",\"statusMessage\":\"mock execute failed\"}"
            ));

            JsonNode statusAck = objectMapper.readTree(handler.await(Duration.ofSeconds(5)));
            assertThat(statusAck.path("status").asText()).isEqualTo("FAILED");

            FollowerDispatchOutboxEntity failedDispatch = followerDispatchOutboxRepository.findById(dispatchId).orElseThrow();
            assertThat(failedDispatch.getStatus()).isEqualTo(FollowerDispatchStatus.FAILED);
            assertThat(failedDispatch.getFailedAt()).isNotNull();
        } finally {
            session.close();
        }
    }

    private WebSocketSession connect(CapturingTextWebSocketHandler handler) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer test-follower-token");
        return client.doHandshake(
                handler,
                headers,
                URI.create("ws://localhost:" + port + "/ws/follower-exec")
        ).get(5, TimeUnit.SECONDS);
    }

    private Long bindAccount(
            Long userId,
            String brokerName,
            String server,
            long login,
            Mt5AccountRole role
    ) {
        BindMt5AccountRequest request = new BindMt5AccountRequest();
        request.setUserId(userId);
        request.setBrokerName(brokerName);
        request.setServerName(server);
        request.setMt5Login(login);
        request.setCredential("secret");
        request.setAccountRole(role);
        Mt5AccountResponse response = accountConfigService.bindAccount(request);
        return response.getId();
    }

    private void saveRiskRule(Long accountId, BigDecimal fixedLot) {
        SaveRiskRuleRequest request = new SaveRiskRuleRequest();
        request.setAccountId(accountId);
        request.setFixedLot(fixedLot);
        request.setMaxLot(new BigDecimal("5.00"));
        request.setFollowTpSl(true);
        accountConfigService.saveRiskRule(request);
    }

    private void createRelation(Long masterAccountId, Long followerAccountId) {
        CreateCopyRelationRequest request = new CreateCopyRelationRequest();
        request.setMasterAccountId(masterAccountId);
        request.setFollowerAccountId(followerAccountId);
        request.setCopyMode(CopyMode.FIXED_LOT);
        accountConfigService.createCopyRelation(request);
    }

    private void createMasterDealSignal(
            String sessionId,
            String traceId,
            long masterLogin,
            String server,
            String eventId,
            long dealId
    ) throws Exception {
        mt5SignalIngestService.registerConnection(sessionId, traceId);
        mt5SignalIngestService.ingest(sessionId, traceId,
                "{\"type\":\"DEAL\",\"event_id\":\"" + eventId + "\",\"login\":" + masterLogin
                        + ",\"server\":\"" + server + "\",\"deal\":" + dealId
                        + ",\"order\":20002,\"position\":20003,\"symbol\":\"XAUUSD\",\"action\":\"BUY OPEN\","
                        + "\"volume\":1.0,\"price\":3025.12,\"deal_type\":0,\"entry\":0,\"magic\":0,"
                        + "\"comment\":\"\",\"time\":\"2026.03.20 18:00:01\","
                        + "\"symbol_digits\":2,\"symbol_point\":0.01,\"symbol_tick_size\":0.01,"
                        + "\"symbol_tick_value\":1.0,\"symbol_contract_size\":100.0,"
                        + "\"symbol_volume_step\":0.01,\"symbol_volume_min\":0.01,\"symbol_volume_max\":100.0,"
                        + "\"symbol_currency_base\":\"XAU\",\"symbol_currency_profit\":\"USD\",\"symbol_currency_margin\":\"USD\"}");
    }

    private List<JsonNode> awaitMessages(
            CapturingTextWebSocketHandler handler,
            int expectedCount,
            Duration timeout
    ) throws Exception {
        List<JsonNode> messages = new ArrayList<>();
        for (int i = 0; i < expectedCount; i++) {
            messages.add(objectMapper.readTree(handler.await(timeout)));
        }
        return messages;
    }

    private JsonNode findByType(List<JsonNode> messages, String type) {
        return messages.stream()
                .filter(message -> type.equals(message.path("type").asText()))
                .findFirst()
                .orElse(null);
    }

    static class CapturingTextWebSocketHandler extends TextWebSocketHandler {

        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.offer(message.getPayload());
        }

        String await(Duration timeout) throws InterruptedException {
            return messages.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
