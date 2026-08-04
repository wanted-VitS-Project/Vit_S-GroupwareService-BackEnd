package com.group3.vitamins.checklist.infrastructure.catalog;

import com.group3.vitamins.checklist.domain.repository.ChecklistBlockRepository;
import com.group3.vitamins.checklist.infrastructure.persistence.SpringDataChecklistBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogChecklistBlockAdapter implements ChecklistBlockRepository {

    private final SpringDataChecklistBlockRepository springDataChecklistBlockRepository;

    @Override
    public boolean existsActive(Long chkBlockId) {
        return springDataChecklistBlockRepository.existsByChkBlockIdAndDeletedAtIsNull(chkBlockId);
    }

    @Override
    @Transactional
    public boolean markDeleted(Long chkBlockId, LocalDateTime deletedAt) {
        int updated = springDataChecklistBlockRepository.markDeletedIfActive(chkBlockId, deletedAt);
        return updated > 0;
    }
}
