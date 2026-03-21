package com.zyc.copier_v0.modules.account.config.cache;

import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import com.zyc.copier_v0.modules.account.config.entity.CopyRelationEntity;
import com.zyc.copier_v0.modules.account.config.entity.RiskRuleEntity;
import com.zyc.copier_v0.modules.account.config.entity.SymbolMappingEntity;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import com.zyc.copier_v0.modules.account.config.repository.RiskRuleRepository;
import com.zyc.copier_v0.modules.account.config.repository.SymbolMappingRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CopyRouteSnapshotFactory {

    private final CopyRelationRepository copyRelationRepository;
    private final RiskRuleRepository riskRuleRepository;
    private final SymbolMappingRepository symbolMappingRepository;

    public CopyRouteSnapshotFactory(
            CopyRelationRepository copyRelationRepository,
            RiskRuleRepository riskRuleRepository,
            SymbolMappingRepository symbolMappingRepository
    ) {
        this.copyRelationRepository = copyRelationRepository;
        this.riskRuleRepository = riskRuleRepository;
        this.symbolMappingRepository = symbolMappingRepository;
    }

    public MasterRouteCacheSnapshot buildMasterRoute(Long masterAccountId) {
        List<CopyRelationEntity> relations = copyRelationRepository.findByMasterAccount_IdAndStatusOrderByPriorityAscIdAsc(
                masterAccountId,
                CopyRelationStatus.ACTIVE
        );

        MasterRouteCacheSnapshot snapshot = new MasterRouteCacheSnapshot();
        snapshot.setMasterAccountId(masterAccountId);

        long routeVersion = 0L;
        List<FollowerRouteCacheItem> followers = new ArrayList<>();
        for (CopyRelationEntity relation : relations) {
            FollowerRouteCacheItem item = new FollowerRouteCacheItem();
            item.setFollowerAccountId(relation.getFollowerAccount().getId());
            item.setCopyMode(relation.getCopyMode());
            item.setPriority(relation.getPriority());
            item.setConfigVersion(relation.getConfigVersion());
            item.setRisk(buildFollowerRisk(relation.getFollowerAccount().getId()));
            followers.add(item);
            routeVersion = Math.max(routeVersion, relation.getConfigVersion());
        }

        snapshot.setRouteVersion(routeVersion);
        snapshot.setFollowers(followers);
        return snapshot;
    }

    public FollowerRiskCacheSnapshot buildFollowerRisk(Long followerAccountId) {
        RiskRuleEntity riskRule = riskRuleRepository.findByAccount_Id(followerAccountId).orElse(null);
        FollowerRiskCacheSnapshot snapshot = new FollowerRiskCacheSnapshot();
        snapshot.setAccountId(followerAccountId);
        snapshot.setSymbolMappings(loadSymbolMappings(followerAccountId));
        if (riskRule == null) {
            return snapshot;
        }

        snapshot.setMaxLot(riskRule.getMaxLot());
        snapshot.setFixedLot(riskRule.getFixedLot());
        snapshot.setBalanceRatio(riskRule.getBalanceRatio());
        snapshot.setMaxSlippagePoints(riskRule.getMaxSlippagePoints());
        snapshot.setMaxSlippagePips(riskRule.getMaxSlippagePips());
        snapshot.setMaxSlippagePrice(riskRule.getMaxSlippagePrice());
        snapshot.setMaxDailyLoss(riskRule.getMaxDailyLoss());
        snapshot.setMaxDrawdownPct(riskRule.getMaxDrawdownPct());
        snapshot.setAllowedSymbols(riskRule.getAllowedSymbols());
        snapshot.setBlockedSymbols(riskRule.getBlockedSymbols());
        snapshot.setFollowTpSl(riskRule.isFollowTpSl());
        snapshot.setReverseFollow(riskRule.isReverseFollow());
        return snapshot;
    }

    private Map<String, String> loadSymbolMappings(Long followerAccountId) {
        List<SymbolMappingEntity> mappings = symbolMappingRepository.findByFollowerAccount_IdOrderByMasterSymbolAsc(followerAccountId);
        if (mappings.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> snapshot = new LinkedHashMap<>();
        for (SymbolMappingEntity mapping : mappings) {
            snapshot.put(mapping.getMasterSymbol(), mapping.getFollowerSymbol());
        }
        return snapshot;
    }

    public List<Long> findMastersByFollower(Long followerAccountId) {
        List<CopyRelationEntity> relations = copyRelationRepository.findByFollowerAccount_IdAndStatusIn(
                followerAccountId,
                EnumSet.of(CopyRelationStatus.ACTIVE, CopyRelationStatus.PAUSED)
        );
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> masterIds = new ArrayList<>();
        for (CopyRelationEntity relation : relations) {
            masterIds.add(relation.getMasterAccount().getId());
        }
        return masterIds;
    }
}
