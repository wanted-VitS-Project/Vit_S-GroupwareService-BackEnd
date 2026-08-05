package com.group3.vitamins.image.infrastructure.catalog;

import com.group3.vitamins.image.domain.model.ImageItem;
import com.group3.vitamins.image.domain.repository.ImageRepository;
import com.group3.vitamins.image.infrastructure.persistence.ImageJpaEntity;
import com.group3.vitamins.image.infrastructure.persistence.SpringDataImageRepository;
import lombok.RequiredArgsConstructor;
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
    public int updateCaptionAndOrder(Long imgId, Long imgBlockId, String caption, int orderIndex) {
        return springDataImageRepository.updateCaptionAndOrder(imgId, imgBlockId, caption, orderIndex);
    }

    @Override
    @Transactional
    public int markDeleted(Long imgId, Long imgBlockId, LocalDateTime deletedAt) {
        return springDataImageRepository.markDeleted(imgId, imgBlockId, deletedAt);
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
                entity.getDeletedAt()
        );
    }
}
