package com.zyc.copier_v0.modules.copy.engine.repository;

import com.zyc.copier_v0.modules.copy.engine.domain.FollowerDispatchStatus;
import com.zyc.copier_v0.modules.copy.engine.entity.FollowerDispatchOutboxEntity;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowerDispatchOutboxRepository extends JpaRepository<FollowerDispatchOutboxEntity, Long> {

    boolean existsByExecutionCommandId(Long executionCommandId);

    List<FollowerDispatchOutboxEntity> findByFollowerAccountIdOrderByIdDesc(Long followerAccountId);

    List<FollowerDispatchOutboxEntity> findByFollowerAccountIdAndStatusOrderByIdAsc(Long followerAccountId, FollowerDispatchStatus status);

    List<FollowerDispatchOutboxEntity> findByMasterEventIdOrderByIdAsc(String masterEventId);

    List<FollowerDispatchOutboxEntity> findByExecutionCommandIdInOrderByIdAsc(Collection<Long> executionCommandIds);
}
