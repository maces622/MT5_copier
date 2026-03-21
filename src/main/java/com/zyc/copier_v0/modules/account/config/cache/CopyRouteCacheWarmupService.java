package com.zyc.copier_v0.modules.account.config.cache;

import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import com.zyc.copier_v0.modules.account.config.entity.CopyRelationEntity;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CopyRouteCacheWarmupService {

    private static final Logger log = LoggerFactory.getLogger(CopyRouteCacheWarmupService.class);

    private final CopyRelationRepository copyRelationRepository;
    private final CopyRouteSnapshotReader snapshotReader;

    public CopyRouteCacheWarmupService(
            CopyRelationRepository copyRelationRepository,
            CopyRouteSnapshotReader snapshotReader
    ) {
        this.copyRelationRepository = copyRelationRepository;
        this.snapshotReader = snapshotReader;
    }

    public void warmUpAll() {
        Set<CopyRelationEntity> relations = copyRelationRepository.findAllByStatusIn(
                        EnumSet.of(CopyRelationStatus.ACTIVE, CopyRelationStatus.PAUSED)
                ).stream()
                .collect(Collectors.toSet());

        Set<Long> masterIds = relations.stream()
                .map(relation -> relation.getMasterAccount().getId())
                .collect(Collectors.toSet());
        Set<Long> followerIds = relations.stream()
                .map(relation -> relation.getFollowerAccount().getId())
                .collect(Collectors.toSet());

        masterIds.forEach(snapshotReader::loadMasterRoute);
        followerIds.forEach(snapshotReader::loadFollowerRisk);

        log.info("Route cache warmup completed, masters={}, followers={}", masterIds.size(), followerIds.size());
    }
}
