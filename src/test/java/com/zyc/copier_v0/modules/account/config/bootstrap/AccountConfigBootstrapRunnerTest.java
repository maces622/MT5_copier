package com.zyc.copier_v0.modules.account.config.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyc.copier_v0.modules.account.config.api.Mt5AccountResponse;
import com.zyc.copier_v0.modules.account.config.entity.CopyRelationEntity;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import com.zyc.copier_v0.modules.account.config.service.AccountConfigService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

class AccountConfigBootstrapRunnerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldBootstrapAccountConfigFromJsonFile() throws Exception {
        AccountConfigService accountConfigService = mock(AccountConfigService.class);
        CopyRelationRepository copyRelationRepository = mock(CopyRelationRepository.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        Mt5AccountResponse master = new Mt5AccountResponse();
        master.setId(11L);
        Mt5AccountResponse follower = new Mt5AccountResponse();
        follower.setId(12L);

        when(accountConfigService.bindAccount(any())).thenReturn(master, follower);
        when(copyRelationRepository.findByMasterAccount_IdAndFollowerAccount_Id(11L, 12L)).thenReturn(Optional.empty());

        Path config = tempDir.resolve("bootstrap.json");
        Files.writeString(config, """
                {
                  "accounts": [
                    {
                      "alias": "master",
                      "userId": 10001,
                      "brokerName": "EBC",
                      "serverName": "EBCFinancialGroupKY-Demo",
                      "mt5Login": 51631,
                      "accountRole": "MASTER",
                      "status": "ACTIVE"
                    },
                    {
                      "alias": "follower",
                      "userId": 10001,
                      "brokerName": "EBC",
                      "serverName": "EBCFinancialGroupKY-Demo",
                      "mt5Login": 51629,
                      "accountRole": "FOLLOWER",
                      "status": "ACTIVE"
                    }
                  ],
                  "riskRules": [
                    {
                      "accountAlias": "follower",
                      "fixedLot": 0.10,
                      "maxLot": 1.00,
                      "maxSlippagePips": 10,
                      "followTpSl": true
                    }
                  ],
                  "copyRelations": [
                    {
                      "masterAccountAlias": "master",
                      "followerAccountAlias": "follower",
                      "copyMode": "FIXED_LOT",
                      "status": "ACTIVE",
                      "priority": 100
                    }
                  ],
                  "symbolMappings": [
                    {
                      "followerAccountAlias": "follower",
                      "masterSymbol": "XAUUSD",
                      "followerSymbol": "XAUUSDm"
                    }
                  ]
                }
                """, StandardCharsets.US_ASCII);

        AccountConfigBootstrapRunner runner = new AccountConfigBootstrapRunner(
                new ObjectMapper(),
                accountConfigService,
                copyRelationRepository,
                applicationContext,
                config.toString(),
                false
        );

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(accountConfigService).saveRiskRule(any());
        verify(accountConfigService).createCopyRelation(any());
        verify(accountConfigService).saveSymbolMapping(any());
        verify(copyRelationRepository).findByMasterAccount_IdAndFollowerAccount_Id(eq(11L), eq(12L));
        verify(applicationContext, never()).close();
    }

    @Test
    void shouldUpdateExistingRelationWhenBootstrapRelationAlreadyExists() throws Exception {
        AccountConfigService accountConfigService = mock(AccountConfigService.class);
        CopyRelationRepository copyRelationRepository = mock(CopyRelationRepository.class);
        ConfigurableApplicationContext applicationContext = mock(ConfigurableApplicationContext.class);

        Mt5AccountResponse master = new Mt5AccountResponse();
        master.setId(21L);
        Mt5AccountResponse follower = new Mt5AccountResponse();
        follower.setId(22L);

        CopyRelationEntity existing = mock(CopyRelationEntity.class);
        when(existing.getId()).thenReturn(99L);

        when(accountConfigService.bindAccount(any())).thenReturn(master, follower);
        when(copyRelationRepository.findByMasterAccount_IdAndFollowerAccount_Id(21L, 22L)).thenReturn(Optional.of(existing));

        Path config = tempDir.resolve("bootstrap-update.json");
        Files.writeString(config, """
                {
                  "accounts": [
                    {
                      "alias": "master",
                      "userId": 10001,
                      "brokerName": "EBC",
                      "serverName": "S1",
                      "mt5Login": 1,
                      "accountRole": "MASTER"
                    },
                    {
                      "alias": "follower",
                      "userId": 10001,
                      "brokerName": "EBC",
                      "serverName": "S2",
                      "mt5Login": 2,
                      "accountRole": "FOLLOWER"
                    }
                  ],
                  "copyRelations": [
                    {
                      "masterAccountAlias": "master",
                      "followerAccountAlias": "follower",
                      "copyMode": "FIXED_LOT",
                      "status": "ACTIVE",
                      "priority": 90
                    }
                  ]
                }
                """, StandardCharsets.US_ASCII);

        AccountConfigBootstrapRunner runner = new AccountConfigBootstrapRunner(
                new ObjectMapper(),
                accountConfigService,
                copyRelationRepository,
                applicationContext,
                config.toString(),
                false
        );

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(accountConfigService).updateCopyRelation(eq(99L), any());
        verify(accountConfigService, never()).createCopyRelation(any());
    }
}
