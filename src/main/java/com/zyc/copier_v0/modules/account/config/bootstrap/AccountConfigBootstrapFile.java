package com.zyc.copier_v0.modules.account.config.bootstrap;

import com.zyc.copier_v0.modules.account.config.domain.AccountStatus;
import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import com.zyc.copier_v0.modules.account.config.domain.Mt5AccountRole;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.Getter;

@Getter
public class AccountConfigBootstrapFile {

    private List<AccountSpec> accounts = new ArrayList<>();
    private List<RiskRuleSpec> riskRules = new ArrayList<>();
    private List<CopyRelationSpec> copyRelations = new ArrayList<>();
    private List<SymbolMappingSpec> symbolMappings = new ArrayList<>();

    public void setAccounts(List<AccountSpec> accounts) {
        this.accounts = accounts == null ? new ArrayList<>() : accounts;
    }

    public void setRiskRules(List<RiskRuleSpec> riskRules) {
        this.riskRules = riskRules == null ? new ArrayList<>() : riskRules;
    }

    public void setCopyRelations(List<CopyRelationSpec> copyRelations) {
        this.copyRelations = copyRelations == null ? new ArrayList<>() : copyRelations;
    }

    public void setSymbolMappings(List<SymbolMappingSpec> symbolMappings) {
        this.symbolMappings = symbolMappings == null ? new ArrayList<>() : symbolMappings;
    }

    @Data
    public static class AccountSpec {

        private String alias;
        private Long userId;
        private String brokerName;
        private String serverName;
        private Long mt5Login;
        private String credential;
        private Mt5AccountRole accountRole;
        private AccountStatus status = AccountStatus.ACTIVE;
    }

    @Data
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
    }

    @Data
    public static class CopyRelationSpec {

        private Long masterAccountId;
        private String masterAccountAlias;
        private Long followerAccountId;
        private String followerAccountAlias;
        private CopyMode copyMode;
        private CopyRelationStatus status = CopyRelationStatus.ACTIVE;
        private Integer priority = 100;
    }

    @Data
    public static class SymbolMappingSpec {

        private Long followerAccountId;
        private String followerAccountAlias;
        private String masterSymbol;
        private String followerSymbol;
    }
}
