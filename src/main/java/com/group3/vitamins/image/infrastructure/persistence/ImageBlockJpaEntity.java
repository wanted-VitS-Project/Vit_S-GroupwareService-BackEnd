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

import java.time.LocalDateTime;

/**
 * 이미지 블록 상세 행({@code image_block}) — 블록 생성 시 이 도메인이 빈 행을 만들고
 * (텍스트·체크리스트와 동일 패턴), 삭제는 Block 도메인이 발행하는 삭제 이벤트를 받아 처리한다.
 */
@Entity
@NoArgsConstructor
@Getter
@Table(name = "image_block")
public class ImageBlockJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "img_block_id")
    private Long imgBlockId;

    // FK 아님. 공용 block 테이블 참조용 값만 저장 (동훈님 쪽에서 채워줌)
    @Column(name = "block_id", nullable = false)
    private Long blockId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 상세 빈 행 생성용. 항목(image)은 0개로 시작한다. */
    public ImageBlockJpaEntity(Long blockId) {
        this.blockId = blockId;
    }
}
