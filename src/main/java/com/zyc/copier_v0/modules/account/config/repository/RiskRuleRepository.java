package com.zyc.copier_v0.modules.account.config.repository;

import com.zyc.copier_v0.modules.account.config.entity.RiskRuleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskRuleRepository extends JpaRepository<RiskRuleEntity, Long> {

    Optional<RiskRuleEntity> findByAccount_Id(Long accountId);
}
