package com.zyc.copier_v0.modules.monitor.entity;

import com.zyc.copier_v0.support.ManualIdGenerator;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "mt5_signal_records",
        indexes = {
                @Index(name = "idx_signal_record_account_key", columnList = "account_key"),
                @Index(name = "idx_signal_record_event_id", columnList = "event_id"),
                @Index(name = "idx_signal_record_received_at", columnList = "received_at")
        }
)
@Getter
@Setter
public class Mt5SignalRecordEntity {

    @Id
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
        if (id == null) {
            id = ManualIdGenerator.nextId();
        }
        if (receivedAt == null) {
            receivedAt = Instant.now();
        }
    }
}
