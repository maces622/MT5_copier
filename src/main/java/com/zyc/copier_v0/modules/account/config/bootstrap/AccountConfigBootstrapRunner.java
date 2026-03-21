package com.zyc.copier_v0.modules.account.config.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.account.config.api.BindMt5AccountRequest;
import com.zyc.copier_v0.modules.account.config.api.CreateCopyRelationRequest;
import com.zyc.copier_v0.modules.account.config.api.Mt5AccountResponse;
import com.zyc.copier_v0.modules.account.config.api.SaveRiskRuleRequest;
import com.zyc.copier_v0.modules.account.config.api.SaveSymbolMappingRequest;
import com.zyc.copier_v0.modules.account.config.api.UpdateCopyRelationRequest;
import com.zyc.copier_v0.modules.account.config.entity.CopyRelationEntity;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import com.zyc.copier_v0.modules.account.config.service.AccountConfigService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AccountConfigBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccountConfigBootstrapRunner.class);

    private final ObjectMapper objectMapper;
    private final AccountConfigService accountConfigService;
    private final CopyRelationRepository copyRelationRepository;
    private final ConfigurableApplicationContext applicationContext;
    private final String configFile;
    private final boolean exitOnComplete;

    public AccountConfigBootstrapRunner(
            ObjectMapper objectMapper,
            AccountConfigService accountConfigService,
            CopyRelationRepository copyRelationRepository,
            ConfigurableApplicationContext applicationContext,
            @Value("${copier.bootstrap.config-file:}") String configFile,
            @Value("${copier.bootstrap.exit-on-complete:true}") boolean exitOnComplete
    ) {
        this.objectMapper = objectMapper;
        this.accountConfigService = accountConfigService;
        this.copyRelationRepository = copyRelationRepository;
        this.applicationContext = applicationContext;
        this.configFile = configFile;
        this.exitOnComplete = exitOnComplete;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!StringUtils.hasText(configFile)) {
            return;
        }

        Path path = resolveConfigPath(configFile);
        AccountConfigBootstrapFile bootstrapFile = loadBootstrapFile(path);
        Map<String, Long> accountAliases = bindAccounts(bootstrapFile);

        saveRiskRules(bootstrapFile, accountAliases);
        saveCopyRelations(bootstrapFile, accountAliases);
        saveSymbolMappings(bootstrapFile, accountAliases);

        log.info("Account config bootstrap completed, configFile={}, accounts={}, riskRules={}, copyRelations={}, symbolMappings={}",
                path,
                bootstrapFile.getAccounts().size(),
                bootstrapFile.getRiskRules().size(),
                bootstrapFile.getCopyRelations().size(),
                bootstrapFile.getSymbolMappings().size());

        if (exitOnComplete) {
            log.info("Account config bootstrap exit-on-complete enabled, closing application context");
            applicationContext.close();
        }
    }

    private AccountConfigBootstrapFile loadBootstrapFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Bootstrap config file not found: " + path);
        }
        return objectMapper.readValue(path.toFile(), AccountConfigBootstrapFile.class);
    }

    private Path resolveConfigPath(String rawPath) {
        Path path = Paths.get(rawPath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get("").toAbsolutePath().resolve(path).normalize();
    }

    private Map<String, Long> bindAccounts(AccountConfigBootstrapFile bootstrapFile) {
        Map<String, Long> accountAliases = new LinkedHashMap<>();
        for (AccountConfigBootstrapFile.AccountSpec spec : bootstrapFile.getAccounts()) {
            BindMt5AccountRequest request = new BindMt5AccountRequest();
            request.setUserId(spec.getUserId());
            request.setBrokerName(spec.getBrokerName());
            request.setServerName(spec.getServerName());
            request.setMt5Login(spec.getMt5Login());
            request.setCredential(spec.getCredential());
            request.setAccountRole(spec.getAccountRole());
            request.setStatus(spec.getStatus());

            Mt5AccountResponse response = accountConfigService.bindAccount(request);
            String alias = normalizeAlias(spec.getAlias());
            if (alias != null) {
                if (accountAliases.putIfAbsent(alias, response.getId()) != null) {
                    throw new IllegalArgumentException("Duplicate bootstrap account alias: " + alias);
                }
            }
        }
        return accountAliases;
    }

    private void saveRiskRules(AccountConfigBootstrapFile bootstrapFile, Map<String, Long> accountAliases) {
        for (AccountConfigBootstrapFile.RiskRuleSpec spec : bootstrapFile.getRiskRules()) {
            SaveRiskRuleRequest request = new SaveRiskRuleRequest();
            request.setAccountId(resolveAccountId(spec.getAccountId(), spec.getAccountAlias(), accountAliases, "risk rule"));
            request.setMaxLot(spec.getMaxLot());
            request.setFixedLot(spec.getFixedLot());
            request.setBalanceRatio(spec.getBalanceRatio());
            request.setMaxSlippagePoints(spec.getMaxSlippagePoints());
            request.setMaxSlippagePips(spec.getMaxSlippagePips());
            request.setMaxSlippagePrice(spec.getMaxSlippagePrice());
            request.setMaxDailyLoss(spec.getMaxDailyLoss());
            request.setMaxDrawdownPct(spec.getMaxDrawdownPct());
            request.setAllowedSymbols(spec.getAllowedSymbols());
            request.setBlockedSymbols(spec.getBlockedSymbols());
            request.setFollowTpSl(spec.getFollowTpSl());
            request.setReverseFollow(spec.getReverseFollow());
            accountConfigService.saveRiskRule(request);
        }
    }

    private void saveCopyRelations(AccountConfigBootstrapFile bootstrapFile, Map<String, Long> accountAliases) {
        for (AccountConfigBootstrapFile.CopyRelationSpec spec : bootstrapFile.getCopyRelations()) {
            Long masterAccountId = resolveAccountId(
                    spec.getMasterAccountId(),
                    spec.getMasterAccountAlias(),
                    accountAliases,
                    "copy relation master"
            );
            Long followerAccountId = resolveAccountId(
                    spec.getFollowerAccountId(),
                    spec.getFollowerAccountAlias(),
                    accountAliases,
                    "copy relation follower"
            );

            CopyRelationEntity existing = copyRelationRepository
                    .findByMasterAccount_IdAndFollowerAccount_Id(masterAccountId, followerAccountId)
                    .orElse(null);
            if (existing == null) {
                CreateCopyRelationRequest request = new CreateCopyRelationRequest();
                request.setMasterAccountId(masterAccountId);
                request.setFollowerAccountId(followerAccountId);
                request.setCopyMode(spec.getCopyMode());
                request.setStatus(spec.getStatus());
                request.setPriority(spec.getPriority());
                accountConfigService.createCopyRelation(request);
                continue;
            }

            UpdateCopyRelationRequest request = new UpdateCopyRelationRequest();
            request.setCopyMode(spec.getCopyMode());
            request.setStatus(spec.getStatus());
            request.setPriority(spec.getPriority());
            accountConfigService.updateCopyRelation(existing.getId(), request);
        }
    }

    private void saveSymbolMappings(AccountConfigBootstrapFile bootstrapFile, Map<String, Long> accountAliases) {
        for (AccountConfigBootstrapFile.SymbolMappingSpec spec : bootstrapFile.getSymbolMappings()) {
            SaveSymbolMappingRequest request = new SaveSymbolMappingRequest();
            request.setFollowerAccountId(resolveAccountId(
                    spec.getFollowerAccountId(),
                    spec.getFollowerAccountAlias(),
                    accountAliases,
                    "symbol mapping follower"
            ));
            request.setMasterSymbol(spec.getMasterSymbol());
            request.setFollowerSymbol(spec.getFollowerSymbol());
            accountConfigService.saveSymbolMapping(request);
        }
    }

    private Long resolveAccountId(Long accountId, String accountAlias, Map<String, Long> accountAliases, String context) {
        if (accountId != null) {
            return accountId;
        }

        String alias = normalizeAlias(accountAlias);
        if (alias != null && accountAliases.containsKey(alias)) {
            return accountAliases.get(alias);
        }

        throw new IllegalArgumentException("Missing accountId/accountAlias for " + context);
    }

    private String normalizeAlias(String alias) {
        if (!StringUtils.hasText(alias)) {
            return null;
        }
        return alias.trim();
    }
}
