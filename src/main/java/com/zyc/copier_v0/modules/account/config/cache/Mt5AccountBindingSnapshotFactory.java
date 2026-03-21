package com.zyc.copier_v0.modules.account.config.cache;

import com.zyc.copier_v0.modules.account.config.entity.Mt5AccountEntity;
import com.zyc.copier_v0.modules.account.config.repository.Mt5AccountRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class Mt5AccountBindingSnapshotFactory {

    private final Mt5AccountRepository mt5AccountRepository;

    public Mt5AccountBindingSnapshotFactory(Mt5AccountRepository mt5AccountRepository) {
        this.mt5AccountRepository = mt5AccountRepository;
    }

    public Optional<Mt5AccountBindingCacheSnapshot> findByServerAndLogin(String serverName, Long mt5Login) {
        return mt5AccountRepository.findByServerNameAndMt5Login(serverName, mt5Login)
                .map(this::buildSnapshot);
    }

    public Mt5AccountBindingCacheSnapshot buildSnapshot(Mt5AccountEntity account) {
        Mt5AccountBindingCacheSnapshot snapshot = new Mt5AccountBindingCacheSnapshot();
        snapshot.setAccountId(account.getId());
        snapshot.setUserId(account.getUserId());
        snapshot.setBrokerName(account.getBrokerName());
        snapshot.setServerName(account.getServerName());
        snapshot.setMt5Login(account.getMt5Login());
        snapshot.setAccountRole(account.getAccountRole());
        snapshot.setStatus(account.getStatus());
        return snapshot;
    }
}
