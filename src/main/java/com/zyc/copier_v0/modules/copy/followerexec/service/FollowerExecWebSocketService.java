package com.zyc.copier_v0.modules.copy.followerexec.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import com.zyc.copier_v0.modules.account.config.entity.Mt5AccountEntity;
import com.zyc.copier_v0.modules.account.config.repository.Mt5AccountRepository;
import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import com.zyc.copier_v0.modules.copy.engine.entity.FollowerDispatchOutboxEntity;
import com.zyc.copier_v0.modules.copy.engine.event.FollowerDispatchCreatedEvent;
import com.zyc.copier_v0.modules.copy.engine.repository.FollowerDispatchOutboxRepository;
import com.zyc.copier_v0.modules.copy.followerexec.api.FollowerExecSessionResponse;
import com.zyc.copier_v0.modules.copy.followerexec.config.FollowerExecWebSocketProperties;
import com.zyc.copier_v0.modules.copy.followerexec.domain.FollowerExecMessageType;
import com.zyc.copier_v0.modules.copy.followerexec.domain.FollowerExecSessionContext;
import com.zyc.copier_v0.modules.monitor.domain.Mt5ConnectionStatus;
import com.zyc.copier_v0.modules.monitor.entity.Mt5AccountRuntimeStateEntity;
import com.zyc.copier_v0.modules.monitor.repository.Mt5AccountRuntimeStateRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class FollowerExecWebSocketService {

    private static final Logger log = LoggerFactory.getLogger(FollowerExecWebSocketService.class);

    private final ObjectMapper objectMapper;
    private final Mt5AccountRepository mt5AccountRepository;
    private final FollowerDispatchOutboxRepository followerDispatchOutboxRepository;
    private final FollowerExecSessionRegistry sessionRegistry;
    private final FollowerExecWebSocketProperties properties;
    private final Mt5AccountRuntimeStateRepository runtimeStateRepository;
    private final ConcurrentMap<String, WebSocketSession> liveSessions = new ConcurrentHashMap<>();

    public FollowerExecWebSocketService(
            ObjectMapper objectMapper,
            Mt5AccountRepository mt5AccountRepository,
            FollowerDispatchOutboxRepository followerDispatchOutboxRepository,
            FollowerExecSessionRegistry sessionRegistry,
            FollowerExecWebSocketProperties properties,
            Mt5AccountRuntimeStateRepository runtimeStateRepository
    ) {
        this.objectMapper = objectMapper;
        this.mt5AccountRepository = mt5AccountRepository;
        this.followerDispatchOutboxRepository = followerDispatchOutboxRepository;
        this.sessionRegistry = sessionRegistry;
        this.properties = properties;
        this.runtimeStateRepository = runtimeStateRepository;
    }

    public void registerConnection(String sessionId, String traceId, WebSocketSession session) {
        liveSessions.put(sessionId, session);
        sessionRegistry.register(sessionId, traceId);
        log.info("Follower-exec websocket connected, sessionId={}, traceId={}", sessionId, traceId);
    }

    public void unregisterConnection(String sessionId) {
        liveSessions.remove(sessionId);
        sessionRegistry.remove(sessionId).ifPresent(context -> {
            markFollowerDisconnected(context);
            log.info("Follower-exec websocket disconnected, sessionId={}, traceId={}, followerAccountId={}",
                    context.getSessionId(), context.getTraceId(), context.getFollowerAccountId());
        });
    }

    @Transactional
    public void handleMessage(String sessionId, String traceId, String textPayload) throws JsonProcessingException {
        JsonNode payload = objectMapper.readTree(textPayload);
        FollowerExecMessageType type = FollowerExecMessageType.fromCode(payload.path("type").asText(null));
        switch (type) {
            case HELLO:
                handleHello(sessionId, traceId, payload);
                return;
            case HEARTBEAT:
                handleHeartbeat(sessionId, payload);
                return;
            case ACK:
                handleDispatchStatusUpdate(sessionId, payload, FollowerDispatchStatus.ACKED);
                return;
            case FAIL:
                handleDispatchStatusUpdate(sessionId, payload, FollowerDispatchStatus.FAILED);
                return;
            default:
                throw new IllegalArgumentException("Unsupported inbound follower-exec message type: " + type);
        }
    }

    @Transactional(readOnly = true)
    public List<FollowerExecSessionResponse> listSessions() {
        return sessionRegistry.listAll().stream()
                .map(this::toSessionResponse)
                .toList();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFollowerDispatchCreated(FollowerDispatchCreatedEvent event) {
        pushDispatchIfFollowerOnline(event.getFollowerAccountId(), event.getDispatchId());
    }

    private void handleHello(String sessionId, String traceId, JsonNode payload) {
        Mt5AccountEntity followerAccount = resolveFollowerAccount(payload);
        validateFollowerAccount(followerAccount);

        Instant helloAt = Instant.now();
        sessionRegistry.bindFollower(
                sessionId,
                followerAccount.getId(),
                followerAccount.getMt5Login(),
                followerAccount.getServerName(),
                helloAt
        );
        upsertFollowerRuntimeState(
                followerAccount,
                sessionId,
                helloAt,
                readDecimal(payload, "balance"),
                readDecimal(payload, "equity"),
                false
        );

        List<FollowerDispatchOutboxEntity> pendingDispatches = followerDispatchOutboxRepository
                .findByFollowerAccountIdAndStatusOrderByIdAsc(followerAccount.getId(), FollowerDispatchStatus.PENDING);
        sendHelloAck(sessionId, traceId, followerAccount, pendingDispatches.size());
        for (FollowerDispatchOutboxEntity dispatch : pendingDispatches) {
            pushDispatchIfFollowerOnline(followerAccount.getId(), dispatch.getId());
        }
    }

    private void handleHeartbeat(String sessionId, JsonNode payload) {
        Instant heartbeatAt = Instant.now();
        sessionRegistry.touchHeartbeat(sessionId, heartbeatAt);

        sessionRegistry.get(sessionId)
                .filter(context -> context.getFollowerAccountId() != null)
                .flatMap(context -> mt5AccountRepository.findById(context.getFollowerAccountId()))
                .ifPresent(followerAccount -> upsertFollowerRuntimeState(
                        followerAccount,
                        sessionId,
                        heartbeatAt,
                        readDecimal(payload, "balance"),
                        readDecimal(payload, "equity"),
                        true
                ));
    }

    private void handleDispatchStatusUpdate(
            String sessionId,
            JsonNode payload,
            FollowerDispatchStatus status
    ) {
        FollowerExecSessionContext sessionContext = sessionRegistry.get(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Follower session is not registered"));
        if (sessionContext.getFollowerAccountId() == null) {
            throw new IllegalArgumentException("Follower session is not bound, HELLO is required first");
        }

        Long dispatchId = readLong(payload, "dispatchId", "dispatch_id");
        if (dispatchId == null) {
            throw new IllegalArgumentException("dispatchId is required");
        }

        FollowerDispatchOutboxEntity dispatch = followerDispatchOutboxRepository.findById(dispatchId)
                .orElseThrow(() -> new IllegalArgumentException("Follower dispatch not found: " + dispatchId));
        if (!sessionContext.getFollowerAccountId().equals(dispatch.getFollowerAccountId())) {
            throw new IllegalArgumentException("Dispatch does not belong to the bound follower account");
        }

        applyDispatchStatus(dispatch, status, readText(payload, "statusMessage", "status_message"));
        FollowerDispatchOutboxEntity saved = followerDispatchOutboxRepository.save(dispatch);
        sendStatusAckAfterCommit(sessionId, saved);
    }

    private void applyDispatchStatus(
            FollowerDispatchOutboxEntity dispatch,
            FollowerDispatchStatus status,
            String statusMessage
    ) {
        dispatch.setStatus(status);
        dispatch.setStatusMessage(trimToNull(statusMessage));
        Instant now = Instant.now();
        if (status == FollowerDispatchStatus.ACKED) {
            dispatch.setAckedAt(now);
            dispatch.setFailedAt(null);
        } else if (status == FollowerDispatchStatus.FAILED) {
            dispatch.setFailedAt(now);
            dispatch.setAckedAt(null);
        } else {
            dispatch.setAckedAt(null);
            dispatch.setFailedAt(null);
        }
    }

    private Mt5AccountEntity resolveFollowerAccount(JsonNode payload) {
        Long followerAccountId = readLong(payload, "followerAccountId", "follower_account_id");
        if (followerAccountId != null) {
            return mt5AccountRepository.findById(followerAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Follower account not found: " + followerAccountId));
        }

        Long login = readLong(payload, "login");
        String server = readText(payload, "server");
        if (login == null || !StringUtils.hasText(server)) {
            throw new IllegalArgumentException("HELLO must include followerAccountId or server + login");
        }

        return mt5AccountRepository.findByServerNameAndMt5Login(server, login)
                .orElseThrow(() -> new IllegalArgumentException("Follower account not found for " + server + ":" + login));
    }

    private void upsertFollowerRuntimeState(
            Mt5AccountEntity followerAccount,
            String sessionId,
            Instant activityAt,
            BigDecimal balance,
            BigDecimal equity,
            boolean heartbeatOnly
    ) {
        Mt5AccountRuntimeStateEntity state = runtimeStateRepository.findByAccountId(followerAccount.getId())
                .orElseGet(Mt5AccountRuntimeStateEntity::new);
        state.setAccountId(followerAccount.getId());
        state.setLogin(followerAccount.getMt5Login());
        state.setServer(followerAccount.getServerName());
        state.setAccountKey(followerAccount.getServerName() + ":" + followerAccount.getMt5Login());
        state.setLastSessionId(sessionId);
        state.setConnectionStatus(Mt5ConnectionStatus.CONNECTED);
        if (heartbeatOnly) {
            state.setLastHeartbeatAt(activityAt);
        } else {
            state.setLastHelloAt(activityAt);
        }
        if (balance != null) {
            state.setBalance(balance);
        }
        if (equity != null) {
            state.setEquity(equity);
        }
        runtimeStateRepository.save(state);
    }

    private void markFollowerDisconnected(FollowerExecSessionContext context) {
        if (context.getFollowerAccountId() == null) {
            return;
        }
        runtimeStateRepository.findByAccountId(context.getFollowerAccountId())
                .ifPresent(state -> {
                    state.setConnectionStatus(Mt5ConnectionStatus.DISCONNECTED);
                    state.setLastSessionId(context.getSessionId());
                    runtimeStateRepository.save(state);
                });
    }

    private void validateFollowerAccount(Mt5AccountEntity followerAccount) {
        if (followerAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Follower account is not active");
        }
        if (followerAccount.getAccountRole() != Mt5AccountRole.FOLLOWER
                && followerAccount.getAccountRole() != Mt5AccountRole.BOTH) {
            throw new IllegalArgumentException("Account is not allowed to act as follower");
        }
    }

    private void pushDispatchIfFollowerOnline(Long followerAccountId, Long dispatchId) {
        if (followerAccountId == null || dispatchId == null) {
            return;
        }

        Optional<String> sessionIdOptional = sessionRegistry.findSessionIdByFollowerAccountId(followerAccountId);
        if (!sessionIdOptional.isPresent()) {
            return;
        }

        WebSocketSession session = liveSessions.get(sessionIdOptional.get());
        if (session == null || !session.isOpen()) {
            return;
        }

        followerDispatchOutboxRepository.findById(dispatchId)
                .filter(dispatch -> dispatch.getStatus() == FollowerDispatchStatus.PENDING)
                .ifPresent(dispatch -> sendDispatch(sessionIdOptional.get(), session, dispatch));
    }

    private void sendHelloAck(
            String sessionId,
            String traceId,
            Mt5AccountEntity followerAccount,
            int pendingDispatchCount
    ) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", FollowerExecMessageType.HELLO_ACK.name());
        payload.put("traceId", traceId);
        payload.put("followerAccountId", followerAccount.getId());
        payload.put("login", followerAccount.getMt5Login());
        payload.put("server", followerAccount.getServerName());
        payload.put("accountKey", followerAccount.getServerName() + ":" + followerAccount.getMt5Login());
        payload.put("pendingDispatchCount", pendingDispatchCount);
        payload.put("serverTime", Instant.now().toString());
        sendJson(sessionId, payload);
    }

    private void sendStatusAck(String sessionId, FollowerDispatchOutboxEntity dispatch) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", FollowerExecMessageType.STATUS_ACK.name());
        payload.put("dispatchId", dispatch.getId());
        payload.put("status", dispatch.getStatus().name());
        putNullableString(payload, "statusMessage", dispatch.getStatusMessage());
        sendJson(sessionId, payload);
    }

    private void sendStatusAckAfterCommit(String sessionId, FollowerDispatchOutboxEntity dispatch) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            sendStatusAck(sessionId, dispatch);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendStatusAck(sessionId, dispatch);
            }
        });
    }

    private void sendDispatch(String sessionId, WebSocketSession session, FollowerDispatchOutboxEntity dispatch) {
        try {
            JsonNode dispatchPayload = objectMapper.readTree(dispatch.getPayloadJson());
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("type", FollowerExecMessageType.DISPATCH.name());
            payload.put("dispatchId", dispatch.getId());
            payload.put("executionCommandId", dispatch.getExecutionCommandId());
            payload.put("masterEventId", dispatch.getMasterEventId());
            payload.set("payload", dispatchPayload);
            sendJson(session, payload);
            sessionRegistry.recordDispatchSent(sessionId, dispatch.getId(), Instant.now());
            log.info("Pushed follower dispatch over websocket, sessionId={}, followerAccountId={}, dispatchId={}",
                    sessionId, dispatch.getFollowerAccountId(), dispatch.getId());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send follower dispatch payload", ex);
        }
    }

    private void sendJson(String sessionId, JsonNode payload) {
        WebSocketSession session = liveSessions.get(sessionId);
        if (session == null) {
            return;
        }
        sendJson(session, payload);
    }

    private void sendJson(WebSocketSession session, JsonNode payload) {
        try {
            if (!session.isOpen()) {
                return;
            }
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to send follower-exec websocket message", ex);
        }
    }

    private FollowerExecSessionResponse toSessionResponse(FollowerExecSessionContext context) {
        FollowerExecSessionResponse response = new FollowerExecSessionResponse();
        response.setSessionId(context.getSessionId());
        response.setTraceId(context.getTraceId());
        response.setConnectedAt(context.getConnectedAt());
        response.setFollowerAccountId(context.getFollowerAccountId());
        response.setLogin(context.getLogin());
        response.setServer(context.getServer());
        response.setAccountKey(context.accountKey());
        response.setConnectionStatus(resolveStatus(context));
        response.setLastHelloAt(context.getLastHelloAt());
        response.setLastHeartbeatAt(context.getLastHeartbeatAt());
        response.setLastDispatchSentAt(context.getLastDispatchSentAt());
        response.setLastDispatchId(context.getLastDispatchId());
        return response;
    }

    private Mt5ConnectionStatus resolveStatus(FollowerExecSessionContext context) {
        if (context.getFollowerAccountId() == null) {
            return Mt5ConnectionStatus.UNKNOWN;
        }
        Instant lastActivity = context.getLastHeartbeatAt() != null
                ? context.getLastHeartbeatAt()
                : context.getLastHelloAt();
        if (lastActivity == null) {
            return Mt5ConnectionStatus.CONNECTED;
        }
        if (Instant.now().isAfter(lastActivity.plus(properties.getHeartbeatStaleAfter()))) {
            return Mt5ConnectionStatus.STALE;
        }
        return Mt5ConnectionStatus.CONNECTED;
    }

    private Long readLong(JsonNode payload, String... fields) {
        for (String field : fields) {
            if (payload.hasNonNull(field)) {
                return payload.path(field).asLong();
            }
        }
        return null;
    }

    private String readText(JsonNode payload, String... fields) {
        for (String field : fields) {
            if (payload.hasNonNull(field) && StringUtils.hasText(payload.path(field).asText())) {
                return payload.path(field).asText();
            }
        }
        return null;
    }

    private BigDecimal readDecimal(JsonNode payload, String... fields) {
        for (String field : fields) {
            if (payload.hasNonNull(field)) {
                return payload.path(field).decimalValue();
            }
        }
        return null;
    }

    private void putNullableString(ObjectNode target, String field, String value) {
        if (StringUtils.hasText(value)) {
            target.put(field, value);
        } else {
            target.putNull(field);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
