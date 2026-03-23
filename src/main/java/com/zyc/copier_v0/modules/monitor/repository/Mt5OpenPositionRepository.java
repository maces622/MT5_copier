package com.zyc.copier_v0.modules.monitor.repository;

import com.zyc.copier_v0.modules.monitor.entity.Mt5OpenPositionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Mt5OpenPositionRepository extends JpaRepository<Mt5OpenPositionEntity, Long> {

    List<Mt5OpenPositionEntity> findByAccountKeyOrderByPositionKeyAsc(String accountKey);

    List<Mt5OpenPositionEntity> findAllByOrderByAccountKeyAscPositionKeyAsc();
}
