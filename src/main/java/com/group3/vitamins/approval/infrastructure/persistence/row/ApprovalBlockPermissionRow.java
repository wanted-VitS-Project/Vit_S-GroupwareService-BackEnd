package com.group3.vitamins.approval.infrastructure.persistence.row;

import com.group3.vitamins.project.domain.model.MemberPermission;

/**
 * 블록이 속한 스텝에 대한 요청자의 권한 재료 1행 — 유효 권한 계산에 필요한 두 값을 한 번에 읽는다.
 *
 * <p>계산 규칙은 {@code StepAccessPolicy.resolve()} 와 같다: 프로젝트 권한이 {@code NONE}(미참여
 * 포함)이면 스텝 오버라이드를 보지 않고 {@code NONE}, 아니면 오버라이드 우선·없으면 프로젝트 권한 상속.
 * <b>판정 자체는 여기서 하지 않는다</b> — 행은 재료만 나르고 조합은 어댑터가 한다(XML 에 비즈니스
 * 로직을 넣지 않는다는 `MYBATIS.md` §8 규칙).
 *
 * @param projectPermission {@code project_member} 행이 없으면 {@code null} (= 미참여)
 * @param stepPermission    {@code step_permission} 오버라이드 행이 없으면 {@code null} (= 상속)
 */
public record ApprovalBlockPermissionRow(
        MemberPermission projectPermission,
        MemberPermission stepPermission
) {
}
