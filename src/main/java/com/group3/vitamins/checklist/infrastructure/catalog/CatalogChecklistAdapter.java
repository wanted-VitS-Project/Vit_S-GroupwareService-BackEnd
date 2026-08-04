package com.group3.vitamins.checklist.infrastructure.catalog;

import com.group3.vitamins.checklist.domain.exception.ChecklistErrorCode;
import com.group3.vitamins.checklist.domain.model.ChecklistItem;
import com.group3.vitamins.checklist.domain.repository.ChecklistRepository;
import com.group3.vitamins.checklist.infrastructure.persistence.ChecklistJpaEntity;
import com.group3.vitamins.checklist.infrastructure.persistence.SpringDataChecklistRepository;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * checklist_block 행 생성·삭제는 Block 도메인(동훈님) 쪽에서 처리한다 — 여기서는 그 블록에 속한
 * 항목(checklist)의 생성·조회·수정·삭제만 다룬다.
 *
 * <p>updateFields/markDeleted 를 분리한 이유는 텍스트 도메인과 동일 — 하나의 save() 가 여러 컬럼을
 * 한꺼번에 덮어쓰면 오래전에 읽어둔 deletedAt(=null) 을 그대로 다시 써서 동시에 삭제된 행을
 * 되살릴 수 있다. 각 메서드는 자기 컬럼만 조회 직전에 새로 읽어 갱신한다.
 */
@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CatalogChecklistAdapter implements ChecklistRepository {

    private final SpringDataChecklistRepository springDataChecklistRepository;

    @Override
    @Transactional
    public ChecklistItem create(Long chkBlockId, String content) {
        ChecklistJpaEntity saved = springDataChecklistRepository.save(new ChecklistJpaEntity(chkBlockId, content));
        return toDomain(saved);
    }

    @Override
    @Transactional
    public ChecklistItem updateFields(Long chkId, String content, Boolean completed) {
        if (content != null) {
            int updated = springDataChecklistRepository.updateContentIfActive(chkId, content);
            if (updated == 0) {
                throw new NotFoundException(ChecklistErrorCode.ITEM_NOT_FOUND);
            }
        }
        if (completed != null) {
            int updated = springDataChecklistRepository.updateCompletionIfActive(chkId, completed);
            if (updated == 0) {
                throw new NotFoundException(ChecklistErrorCode.ITEM_NOT_FOUND);
            }
        }

        ChecklistJpaEntity entity = springDataChecklistRepository.findById(chkId)
                .orElseThrow(() -> new IllegalStateException("checklist item not found after update: " + chkId));
        return toDomain(entity);
    }

    @Override
    @Transactional
    public boolean markDeleted(Long chkId, LocalDateTime deletedAt) {
        int updated = springDataChecklistRepository.markDeletedIfActive(chkId, deletedAt);
        return updated > 0;
    }

    @Override
    @Transactional
    public int markAllDeletedByBlock(Long chkBlockId, LocalDateTime deletedAt) {
        return springDataChecklistRepository.markAllDeletedByBlockIfActive(chkBlockId, deletedAt);
    }

    @Override
    public Optional<ChecklistItem> findActiveByChkId(Long chkId) {
        return springDataChecklistRepository.findById(chkId)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public int countActiveItems(Long chkBlockId) {
        return (int) springDataChecklistRepository.countByChkBlockIdAndDeletedAtIsNull(chkBlockId);
    }

    @Override
    public int countCompletedActiveItems(Long chkBlockId) {
        return (int) springDataChecklistRepository.countByChkBlockIdAndCompletedTrueAndDeletedAtIsNull(chkBlockId);
    }

    private ChecklistItem toDomain(ChecklistJpaEntity entity) {
        return ChecklistItem.reconstruct(
                entity.getChkId(),
                entity.getChkBlockId(),
                entity.getContent(),
                entity.isCompleted(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
