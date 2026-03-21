package com.zyc.copier_v0.modules.account.config.repository;

import com.zyc.copier_v0.modules.account.config.entity.SymbolMappingEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SymbolMappingRepository extends JpaRepository<SymbolMappingEntity, Long> {

    Optional<SymbolMappingEntity> findByFollowerAccount_IdAndMasterSymbol(Long followerAccountId, String masterSymbol);

    List<SymbolMappingEntity> findByFollowerAccount_IdOrderByMasterSymbolAsc(Long followerAccountId);

    List<SymbolMappingEntity> findByFollowerAccount_IdIn(Collection<Long> followerAccountIds);
}
