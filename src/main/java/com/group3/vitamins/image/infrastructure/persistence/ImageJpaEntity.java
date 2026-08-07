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

    public ImageJpaEntity(Long imgBlockId, String originalName, String imageUrl, String extension,
                           long size, String caption, int orderIndex) {
        this.imgBlockId = imgBlockId;
        this.originalName = originalName;
        this.imageUrl = imageUrl;
        this.extension = extension;
        this.size = size;
        this.caption = caption;
        this.orderIndex = orderIndex;
    }
}
