package com.group3.vitamins.major.application.result;

/**
 * 전공 목록 MyBatis 위치기반 매핑 레코드. ⚠️ 필드 순서 = XML SELECT alias 순서.
 * {@code deletable} 은 파생값이라 여기 없다(서비스가 {@link MajorListItemResult} 로 만든다).
 */
public record MajorListProjection(Long majorId, String name, int employeeCount) {
}
