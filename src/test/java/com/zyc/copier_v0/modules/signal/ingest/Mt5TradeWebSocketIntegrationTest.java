package com.zyc.copier_v0.modules.signal.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import com.zyc.copier_v0.modules.monitor.api.Mt5WsSessionResponse;
import com.zyc.copier_v0.modules.signal.ingest.domain.Mt5SignalType;
import com.zyc.copier_v0.modules.signal.ingest.domain.NormalizedMt5Signal;
import com.zyc.copier_v0.modules.signal.ingest.service.Mt5SignalPublisher;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:mt5trade;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "copier.account-config.route-cache.backend=log",
                "copier.mt5.signal-ingest.bearer-token=test-token",
                "copier.mt5.signal-ingest.dedup-ttl=PT10M",
                "copier.mt5.signal-ingest.dedup-backend=memory",
                "copier.monitor.runtime-state.backend=database",
                "copier.monitor.session-registry.backend=memory",
                "copier.mt5.follower-exec.realtime-dispatch.backend=local"
        }
)
@ActiveProfiles("test")
class Mt5TradeWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CapturingMt5SignalPublisher capturingPublisher;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private final StandardWebSocketClient client = new StandardWebSocketClient();

    @AfterEach
    void tearDown() {
        capturingPublisher.clear();
    }

    @Test
    void shouldAcceptHelloAndDeduplicateDealSignals() throws Exception {
        WebSocketSession session = connect();
        try {
            session.sendMessage(new TextMessage(
                    "{\"type\":\"HELLO\",\"login\":123456,\"server\":\"Broker-Demo\",\"ts\":\"2026.03.20 15:10:00\"}"
            ));
            session.sendMessage(new TextMessage(
                    "{\"type\":\"DEAL\",\"event_id\":\"123456-DEAL-1\",\"login\":123456,\"server\":\"Broker-Demo\",\"deal\":1,\"order\":2,\"position\":3,\"symbol\":\"XAUUSD\",\"action\":\"BUY OPEN\",\"volume\":1.0,\"price\":3025.12,\"deal_type\":0,\"entry\":0,\"magic\":0,\"comment\":\"\",\"time\":\"2026.03.20 15:10:01\"}"
            ));
            session.sendMessage(new TextMessage(
                    "{\"type\":\"DEAL\",\"event_id\":\"123456-DEAL-1\",\"login\":123456,\"server\":\"Broker-Demo\",\"deal\":1,\"order\":2,\"position\":3,\"symbol\":\"XAUUSD\",\"action\":\"BUY OPEN\",\"volume\":1.0,\"price\":3025.12,\"deal_type\":0,\"entry\":0,\"magic\":0,\"comment\":\"\",\"time\":\"2026.03.20 15:10:01\"}"
            ));

            NormalizedMt5Signal hello = capturingPublisher.await(Duration.ofSeconds(5));
            NormalizedMt5Signal deal = capturingPublisher.await(Duration.ofSeconds(5));

            assertThat(hello).isNotNull();
            assertThat(hello.getType()).isEqualTo(Mt5SignalType.HELLO);
            assertThat(hello.getMasterAccountKey()).isEqualTo("Broker-Demo:123456");

            assertThat(deal).isNotNull();
            assertThat(deal.getType()).isEqualTo(Mt5SignalType.DEAL);
            assertThat(deal.getEventId()).isEqualTo("123456-DEAL-1");
            assertThat(deal.getMasterAccountKey()).isEqualTo("Broker-Demo:123456");

            assertThat(capturingPublisher.poll(Duration.ofMillis(500))).isNull();
        } finally {
            session.close();
        }
    }

    @Test
    void shouldExposeActiveWsSessionAfterHeartbeat() throws Exception {
        WebSocketSession session = connect();
        try {
            session.sendMessage(new TextMessage(
                    "{\"type\":\"HELLO\",\"login\":223456,\"server\":\"Broker-Live\",\"ts\":\"2026.03.20 15:12:00\"}"
            ));
            session.sendMessage(new TextMessage(
                    "{\"type\":\"HEARTBEAT\",\"login\":223456,\"server\":\"Broker-Live\",\"ts\":\"2026.03.20 15:12:01\"}"
            ));

            NormalizedMt5Signal hello = capturingPublisher.await(Duration.ofSeconds(5));
            NormalizedMt5Signal heartbeat = capturingPublisher.await(Duration.ofSeconds(5));

            assertThat(hello).isNotNull();
            assertThat(heartbeat).isNotNull();

            Mt5WsSessionResponse[] sessions = testRestTemplate.getForObject(
                    "http://localhost:" + port + "/api/monitor/ws-sessions",
                    Mt5WsSessionResponse[].class
            );

            assertThat(sessions).isNotNull();
            assertThat(sessions).hasSize(1);
            assertThat(sessions[0].getLogin()).isEqualTo(223456L);
            assertThat(sessions[0].getServer()).isEqualTo("Broker-Live");
            assertThat(sessions[0].getAccountKey()).isEqualTo("Broker-Live:223456");
            assertThat(sessions[0].getConnectionStatus()).isNotNull();
            assertThat(sessions[0].getLastHelloAt()).isNotNull();
            assertThat(sessions[0].getLastHeartbeatAt()).isNotNull();
            assertThat(sessions[0].getLastSignalType()).isEqualTo("HEARTBEAT");
        } finally {
            session.close();
        }
    }

    private WebSocketSession connect() throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        return client.doHandshake(
                new TextWebSocketHandler(),
                headers,
                URI.create("ws://localhost:" + port + "/ws/trade")
        ).get(5, TimeUnit.SECONDS);
    }

    @TestConfiguration
    static class TestPublisherConfiguration {

        @Bean
        @Primary
        CapturingMt5SignalPublisher capturingMt5SignalPublisher() {
            return new CapturingMt5SignalPublisher();
        }
    }

    static class CapturingMt5SignalPublisher implements Mt5SignalPublisher {

        private final BlockingQueue<NormalizedMt5Signal> signals = new LinkedBlockingQueue<>();

        @Override
        public void publish(NormalizedMt5Signal signal) {
            signals.offer(signal);
        }

        NormalizedMt5Signal await(Duration timeout) throws InterruptedException {
            return signals.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        NormalizedMt5Signal poll(Duration timeout) throws InterruptedException {
            return signals.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }

        void clear() {
            signals.clear();
        }
    }
}
