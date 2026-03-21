package com.zyc.copier_v0.modules.account.config.cache;

import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import com.zyc.copier_v0.modules.account.config.entity.CopyRelationEntity;
import com.zyc.copier_v0.modules.account.config.entity.Mt5AccountEntity;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public void warmUpAll() {
        Set<CopyRelationEntity> relations = copyRelationRepository.findAllByStatusIn(
                        EnumSet.of(CopyRelationStatus.ACTIVE, CopyRelationStatus.PAUSED)
                ).stream()
                .collect(Collectors.toSet());

        Set<Long> masterIds = relations.stream()
                .map(relation -> relation.getMasterAccount().getId())
                .collect(Collectors.toSet());
        Map<String, Mt5AccountEntity> mastersByBinding = relations.stream()
                .map(CopyRelationEntity::getMasterAccount)
                .collect(Collectors.toMap(
                        master -> master.getServerName() + ":" + master.getMt5Login(),
                        master -> master,
                        (left, right) -> left
                ));
        Set<Long> followerIds = relations.stream()
                .map(relation -> relation.getFollowerAccount().getId())
                .collect(Collectors.toSet());

        masterIds.forEach(snapshotReader::loadMasterRoute);
        mastersByBinding.values().stream()
                .forEach(master -> snapshotReader.loadAccountBinding(master.getServerName(), master.getMt5Login()));
        followerIds.forEach(snapshotReader::loadFollowerRisk);

        log.info("Route cache warmup completed, masters={}, masterBindings={}, followers={}",
                masterIds.size(), mastersByBinding.size(), followerIds.size());
    }
}
