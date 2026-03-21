package com.zyc.copier_v0.modules.monitor.repository;

import com.zyc.copier_v0.modules.monitor.entity.Mt5SignalRecordEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Mt5SignalRecordRepository extends JpaRepository<Mt5SignalRecordEntity, Long> {

    List<Mt5SignalRecordEntity> findTop50ByAccountIdOrderByReceivedAtDesc(Long accountId);

    List<Mt5SignalRecordEntity> findTop50ByAccountKeyOrderByReceivedAtDesc(String accountKey);
}
