package com.group3.vitamins.project.step.application.policy;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StepAccessPolicy 스텝 권한 판정 (회사 격리 포함)")
class StepAccessPolicyTest {

    private final StepAccessPolicy policy = new StepAccessPolicy();

    @Nested
    @DisplayName("회사 경계 — projectPermission 이 NONE 이면 role 승격도 막는다")
    class TenantBoundary {

        // 회귀 방어: ProjectAccessService 는 타 회사 프로젝트에 NONE 을 준다. 과거 버그는 role(MASTER/ADMIN)을
        // 먼저 판정해 이 NONE 을 무시하고 EDITOR 를 돌려줬다. MASTER 는 각 회사 ADMIN 이 자기 직원에게
        // 발급 가능한 role 이라, 이 경로가 열리면 일반 고객이 타 회사 콘텐츠에 도달한다.
        @Test
        @DisplayName("타 회사 MASTER (projectPermission=NONE) → NONE")
        void masterOnForeignProjectIsNone() {
            assertThat(policy.resolve("MASTER", MemberPermission.NONE, null))
                    .isEqualTo(MemberPermission.NONE);
        }

        @Test
        @DisplayName("타 회사 ADMIN (projectPermission=NONE) → NONE")
        void adminOnForeignProjectIsNone() {
            assertThat(policy.resolve("ADMIN", MemberPermission.NONE, null))
                    .isEqualTo(MemberPermission.NONE);
        }

        @Test
        @DisplayName("스텝 오버라이드가 있어도 NONE 이면 승격하지 않는다")
        void foreignMasterWithOverrideStaysNone() {
            assertThat(policy.resolve("MASTER", MemberPermission.NONE, MemberPermission.EDITOR))
                    .isEqualTo(MemberPermission.NONE);
        }

        @Test
        @DisplayName("projectPermission 이 null 이면 NONE")
        void nullProjectPermissionIsNone() {
            assertThat(policy.resolve("MEMBER", null, null))
                    .isEqualTo(MemberPermission.NONE);
        }

        @Test
        @DisplayName("타 회사 MASTER 상세 조회 → STEP_ACCESS_DENIED")
        void requireAccessForeignMasterDenied() {
            assertThatThrownBy(() -> policy.requireAccess("MASTER", MemberPermission.NONE, null))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(e -> ((ForbiddenException) e).getErrorCode())
                    .isEqualTo(StepErrorCode.STEP_ACCESS_DENIED);
        }

        @Test
        @DisplayName("타 회사 MASTER 편집 → STEP_ACCESS_DENIED (접근이 먼저 막힌다)")
        void requireEditableForeignMasterDenied() {
            assertThatThrownBy(() -> policy.requireEditable("MASTER", MemberPermission.NONE, null))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(e -> ((ForbiddenException) e).getErrorCode())
                    .isEqualTo(StepErrorCode.STEP_ACCESS_DENIED);
        }
    }

    @Nested
    @DisplayName("회사 경계 안 — 기존 판정은 그대로 유지된다")
    class WithinCompany {

        // 같은 회사 MASTER 는 ProjectAccessService 가 항상 EDITOR 로 계산해 넘긴다(참여자 행이 없어도).
        // 따라서 NONE 가드에 걸리지 않고, 스텝 오버라이드까지 무시하고 EDITOR 로 본다.
        @Test
        @DisplayName("같은 회사 MASTER (projectPermission=EDITOR) → EDITOR, 오버라이드 무시")
        void masterWithinCompanyIsEditor() {
            assertThat(policy.resolve("MASTER", MemberPermission.EDITOR, MemberPermission.VIEWER))
                    .isEqualTo(MemberPermission.EDITOR);
        }

        @Test
        @DisplayName("일반 EDITOR, 오버라이드 없음 → EDITOR")
        void memberEditorNoOverride() {
            assertThat(policy.resolve("MEMBER", MemberPermission.EDITOR, null))
                    .isEqualTo(MemberPermission.EDITOR);
        }

        @Test
        @DisplayName("일반 EDITOR, 스텝 오버라이드 VIEWER → VIEWER (오버라이드 우선)")
        void memberOverrideWins() {
            assertThat(policy.resolve("MEMBER", MemberPermission.EDITOR, MemberPermission.VIEWER))
                    .isEqualTo(MemberPermission.VIEWER);
        }

        @Test
        @DisplayName("같은 회사 편집 요청 EDITOR → 통과")
        void requireEditableEditorPasses() {
            assertThat(policy.requireEditable("MEMBER", MemberPermission.EDITOR, null))
                    .isEqualTo(MemberPermission.EDITOR);
        }

        @Test
        @DisplayName("VIEWER 가 편집을 요청하면 STEP_EDIT_DENIED")
        void viewerCannotEdit() {
            assertThatThrownBy(() -> policy.requireEditable("MEMBER", MemberPermission.VIEWER, null))
                    .isInstanceOf(ForbiddenException.class)
                    .extracting(e -> ((ForbiddenException) e).getErrorCode())
                    .isEqualTo(StepErrorCode.STEP_EDIT_DENIED);
        }
    }
}
