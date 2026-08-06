package com.group3.vitamins.vitamate.analysis.infrastructure.catalog;

import com.group3.vitamins.vitamate.analysis.domain.repository.VitamateBlockRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.VitamateBlockEntity;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.VitamateBlockJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

// 비타메이트 블록 상세 행의 실제 JPA 저장 구현
@Repository
@RequiredArgsConstructor
public class CatalogVitamateBlockAdapter implements VitamateBlockRepository {

    private final VitamateBlockJpaRepository vitamateBlockJpaRepository;

    // IDENTITY 전략으로 INSERT 후 생성된 vitamate_block_id를 즉시 반환한다.
    @Override
    @Transactional
    public Long create(Long blockId) {
        return vitamateBlockJpaRepository.save(new VitamateBlockEntity(blockId)).getId();
    }

    // 상세 PK로 공통 block_id만 꺼낸다. Activity Log 이벤트의 기준 ID로 사용한다.
    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findBlockId(Long vitamateBlockId) {
        return vitamateBlockJpaRepository.findById(vitamateBlockId)
                .map(VitamateBlockEntity::getBlockId);
    }

    // deleted_at 조건을 UPDATE 자체에 걸어 중복 삭제를 멱등하게 처리한다.
    @Override
    @Transactional
    public boolean markDeleted(Long vitamateBlockId, LocalDateTime deletedAt) {
        return vitamateBlockJpaRepository.markDeletedIfActive(vitamateBlockId, deletedAt) > 0;
    }
}
