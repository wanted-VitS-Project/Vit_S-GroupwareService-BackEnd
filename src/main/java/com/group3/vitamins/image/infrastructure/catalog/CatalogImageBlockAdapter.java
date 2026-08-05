package com.group3.vitamins.image.infrastructure.catalog;

import com.group3.vitamins.image.domain.repository.ImageBlockRepository;
import com.group3.vitamins.image.infrastructure.persistence.ImageBlockJpaEntity;
import com.group3.vitamins.image.infrastructure.persistence.SpringDataImageBlockRepository;
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
public class CatalogImageBlockAdapter implements ImageBlockRepository {

    private final SpringDataImageBlockRepository springDataImageBlockRepository;

    @Override
    @Transactional
    public boolean existsActive(Long imgBlockId) {
        return springDataImageBlockRepository.findActiveForUpdate(imgBlockId).isPresent();
    }

    @Override
    public Long getBlockId(Long imgBlockId) {
        return springDataImageBlockRepository.findById(imgBlockId)
                .map(ImageBlockJpaEntity::getBlockId)
                .orElseThrow(() -> new IllegalStateException(
                        "image_block 행을 찾을 수 없습니다 — existsActive 통과 후에만 호출해야 합니다: " + imgBlockId));
    }

    @Override
    @Transactional
    public boolean markDeleted(Long imgBlockId, LocalDateTime deletedAt) {
        int updated = springDataImageBlockRepository.markDeletedIfActive(imgBlockId, deletedAt);
        return updated > 0;
    }
}
