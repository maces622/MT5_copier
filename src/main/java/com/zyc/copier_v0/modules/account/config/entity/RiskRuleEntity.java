package com.zyc.copier_v0.modules.account.config.entity;

import java.math.BigDecimal;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "risk_rules", uniqueConstraints = @UniqueConstraint(name = "uk_risk_rule_account", columnNames = {"account_id"}))
@Getter
@Setter
public class RiskRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Mt5AccountEntity account;

    @Column(name = "max_lot", precision = 18, scale = 6)
    private BigDecimal maxLot;

    @Column(name = "fixed_lot", precision = 18, scale = 6)
    private BigDecimal fixedLot;

    @Column(name = "balance_ratio", precision = 18, scale = 6)
    private BigDecimal balanceRatio;

    @Column(name = "max_slippage_points")
    private Integer maxSlippagePoints;

    @Column(name = "max_slippage_pips", precision = 18, scale = 6)
    private BigDecimal maxSlippagePips;

    @Column(name = "max_slippage_price", precision = 18, scale = 6)
    private BigDecimal maxSlippagePrice;

    @Column(name = "max_daily_loss", precision = 18, scale = 6)
    private BigDecimal maxDailyLoss;

    @Column(name = "max_drawdown_pct", precision = 18, scale = 6)
    private BigDecimal maxDrawdownPct;

    @Column(name = "allowed_symbols", length = 512)
    private String allowedSymbols;

    @Column(name = "blocked_symbols", length = 512)
    private String blockedSymbols;

    @Column(name = "follow_tp_sl", nullable = false)
    private boolean followTpSl;

    @Column(name = "reverse_follow", nullable = false)
    private boolean reverseFollow;

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
