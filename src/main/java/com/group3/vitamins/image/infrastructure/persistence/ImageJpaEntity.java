package com.group3.vitamins.image.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "image")
public class ImageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "img_id")
    private Long imgId;

    // FK 아님(엔티티 매핑만 FK). 소속 이미지 블록(image_block.img_block_id) 참조값.
    @Column(name = "img_block_id", nullable = false)
    private Long imgBlockId;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    @Column(name = "extension", nullable = false)
    private String extension;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "caption", columnDefinition = "TEXT")
    private String caption;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // ⚠️ @Version(JPA)을 붙이지 않는다 — 캡션·순서 수정은 엔티티를 save/merge하지 않고
    // SpringDataImageRepository.updateCaptionAndOrderIfVersionMatches의 JPQL 벌크 UPDATE로만
    // 반영된다(WHERE i.version = :expectedVersion 검사 후 i.version = i.version + 1). 벌크 UPDATE는
    // JPA 엔티티 생명주기(dirty checking)를 안 타서 @Version 증가 로직 자체가 안 걸린다(CONCURRENCY.md §6-1).
    @Column(name = "version", nullable = false)
    private int version;

    /**
     * ⚠️ version을 명시적으로 1로 채운다 — Java int 필드 기본값 0을 그대로 두면 컬럼의
     * {@code DEFAULT 1}과 무관하게 INSERT 문에 0이 실린다(CONCURRENCY.md §3-1).
     */
    public ImageJpaEntity(Long imgBlockId, String originalName, String imageUrl, String extension,
                           long size, String caption, int orderIndex) {
        this.imgBlockId = imgBlockId;
        this.originalName = originalName;
        this.imageUrl = imageUrl;
        this.extension = extension;
        this.size = size;
        this.caption = caption;
        this.orderIndex = orderIndex;
        this.version = 1;
    }
}
