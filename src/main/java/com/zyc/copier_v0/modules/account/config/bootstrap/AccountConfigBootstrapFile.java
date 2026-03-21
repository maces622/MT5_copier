package com.zyc.copier_v0.modules.account.config.bootstrap;

import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AccountConfigBootstrapFile {

    private List<AccountSpec> accounts = new ArrayList<>();
    private List<RiskRuleSpec> riskRules = new ArrayList<>();
    private List<CopyRelationSpec> copyRelations = new ArrayList<>();
    private List<SymbolMappingSpec> symbolMappings = new ArrayList<>();

    public List<AccountSpec> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountSpec> accounts) {
        this.accounts = accounts == null ? new ArrayList<>() : accounts;
    }

    public List<RiskRuleSpec> getRiskRules() {
        return riskRules;
    }

    public void setRiskRules(List<RiskRuleSpec> riskRules) {
        this.riskRules = riskRules == null ? new ArrayList<>() : riskRules;
    }

    public List<CopyRelationSpec> getCopyRelations() {
        return copyRelations;
    }

    public void setCopyRelations(List<CopyRelationSpec> copyRelations) {
        this.copyRelations = copyRelations == null ? new ArrayList<>() : copyRelations;
    }

    public List<SymbolMappingSpec> getSymbolMappings() {
        return symbolMappings;
    }

    public void setSymbolMappings(List<SymbolMappingSpec> symbolMappings) {
        this.symbolMappings = symbolMappings == null ? new ArrayList<>() : symbolMappings;
    }

    public static class AccountSpec {

        private String alias;
        private Long userId;
        private String brokerName;
        private String serverName;
        private Long mt5Login;
        private String credential;
        private Mt5AccountRole accountRole;
        private AccountStatus status = AccountStatus.ACTIVE;

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getBrokerName() {
            return brokerName;
        }

        public void setBrokerName(String brokerName) {
            this.brokerName = brokerName;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public Long getMt5Login() {
            return mt5Login;
        }

        public void setMt5Login(Long mt5Login) {
            this.mt5Login = mt5Login;
        }

        public String getCredential() {
            return credential;
        }

        public void setCredential(String credential) {
            this.credential = credential;
        }

        public Mt5AccountRole getAccountRole() {
            return accountRole;
        }

        public void setAccountRole(Mt5AccountRole accountRole) {
            this.accountRole = accountRole;
        }

        public AccountStatus getStatus() {
            return status;
        }

        public void setStatus(AccountStatus status) {
            this.status = status;
        }
    }

    public static class RiskRuleSpec {

        private Long accountId;
        private String accountAlias;
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

        public String getAccountAlias() {
            return accountAlias;
        }

        public void setAccountAlias(String accountAlias) {
            this.accountAlias = accountAlias;
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

    public static class CopyRelationSpec {

        private Long masterAccountId;
        private String masterAccountAlias;
        private Long followerAccountId;
        private String followerAccountAlias;
        private CopyMode copyMode;
        private CopyRelationStatus status = CopyRelationStatus.ACTIVE;
        private Integer priority = 100;

        public Long getMasterAccountId() {
            return masterAccountId;
        }

        public void setMasterAccountId(Long masterAccountId) {
            this.masterAccountId = masterAccountId;
        }

        public String getMasterAccountAlias() {
            return masterAccountAlias;
        }

        public void setMasterAccountAlias(String masterAccountAlias) {
            this.masterAccountAlias = masterAccountAlias;
        }

        public Long getFollowerAccountId() {
            return followerAccountId;
        }

        public void setFollowerAccountId(Long followerAccountId) {
            this.followerAccountId = followerAccountId;
        }

        public String getFollowerAccountAlias() {
            return followerAccountAlias;
        }

        public void setFollowerAccountAlias(String followerAccountAlias) {
            this.followerAccountAlias = followerAccountAlias;
        }

        public CopyMode getCopyMode() {
            return copyMode;
        }

        public void setCopyMode(CopyMode copyMode) {
            this.copyMode = copyMode;
        }

        public CopyRelationStatus getStatus() {
            return status;
        }

        public void setStatus(CopyRelationStatus status) {
            this.status = status;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }
    }

    public static class SymbolMappingSpec {

        private Long followerAccountId;
        private String followerAccountAlias;
        private String masterSymbol;
        private String followerSymbol;

        public Long getFollowerAccountId() {
            return followerAccountId;
        }

        public void setFollowerAccountId(Long followerAccountId) {
            this.followerAccountId = followerAccountId;
        }

        public String getFollowerAccountAlias() {
            return followerAccountAlias;
        }

        public void setFollowerAccountAlias(String followerAccountAlias) {
            this.followerAccountAlias = followerAccountAlias;
        }

        public String getMasterSymbol() {
            return masterSymbol;
        }

        public void setMasterSymbol(String masterSymbol) {
            this.masterSymbol = masterSymbol;
        }

        public String getFollowerSymbol() {
            return followerSymbol;
        }

        public void setFollowerSymbol(String followerSymbol) {
            this.followerSymbol = followerSymbol;
        }
    }
}
