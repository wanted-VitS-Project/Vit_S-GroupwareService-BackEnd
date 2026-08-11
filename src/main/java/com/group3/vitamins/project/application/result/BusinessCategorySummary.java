package com.group3.vitamins.project.application.result;

/**
 * @param deleted 이 카테고리가 삭제됐는지. 삭제됐어도 이름은 그대로 담는다 —
 *                지우면 "분류 없음" 과 구분이 안 된다 (DELETE.md D-6)
 */
public record BusinessCategorySummary(Long categoryId, String name, String code, boolean deleted) {
}
