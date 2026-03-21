package com.zyc.copier_v0.modules.account.config.entity;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "copy_relations",
        indexes = {
                @Index(name = "idx_copy_relation_master_status_priority", columnList = "master_account_id,status,priority"),
                @Index(name = "idx_copy_relation_follower_status", columnList = "follower_account_id,status")
        },
        uniqueConstraints = @UniqueConstraint(
                name = "uk_copy_relation_master_follower",
                columnNames = {"master_account_id", "follower_account_id"}
        )
)
@Getter
@Setter
public class CopyRelationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "master_account_id", nullable = false)
    private Mt5AccountEntity masterAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_account_id", nullable = false)
    private Mt5AccountEntity followerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "copy_mode", nullable = false, length = 32)
    private CopyMode copyMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CopyRelationStatus status;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "config_version", nullable = false)
    private Long configVersion;

    @Column(name = "created_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long rowVersion;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
