package com.zyc.copier_v0.modules.account.config.cache;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zyc.copier_v0.modules.account.config.domain.CopyRelationStatus;
import com.zyc.copier_v0.modules.account.config.entity.CopyRelationEntity;
import com.zyc.copier_v0.modules.account.config.entity.Mt5AccountEntity;
import com.zyc.copier_v0.modules.account.config.repository.CopyRelationRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CopyRouteCacheWarmupServiceTest {

    @Mock
    private CopyRelationRepository copyRelationRepository;

    @Mock
    private CopyRouteSnapshotReader snapshotReader;

    @Test
    void shouldWarmUpUniqueMastersAndFollowersFromActiveAndPausedRelations() {
        CopyRouteCacheWarmupService service = new CopyRouteCacheWarmupService(copyRelationRepository, snapshotReader);
        when(copyRelationRepository.findAllByStatusIn(org.mockito.ArgumentMatchers.anyCollection()))
                .thenReturn(List.of(
                        relation(1L, 2L, CopyRelationStatus.ACTIVE),
                        relation(1L, 3L, CopyRelationStatus.PAUSED),
                        relation(4L, 3L, CopyRelationStatus.ACTIVE)
                ));

        service.warmUpAll();

        verify(snapshotReader, times(1)).loadMasterRoute(1L);
        verify(snapshotReader, times(1)).loadMasterRoute(4L);
        verify(snapshotReader, times(1)).loadFollowerRisk(2L);
        verify(snapshotReader, times(1)).loadFollowerRisk(3L);
    }

    private CopyRelationEntity relation(Long masterAccountId, Long followerAccountId, CopyRelationStatus status) {
        Mt5AccountEntity master = new Mt5AccountEntity();
        ReflectionTestUtils.setField(master, "id", masterAccountId);
        Mt5AccountEntity follower = new Mt5AccountEntity();
        ReflectionTestUtils.setField(follower, "id", followerAccountId);

        CopyRelationEntity relation = new CopyRelationEntity();
        relation.setMasterAccount(master);
        relation.setFollowerAccount(follower);
        relation.setStatus(status);
        return relation;
    }
}
