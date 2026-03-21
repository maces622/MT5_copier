package com.zyc.copier_v0.modules.monitor.repository;

import com.zyc.copier_v0.modules.monitor.entity.Mt5AccountRuntimeStateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Mt5AccountRuntimeStateRepository extends JpaRepository<Mt5AccountRuntimeStateEntity, Long> {

    Optional<Mt5AccountRuntimeStateEntity> findByServerAndLogin(String server, Long login);

    List<Mt5AccountRuntimeStateEntity> findAllByOrderByUpdatedAtDesc();
}
