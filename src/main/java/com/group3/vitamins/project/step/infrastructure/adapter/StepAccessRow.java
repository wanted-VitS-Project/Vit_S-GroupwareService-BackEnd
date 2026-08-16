package com.group3.vitamins.project.step.infrastructure.adapter;

/**
 * 스텝 접근 판정 재료 한 행 (평면). 조인 대상이 전부 UNIQUE 라 항상 1행이다.
 *
 * <p>⚠️ 필드 순서 = XML SELECT 컬럼 순서. MyBatis 가 위치로 생성자에 꽂는다 —
 * 중간에 끼워 넣으면 값이 한 칸씩 밀리는데 <b>컴파일도 되고 예외도 안 난다.</b>
 *
 * <p>권한 두 개를 {@code MemberPermission} 이 아니라 {@code String} 으로 받는 이유 —
 * 행이 없으면 {@code null} 이 와야 하는데, enum 으로 직접 받으면 그 {@code null} 이
 * {@code NONE} 과 섞일 여지가 생긴다. 변환은 어댑터가 한 곳에서만 한다
 * ({@code ProjectDetailRow} 와 같은 규칙).
 */
public record StepAccessRow(
        Long stepId,
        Long projectId,
        boolean projectVisible,
        String memberPermission,
        String overridePermission
) {
}
