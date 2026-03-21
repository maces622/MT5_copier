package com.zyc.copier_v0.modules.monitor.entity;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(
        name = "mt5_signal_records",
        indexes = {
                @Index(name = "idx_signal_record_account_key", columnList = "account_key"),
                @Index(name = "idx_signal_record_event_id", columnList = "event_id"),
                @Index(name = "idx_signal_record_received_at", columnList = "received_at")
        }
)
public class Mt5SignalRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 128)
    private String eventId;

    @Column(name = "signal_type", nullable = false, length = 32)
    private String signalType;

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(name = "trace_id", nullable = false, length = 128)
    private String traceId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "login_no")
    private Long login;

    @Column(name = "server_name", length = 128)
    private String server;

    @Column(name = "account_key", length = 255)
    private String accountKey;

    @Column(name = "source_timestamp", length = 64)
    private String sourceTimestamp;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @PrePersist
    void prePersist() {
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getSignalType() {
        return signalType;
    }

    public void setSignalType(String signalType) {
        this.signalType = signalType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getLogin() {
        return login;
    }

    public void setLogin(Long login) {
        this.login = login;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getAccountKey() {
        return accountKey;
    }

    public void setAccountKey(String accountKey) {
        this.accountKey = accountKey;
    }

    public String getSourceTimestamp() {
        return sourceTimestamp;
    }

    public void setSourceTimestamp(String sourceTimestamp) {
        this.sourceTimestamp = sourceTimestamp;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}
