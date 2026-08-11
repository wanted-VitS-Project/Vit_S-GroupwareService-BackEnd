package com.group3.vitamins.checklist.infrastructure.catalog;

import com.group3.vitamins.checklist.domain.repository.ChecklistBlockRepository;
import com.group3.vitamins.checklist.infrastructure.persistence.ChecklistBlockJpaEntity;
import com.group3.vitamins.checklist.infrastructure.persistence.SpringDataChecklistBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 클래스 레벨에 {@code @Transactional(readOnly = true)} 를 두지 않는다 — {@link #existsActive}
 * 가 PESSIMISTIC_WRITE 락 조회라 쓰기 가능한 트랜잭션이 필요하고, 이 메서드는 항상 호출자
 * (서비스 계층)의 진행 중인 트랜잭션에 그대로 참여해야 락이 이어지는 INSERT 까지 유지된다.
 */
@Repository
@RequiredArgsConstructor
public class CatalogChecklistBlockAdapter implements ChecklistBlockRepository {

    private final SpringDataChecklistBlockRepository springDataChecklistBlockRepository;

    @Override
    @Transactional
    public Long create(Long blockId) {
        // IDENTITY 라 save() 시점에 INSERT 가 나가고 PK 가 채워져 돌아온다 — 되찾기 조회가 필요없다.
        return springDataChecklistBlockRepository
                .save(new ChecklistBlockJpaEntity(blockId)).getChkBlockId();
    }

    @Override
    @Transactional
    public boolean existsActive(Long chkBlockId) {
        return springDataChecklistBlockRepository.findActiveForUpdate(chkBlockId).isPresent();
    }

    @Override
    @Transactional
    public boolean markDeleted(Long chkBlockId, LocalDateTime deletedAt) {
        int updated = springDataChecklistBlockRepository.markDeletedIfActive(chkBlockId, deletedAt);
        return updated > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Long findBlockId(Long chkBlockId) {
        return springDataChecklistBlockRepository.findBlockIdByChkBlockId(chkBlockId).orElseThrow();
    }
}
