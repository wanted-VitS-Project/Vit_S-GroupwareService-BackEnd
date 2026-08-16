package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.policy.ProjectAccessPolicy;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.policy.StepAccessPolicy;
import com.group3.vitamins.project.step.application.port.StepAccessQueryPort;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.project.step.domain.exception.StepErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * 4회 조회를 1회로 합친 뒤에도 404/403 경계가 그대로인지 지킨다.
 *
 * <p>정책 2개는 <b>목이 아니라 실물</b>을 쓴다 — 이 클래스가 하는 일이 "포트가 준 원시값을
 * 두 정책에 올바른 순서로 넘기는 것" 뿐이라, 정책을 목으로 바꾸면 검증할 게 남지 않는다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StepAccessService 스텝 접근 판정 (단일 쿼리)")
class StepAccessServiceTest {

    private static final Long STEP_ID = 10L;
    private static final Long PROJECT_ID = 3L;
    private static final Long COMPANY_ID = 1L;
    private static final String REQUESTER = "E2024001";

    @Mock private StepAccessQueryPort stepAccessQueryPort;
    @Mock private CurrentCompanyIdProvider currentCompanyIdProvider;

    private StepAccessService stepAccessService;

    @BeforeEach
    void setUp() {
        stepAccessService = new StepAccessService(
                stepAccessQueryPort, new ProjectAccessPolicy(), new StepAccessPolicy(),
                currentCompanyIdProvider);
        given(currentCompanyIdProvider.currentCompanyId()).willReturn(COMPANY_ID);
    }

    @Test
    @DisplayName("스텝이 없으면 404 — 조회는 한 번만 나간다")
    void 스텝_없음_404() {
        givenSnapshot(null);

        assertThatThrownBy(() -> stepAccessService.requireAccess(STEP_ID, REQUESTER, "MEMBER"))
                .isInstanceOf(NotFoundException.class)
                .extracting(e -> ((NotFoundException) e).getErrorCode())
                .isEqualTo(StepErrorCode.STEP_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 회사 프로젝트의 스텝은 404 가 아니라 403 이다")
    void 타회사_403() {
        givenSnapshot(snapshot(false, MemberPermission.EDITOR, null));

        assertThatThrownBy(() -> stepAccessService.requireAccess(STEP_ID, REQUESTER, "MEMBER"))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getErrorCode())
                .isEqualTo(StepErrorCode.STEP_ACCESS_DENIED);
    }

    @Test
    @DisplayName("프로젝트가 안 보이면 MASTER 도 통과하지 못한다")
    void 타회사_마스터도_403() {
        givenSnapshot(snapshot(false, null, null));

        assertThatThrownBy(() -> stepAccessService.requireAccess(STEP_ID, REQUESTER, "MASTER"))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getErrorCode())
                .isEqualTo(StepErrorCode.STEP_ACCESS_DENIED);
    }

    @Test
    @DisplayName("참여자 행이 없으면 403 (프로젝트는 보이지만 권한이 없다)")
    void 미참여_403() {
        givenSnapshot(snapshot(true, null, null));

        assertThatThrownBy(() -> stepAccessService.requireAccess(STEP_ID, REQUESTER, "MEMBER"))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getErrorCode())
                .isEqualTo(StepErrorCode.STEP_ACCESS_DENIED);
    }

    @Test
    @DisplayName("오버라이드가 없으면 프로젝트 권한을 상속한다")
    void 오버라이드_없으면_상속() {
        givenSnapshot(snapshot(true, MemberPermission.EDITOR, null));

        StepAccessUseCase.StepAccessView view =
                stepAccessService.requireAccess(STEP_ID, REQUESTER, "MEMBER");

        assertThat(view.permission()).isEqualTo(MemberPermission.EDITOR);
        assertThat(view.stepId()).isEqualTo(STEP_ID);
        assertThat(view.projectId()).isEqualTo(PROJECT_ID);
    }

    @Test
    @DisplayName("오버라이드 NONE 은 프로젝트 EDITOR 를 덮어 차단한다 — null 과 뭉개면 안 된다")
    void 오버라이드_NONE_차단() {
        givenSnapshot(snapshot(true, MemberPermission.EDITOR, MemberPermission.NONE));

        assertThatThrownBy(() -> stepAccessService.requireAccess(STEP_ID, REQUESTER, "MEMBER"))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getErrorCode())
                .isEqualTo(StepErrorCode.STEP_ACCESS_DENIED);
    }

    @Test
    @DisplayName("오버라이드 VIEWER 는 프로젝트 EDITOR 를 덮는다")
    void 오버라이드_VIEWER_우선() {
        givenSnapshot(snapshot(true, MemberPermission.EDITOR, MemberPermission.VIEWER));

        StepAccessUseCase.StepAccessView view =
                stepAccessService.requireAccess(STEP_ID, REQUESTER, "MEMBER");

        assertThat(view.permission()).isEqualTo(MemberPermission.VIEWER);
    }

    @Test
    @DisplayName("MASTER 는 참여자가 아니어도, 오버라이드가 있어도 EDITOR 다")
    void 마스터_승격() {
        givenSnapshot(snapshot(true, null, MemberPermission.VIEWER));

        StepAccessUseCase.StepAccessView view =
                stepAccessService.requireAccess(STEP_ID, REQUESTER, "MASTER");

        assertThat(view.permission()).isEqualTo(MemberPermission.EDITOR);
    }

    @Test
    @DisplayName("requireEditable 은 VIEWER 를 EDIT_DENIED 로 막는다")
    void 편집_VIEWER_거부() {
        givenSnapshot(snapshot(true, MemberPermission.VIEWER, null));

        assertThatThrownBy(() -> stepAccessService.requireEditable(STEP_ID, REQUESTER, "MEMBER"))
                .isInstanceOf(ForbiddenException.class)
                .extracting(e -> ((ForbiddenException) e).getErrorCode())
                .isEqualTo(StepErrorCode.STEP_EDIT_DENIED);
    }

    @Test
    @DisplayName("requireEditable 은 EDITOR 를 통과시킨다")
    void 편집_EDITOR_통과() {
        givenSnapshot(snapshot(true, MemberPermission.EDITOR, null));

        StepAccessUseCase.StepAccessView view =
                stepAccessService.requireEditable(STEP_ID, REQUESTER, "MEMBER");

        assertThat(view.permission()).isEqualTo(MemberPermission.EDITOR);
    }

    private void givenSnapshot(StepAccessQueryPort.StepAccessSnapshot snapshot) {
        given(stepAccessQueryPort.findAccess(STEP_ID, REQUESTER, COMPANY_ID))
                .willReturn(Optional.ofNullable(snapshot));
    }

    private StepAccessQueryPort.StepAccessSnapshot snapshot(boolean projectVisible,
                                                            MemberPermission member,
                                                            MemberPermission override) {
        return new StepAccessQueryPort.StepAccessSnapshot(
                STEP_ID, PROJECT_ID, projectVisible, member, override);
    }
}
