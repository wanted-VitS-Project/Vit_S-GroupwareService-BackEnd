package com.group3.vitamins.image.infrastructure.blockdetail;

import java.time.LocalDateTime;

/**
 * image 테이블 조회 행. 블록 하나당 {@code order_index} 최솟값 1행만 온다.
 * {@code imageUrl} 은 실제로는 S3 저장 키(원시값)라, 응답으로 쓰려면 presign 이 필요하다.
 * {@code totalCount} 는 그 블록에 속한 활성 이미지 총 개수(첫 이미지 포함).
 */
public record ImageDetailRow(Long imgBlockId, Long imgId, String originalName, String imageUrl,
                              String caption, int orderIndex, LocalDateTime createdAt, int totalCount) {
}
