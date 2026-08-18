package com.group3.vitamins.image.infrastructure.catalog;

import com.group3.vitamins.image.domain.model.ImageItem;
import com.group3.vitamins.image.domain.repository.ImageRepository;
import com.group3.vitamins.image.infrastructure.persistence.ImageJpaEntity;
import com.group3.vitamins.image.infrastructure.persistence.SpringDataImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogImageAdapter implements ImageRepository {

    private final SpringDataImageRepository springDataImageRepository;

    @Override
    @Transactional
    public List<ImageItem> createAll(List<ImageItem> items) {
        List<ImageJpaEntity> entities = items.stream()
                .map(item -> new ImageJpaEntity(
                        item.getImgBlockId(),
                        item.getOriginalName(),
                        item.getImageUrl(),
                        item.getExtension(),
                        item.getSize(),
                        item.getCaption(),
                        item.getOrderIndex()
                ))
                .toList();

        return springDataImageRepository.saveAll(entities).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public int findMaxOrderIndex(Long imgBlockId) {
        return springDataImageRepository.findMaxOrderIndex(imgBlockId);
    }

    @Override
    public List<ImageItem> findAllActiveByImgBlockId(Long imgBlockId) {
        return springDataImageRepository.findAllActiveByImgBlockId(imgBlockId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public int updateCaptionAndOrderIfVersionMatches(Long imgId, Long imgBlockId, String caption, int orderIndex,
                                                      int expectedVersion) {
        return springDataImageRepository.updateCaptionAndOrderIfVersionMatches(
                imgId, imgBlockId, caption, orderIndex, expectedVersion);
    }

    @Override
    @Transactional
    public int touchVersionIfMatches(Long imgId, Long imgBlockId, int expectedVersion) {
        return springDataImageRepository.touchVersionIfMatches(imgId, imgBlockId, expectedVersion);
    }

    @Override
    @Transactional
    public int markDeleted(Long imgId, Long imgBlockId, LocalDateTime deletedAt) {
        return springDataImageRepository.markDeleted(imgId, imgBlockId, deletedAt);
    }

    @Override
    @Transactional
    public int decrementOrderIndexAfter(Long imgBlockId, int orderIndex) {
        return springDataImageRepository.decrementOrderIndexAfter(imgBlockId, orderIndex);
    }

    @Override
    public Optional<ImageItem> findActiveByImgId(Long imgId) {
        return springDataImageRepository.findById(imgId)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public int markAllDeletedByBlock(Long imgBlockId, LocalDateTime deletedAt) {
        return springDataImageRepository.markAllDeletedByBlockIfActive(imgBlockId, deletedAt);
    }

    @Override
    public Optional<ImageItem> findNextByOrderIndex(Long imgBlockId, int orderIndex) {
        return firstOf(springDataImageRepository.findActiveAfterOrderIndex(imgBlockId, orderIndex, TOP_1));
    }

    @Override
    public Optional<ImageItem> findPreviousByOrderIndex(Long imgBlockId, int orderIndex) {
        return firstOf(springDataImageRepository.findActiveBeforeOrderIndex(imgBlockId, orderIndex, TOP_1));
    }

    @Override
    public Optional<ImageItem> findFirstActive(Long imgBlockId) {
        return firstOf(springDataImageRepository.findActiveOrderByOrderIndexAsc(imgBlockId, TOP_1));
    }

    @Override
    public Optional<ImageItem> findLastActive(Long imgBlockId) {
        return firstOf(springDataImageRepository.findActiveOrderByOrderIndexDesc(imgBlockId, TOP_1));
    }

    @Override
    public int countActive(Long imgBlockId) {
        return springDataImageRepository.countActiveByImgBlockId(imgBlockId);
    }

    @Override
    public boolean existsActiveByOrderIndex(Long imgBlockId, int orderIndex) {
        return springDataImageRepository.existsActiveByOrderIndex(imgBlockId, orderIndex);
    }

    @Override
    public List<ImageItem> findAllByImgIds(List<Long> imgIds) {
        return springDataImageRepository.findAllByImgIdIn(imgIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public int restore(Long imgId, Long imgBlockId, int orderIndex) {
        return springDataImageRepository.restore(imgId, imgBlockId, orderIndex);
    }

    @Override
    @Transactional
    public int hardDeleteAll(List<Long> imgIds) {
        return springDataImageRepository.hardDeleteAll(imgIds);
    }

    private static final Pageable TOP_1 = PageRequest.of(0, 1);

    private Optional<ImageItem> firstOf(List<ImageJpaEntity> entities) {
        return entities.stream().findFirst().map(this::toDomain);
    }

    private ImageItem toDomain(ImageJpaEntity entity) {
        return ImageItem.reconstruct(
                entity.getImgId(),
                entity.getImgBlockId(),
                entity.getOriginalName(),
                entity.getImageUrl(),
                entity.getExtension(),
                entity.getSize(),
                entity.getCaption(),
                entity.getOrderIndex(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt(),
                entity.getVersion()
        );
    }
}
