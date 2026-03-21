package com.zyc.copier_v0.modules.account.config.api;

import java.math.BigDecimal;
import javax.validation.constraints.NotNull;

public class SaveRiskRuleRequest {

    @NotNull
    private Long accountId;

    private BigDecimal maxLot;
    private BigDecimal fixedLot;
    private BigDecimal balanceRatio;
    private Integer maxSlippagePoints;
    private BigDecimal maxSlippagePips;
    private BigDecimal maxSlippagePrice;
    private BigDecimal maxDailyLoss;
    private BigDecimal maxDrawdownPct;
    private String allowedSymbols;
    private String blockedSymbols;
    private Boolean followTpSl;
    private Boolean reverseFollow;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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

    public Boolean getFollowTpSl() {
        return followTpSl;
    }

    public void setFollowTpSl(Boolean followTpSl) {
        this.followTpSl = followTpSl;
    }

    public Boolean getReverseFollow() {
        return reverseFollow;
    }

    public void setReverseFollow(Boolean reverseFollow) {
        this.reverseFollow = reverseFollow;
    }
}
