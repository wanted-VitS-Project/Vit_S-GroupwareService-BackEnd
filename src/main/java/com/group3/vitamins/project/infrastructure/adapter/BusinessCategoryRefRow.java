package com.group3.vitamins.project.infrastructure.adapter;

/**
 * {@code business_category} 조회 결과 — 이미 연결된 카테고리 표시용.
 *
 * <p>⚠️ 필드 순서 = XML SELECT 컬럼 순서. MyBatis 가 위치로 생성자에 꽂는다.
 *
 * @param deleted 카테고리가 논리 삭제됐는지 (DELETE.md D-6)
 */
public record BusinessCategoryRefRow(Long categoryId, String name, String code, boolean deleted) {
}
