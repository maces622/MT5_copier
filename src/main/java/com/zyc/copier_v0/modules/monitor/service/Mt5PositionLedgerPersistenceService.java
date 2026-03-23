package com.zyc.copier_v0.modules.monitor.service;

import com.zyc.copier_v0.modules.monitor.entity.Mt5OpenPositionEntity;
import com.zyc.copier_v0.modules.monitor.repository.Mt5OpenPositionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class Mt5PositionLedgerPersistenceService {

    private final Mt5OpenPositionRepository repository;

    public Mt5PositionLedgerPersistenceService(Mt5OpenPositionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void persistReconcileMessage(Mt5PositionLedgerReconcileMessage message) {
        List<Mt5OpenPositionEntity> existing = repository.findByAccountKeyOrderByPositionKeyAsc(message.getAccountKey());
        Map<String, Mt5OpenPositionEntity> existingByKey = existing.stream()
                .collect(Collectors.toMap(Mt5OpenPositionEntity::getPositionKey, entity -> entity, (left, right) -> left, LinkedHashMap::new));

        List<Mt5OpenPositionEntity> toSave = new ArrayList<>();
        for (Mt5OpenPositionSnapshot snapshot : message.getPositions()) {
            Mt5OpenPositionEntity entity = existingByKey.remove(snapshot.getPositionKey());
            if (entity == null) {
                entity = new Mt5OpenPositionEntity();
            }
            entity.setAccountId(message.getAccountId());
            entity.setLogin(message.getLogin());
            entity.setServer(message.getServer());
            entity.setAccountKey(message.getAccountKey());
            entity.setPositionKey(snapshot.getPositionKey());
            entity.setSourcePositionId(snapshot.getSourcePositionId());
            entity.setSourceOrderId(snapshot.getSourceOrderId());
            entity.setMasterPositionId(snapshot.getMasterPositionId());
            entity.setMasterOrderId(snapshot.getMasterOrderId());
            entity.setSymbol(snapshot.getSymbol());
            entity.setVolume(snapshot.getVolume());
            entity.setPriceOpen(snapshot.getPriceOpen());
            entity.setSl(snapshot.getSl());
            entity.setTp(snapshot.getTp());
            entity.setCommentText(snapshot.getCommentText());
            entity.setObservedAt(snapshot.getObservedAt());
            toSave.add(entity);
        }

        repository.saveAll(toSave);
        if (!existingByKey.isEmpty()) {
            repository.deleteAll(existingByKey.values());
        }
    }
}
