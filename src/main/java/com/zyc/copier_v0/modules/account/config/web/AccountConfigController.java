package com.zyc.copier_v0.modules.account.config.web;

import com.zyc.copier_v0.modules.account.config.api.BindMt5AccountRequest;
import com.zyc.copier_v0.modules.account.config.api.CopyRelationResponse;
import com.zyc.copier_v0.modules.account.config.api.CreateCopyRelationRequest;
import com.zyc.copier_v0.modules.account.config.api.Mt5AccountResponse;
import com.zyc.copier_v0.modules.account.config.api.RiskRuleResponse;
import com.zyc.copier_v0.modules.account.config.api.SaveRiskRuleRequest;
import com.zyc.copier_v0.modules.account.config.api.SaveSymbolMappingRequest;
import com.zyc.copier_v0.modules.account.config.api.SymbolMappingResponse;
import com.zyc.copier_v0.modules.account.config.api.UpdateCopyRelationRequest;
import com.zyc.copier_v0.modules.account.config.service.AccountConfigService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AccountConfigController {

    private final AccountConfigService accountConfigService;

    public AccountConfigController(AccountConfigService accountConfigService) {
        this.accountConfigService = accountConfigService;
    }

    @PostMapping("/accounts")
    public Mt5AccountResponse bindAccount(@Valid @RequestBody BindMt5AccountRequest request) {
        return accountConfigService.bindAccount(request);
    }

    @GetMapping("/accounts")
    public List<Mt5AccountResponse> listAccounts() {
        return accountConfigService.listAccounts();
    }

    @PostMapping("/risk-rules")
    public RiskRuleResponse saveRiskRule(@Valid @RequestBody SaveRiskRuleRequest request) {
        return accountConfigService.saveRiskRule(request);
    }

    @PostMapping("/copy-relations")
    public CopyRelationResponse createCopyRelation(@Valid @RequestBody CreateCopyRelationRequest request) {
        return accountConfigService.createCopyRelation(request);
    }

    @PutMapping("/copy-relations/{relationId}")
    public CopyRelationResponse updateCopyRelation(
            @PathVariable Long relationId,
            @Valid @RequestBody UpdateCopyRelationRequest request
    ) {
        return accountConfigService.updateCopyRelation(relationId, request);
    }

    @GetMapping("/copy-relations/master/{masterAccountId}")
    public List<CopyRelationResponse> listRelationsByMaster(@PathVariable Long masterAccountId) {
        return accountConfigService.listRelationsByMaster(masterAccountId);
    }

    @PostMapping("/symbol-mappings")
    public SymbolMappingResponse saveSymbolMapping(@Valid @RequestBody SaveSymbolMappingRequest request) {
        return accountConfigService.saveSymbolMapping(request);
    }

    @GetMapping("/symbol-mappings/followers/{followerAccountId}")
    public List<SymbolMappingResponse> listSymbolMappings(@PathVariable Long followerAccountId) {
        return accountConfigService.listSymbolMappings(followerAccountId);
    }
}
