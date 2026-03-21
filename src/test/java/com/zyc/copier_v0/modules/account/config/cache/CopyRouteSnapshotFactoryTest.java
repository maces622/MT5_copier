package com.zyc.copier_v0.modules.account.config.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zyc.copier_v0.modules.account.config.domain.CopyMode;
import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import com.zyc.copier_v0.modules.account.config.entity.CopyRelationEntity;
import com.zyc.copier_v0.modules.account.config.entity.Mt5AccountEntity;
import com.zyc.copier_v0.modules.account.config.entity.RiskRuleEntity;
import com.zyc.copier_v0.modules.account.config.entity.SymbolMappingEntity;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import com.zyc.copier_v0.modules.account.config.repository.RiskRuleRepository;
import com.zyc.copier_v0.modules.account.config.repository.SymbolMappingRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CopyRouteSnapshotFactoryTest {

    @Mock
    private CopyRelationRepository copyRelationRepository;

    @Mock
    private RiskRuleRepository riskRuleRepository;

    @Mock
    private SymbolMappingRepository symbolMappingRepository;

    @Test
    void shouldBatchLoadFollowerRiskAndMappingsWhenBuildingMasterRoute() {
        CopyRouteSnapshotFactory factory = new CopyRouteSnapshotFactory(
                copyRelationRepository,
                riskRuleRepository,
                symbolMappingRepository
        );

        Mt5AccountEntity master = account(1L);
        Mt5AccountEntity followerA = account(2L);
        Mt5AccountEntity followerB = account(3L);

        when(copyRelationRepository.findByMasterAccount_IdAndStatusOrderByPriorityAscIdAsc(1L, CopyRelationStatus.ACTIVE))
                .thenReturn(List.of(
                        relation(master, followerA, 100, 2L),
                        relation(master, followerB, 200, 3L)
                ));
        when(riskRuleRepository.findByAccount_IdIn(anyCollection()))
                .thenReturn(List.of(
                        riskRule(followerA, new BigDecimal("1.0"), new BigDecimal("0.80"), true, false),
                        riskRule(followerB, new BigDecimal("2.0"), new BigDecimal("1.20"), false, true)
                ));
        when(symbolMappingRepository.findByFollowerAccount_IdIn(anyCollection()))
                .thenReturn(List.of(
                        symbolMapping(followerB, "US30", "US30.cash"),
                        symbolMapping(followerA, "EURUSD", "EURUSDm"),
                        symbolMapping(followerA, "XAUUSD", "XAUUSDm")
                ));

        MasterRouteCacheSnapshot snapshot = factory.buildMasterRoute(1L);

        assertThat(snapshot.getMasterAccountId()).isEqualTo(1L);
        assertThat(snapshot.getRouteVersion()).isEqualTo(3L);
        assertThat(snapshot.getFollowers()).hasSize(2);
        assertThat(snapshot.getFollowers().get(0).getFollowerAccountId()).isEqualTo(2L);
        assertThat(snapshot.getFollowers().get(0).getRisk().getBalanceRatio()).isEqualByComparingTo("0.80");
        assertThat(snapshot.getFollowers().get(0).getRisk().getSymbolMappings())
                .containsEntry("EURUSD", "EURUSDm")
                .containsEntry("XAUUSD", "XAUUSDm");
        assertThat(snapshot.getFollowers().get(1).getFollowerAccountId()).isEqualTo(3L);
        assertThat(snapshot.getFollowers().get(1).getRisk().getMaxLot()).isEqualByComparingTo("2.0");
        assertThat(snapshot.getFollowers().get(1).getRisk().getSymbolMappings())
                .containsEntry("US30", "US30.cash");

        ArgumentCaptor<java.util.Collection<Long>> followerIdsCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(riskRuleRepository).findByAccount_IdIn(followerIdsCaptor.capture());
        assertThat(followerIdsCaptor.getValue()).containsExactly(2L, 3L);

        ArgumentCaptor<java.util.Collection<Long>> mappingFollowerIdsCaptor = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(symbolMappingRepository).findByFollowerAccount_IdIn(mappingFollowerIdsCaptor.capture());
        assertThat(mappingFollowerIdsCaptor.getValue()).containsExactly(2L, 3L);

        verify(riskRuleRepository, never()).findByAccount_Id(anyLong());
        verify(symbolMappingRepository, never()).findByFollowerAccount_IdOrderByMasterSymbolAsc(anyLong());
    }

    private Mt5AccountEntity account(Long id) {
        Mt5AccountEntity account = new Mt5AccountEntity();
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private CopyRelationEntity relation(Mt5AccountEntity master, Mt5AccountEntity follower, int priority, long configVersion) {
        CopyRelationEntity relation = new CopyRelationEntity();
        relation.setMasterAccount(master);
        relation.setFollowerAccount(follower);
        relation.setCopyMode(CopyMode.BALANCE_RATIO);
        relation.setStatus(CopyRelationStatus.ACTIVE);
        relation.setPriority(priority);
        relation.setConfigVersion(configVersion);
        return relation;
    }

    private RiskRuleEntity riskRule(
            Mt5AccountEntity account,
            BigDecimal maxLot,
            BigDecimal balanceRatio,
            boolean followTpSl,
            boolean reverseFollow
    ) {
        RiskRuleEntity rule = new RiskRuleEntity();
        rule.setAccount(account);
        rule.setMaxLot(maxLot);
        rule.setBalanceRatio(balanceRatio);
        rule.setFollowTpSl(followTpSl);
        rule.setReverseFollow(reverseFollow);
        return rule;
    }

    private SymbolMappingEntity symbolMapping(Mt5AccountEntity follower, String masterSymbol, String followerSymbol) {
        SymbolMappingEntity mapping = new SymbolMappingEntity();
        mapping.setFollowerAccount(follower);
        mapping.setMasterSymbol(masterSymbol);
        mapping.setFollowerSymbol(followerSymbol);
        return mapping;
    }
}
