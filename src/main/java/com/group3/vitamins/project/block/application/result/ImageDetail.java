package com.group3.vitamins.project.block.application.result;

import java.time.LocalDateTime;

/**
 * IMAGE 블록 상세 — 블록 목록 카드 미리보기용으로 {@code order_index} 가 가장 작은 이미지 1장과
 * 그 블록에 속한 활성 이미지 총 개수를 담는다. 이미지가 하나도 없는 블록은 {@code firstImage} 가
 * null 이고 {@code totalCount} 는 0 이다.
 */
public record ImageDetail(Long imgBlockId, int totalCount, ImagePreview firstImage) implements BlockDetail {

    public record ImagePreview(Long imgId, String originalName, String imageUrl, String caption,
                                int orderIndex, LocalDateTime createdAt) {
    }
}
