package com.zyc.copier_v0.modules.account.config.service;

import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import com.zyc.copier_v0.modules.account.config.entity.CopyRelationEntity;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CopyRelationGraphValidator {

    private final CopyRelationRepository copyRelationRepository;

    public CopyRelationGraphValidator(CopyRelationRepository copyRelationRepository) {
        this.copyRelationRepository = copyRelationRepository;
    }

    public void validate(Long relationIdToIgnore, Long masterAccountId, Long followerAccountId, CopyRelationStatus targetStatus) {
        if (masterAccountId.equals(followerAccountId)) {
            throw new IllegalArgumentException("Master account and follower account cannot be the same");
        }
        if (targetStatus == CopyRelationStatus.DISABLED) {
            return;
        }

        Map<Long, List<Long>> graph = new HashMap<>();
        List<CopyRelationEntity> relations = copyRelationRepository.findAllByStatusIn(
                EnumSet.of(CopyRelationStatus.ACTIVE, CopyRelationStatus.PAUSED)
        );

        for (CopyRelationEntity relation : relations) {
            if (relationIdToIgnore != null && relationIdToIgnore.equals(relation.getId())) {
                continue;
            }
            graph.computeIfAbsent(relation.getMasterAccount().getId(), ignored -> new ArrayList<>())
                    .add(relation.getFollowerAccount().getId());
        }

        graph.computeIfAbsent(masterAccountId, ignored -> new ArrayList<>()).add(followerAccountId);
        if (hasPath(followerAccountId, masterAccountId, graph)) {
            throw new IllegalArgumentException("Copy relation would create a cycle");
        }
    }

    private boolean hasPath(Long start, Long target, Map<Long, List<Long>> graph) {
        Deque<Long> stack = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        stack.push(start);

        while (!stack.isEmpty()) {
            Long current = stack.pop();
            if (!visited.add(current)) {
                continue;
            }
            if (current.equals(target)) {
                return true;
            }
            List<Long> nextNodes = graph.get(current);
            if (nextNodes == null) {
                continue;
            }
            for (Long next : nextNodes) {
                stack.push(next);
            }
        }
        return false;
    }
}
