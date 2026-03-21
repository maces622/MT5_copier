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

@Entity
@Table(name = "risk_rules", uniqueConstraints = @UniqueConstraint(name = "uk_risk_rule_account", columnNames = {"account_id"}))
public class RiskRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "row_version", nullable = false)
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

    public Long getId() {
        return id;
    }

    public Mt5AccountEntity getAccount() {
        return account;
    }

    public void setAccount(Mt5AccountEntity account) {
        this.account = account;
    }

    public BigDecimal getMaxLot() {
        return maxLot;
    }

    public void setMaxLot(BigDecimal maxLot) {
        this.maxLot = maxLot;
    }

    public BigDecimal getFixedLot() {
        return fixedLot;
    }

    public void setFixedLot(BigDecimal fixedLot) {
        this.fixedLot = fixedLot;
    }

    public BigDecimal getBalanceRatio() {
        return balanceRatio;
    }

    public void setBalanceRatio(BigDecimal balanceRatio) {
        this.balanceRatio = balanceRatio;
    }

    public Integer getMaxSlippagePoints() {
        return maxSlippagePoints;
    }

    public void setMaxSlippagePoints(Integer maxSlippagePoints) {
        this.maxSlippagePoints = maxSlippagePoints;
    }

    public BigDecimal getMaxSlippagePips() {
        return maxSlippagePips;
    }

    public void setMaxSlippagePips(BigDecimal maxSlippagePips) {
        this.maxSlippagePips = maxSlippagePips;
    }

    public BigDecimal getMaxSlippagePrice() {
        return maxSlippagePrice;
    }

    public void setMaxSlippagePrice(BigDecimal maxSlippagePrice) {
        this.maxSlippagePrice = maxSlippagePrice;
    }

    public BigDecimal getMaxDailyLoss() {
        return maxDailyLoss;
    }

    public void setMaxDailyLoss(BigDecimal maxDailyLoss) {
        this.maxDailyLoss = maxDailyLoss;
    }

    public BigDecimal getMaxDrawdownPct() {
        return maxDrawdownPct;
    }

    public void setMaxDrawdownPct(BigDecimal maxDrawdownPct) {
        this.maxDrawdownPct = maxDrawdownPct;
    }

    public String getAllowedSymbols() {
        return allowedSymbols;
    }

    public void setAllowedSymbols(String allowedSymbols) {
        this.allowedSymbols = allowedSymbols;
    }

    public String getBlockedSymbols() {
        return blockedSymbols;
    }

    public void setBlockedSymbols(String blockedSymbols) {
        this.blockedSymbols = blockedSymbols;
    }

    public boolean isFollowTpSl() {
        return followTpSl;
    }

    public void setFollowTpSl(boolean followTpSl) {
        this.followTpSl = followTpSl;
    }

    public boolean isReverseFollow() {
        return reverseFollow;
    }

    public void setReverseFollow(boolean reverseFollow) {
        this.reverseFollow = reverseFollow;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
