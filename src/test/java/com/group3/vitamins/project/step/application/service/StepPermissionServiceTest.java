package com.group3.vitamins.project.step.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.command.RevokeStepPermissionCommand;
import com.group3.vitamins.project.step.application.command.SetStepPermissionCommand;
import com.group3.vitamins.project.step.application.port.ProjectMemberLookupPort;
import com.group3.vitamins.project.step.application.query.StepPermissionListQuery;
import com.group3.vitamins.project.step.application.result.StepPermissionResult;
import com.group3.vitamins.project.step.application.result.StepPermissionSummary;
import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;
import com.group3.vitamins.project.step.domain.repository.StepPermissionRepository;
import com.group3.vitamins.project.step.domain.repository.StepRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/** 스텝 권한 오버라이드 3종 (STP-010 · STP-011). 판정 자체는 StepAccessPolicy 테스트 소관이다. */
@ExtendWith(MockitoExtension.class)
class StepPermissionServiceTest {

    @Mock private StepRepository stepRepository;
    @Mock private StepPermissionRepository stepPermissionRepository;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private ProjectMemberLookupPort projectMemberLookupPort;
    @Mock private ProjectAccessUseCase projectAccessUseCase;

    @InjectMocks private StepPermissionService stepPermissionService;

    private static final Long STEP_ID = 10L;
    private static final Long PROJECT_ID = 3L;
    private static final String REQUESTER = "E2024001";
    private static final String TARGET = "E2024007";

    @Test
    @DisplayName("오버라이드가 있으면 그 값이, 없으면 프로젝트 권한이 판정 결과다")
    void 목록_상속과_오버라이드() {
        givenStep();
        given(projectMemberLookupPort.findMembers(PROJECT_ID, REQUESTER, "USER")).willReturn(List.of(
                new ProjectMemberLookupPort.Member(REQUESTER, "김용준", MemberPermission.EDITOR),
                new ProjectMemberLookupPort.Member(TARGET, "김동훈", MemberPermission.EDITOR)));
        given(stepPermissionRepository.findAllByStepId(STEP_ID))
                .willReturn(Map.of(TARGET, MemberPermission.NONE));

        List<StepPermissionSummary> result = stepPermissionService.getPermissions(
                new StepPermissionListQuery(STEP_ID, REQUESTER, "USER"));

        assertThat(result).containsExactly(
                new StepPermissionSummary(REQUESTER, "김용준", "EDITOR", false),
                new StepPermissionSummary(TARGET, "김동훈", "NONE", true));
    }

    @Test
    @DisplayName("NONE 도 명시적으로 저장된다 — 행이 없는 것과 다른 의미다")
    void 부여_NONE() {
        givenStep();
        given(employeeLookupPort.findNameByUserId(TARGET)).willReturn("김동훈");

        StepPermissionResult result = stepPermissionService.setPermission(
                new SetStepPermissionCommand(STEP_ID, TARGET, "NONE", REQUESTER, "USER"));

        assertThat(result.permission()).isEqualTo("NONE");
        assertThat(result.overridden()).isTrue();
        Mockito.verify(stepPermissionRepository).save(
                eq(STEP_ID), eq(TARGET), eq(MemberPermission.NONE), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("자기 자신의 권한은 못 바꾼다 — 403")
    void 부여_자기자신() {
        givenStep();

        assertThatThrownBy(() -> stepPermissionService.setPermission(
                new SetStepPermissionCommand(STEP_ID, REQUESTER, "EDITOR", REQUESTER, "USER")))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verify(stepPermissionRepository, Mockito.never())
                .save(any(), anyString(), any(), any());
    }

    @Test
    @DisplayName("없는 권한 등급은 400 이다 — null 이어도 500 이 아니다")
    void 부여_등급_오타() {
        givenStep();

        assertThatThrownBy(() -> stepPermissionService.setPermission(
                new SetStepPermissionCommand(STEP_ID, TARGET, null, REQUESTER, "USER")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("없는 사원에게는 권한을 줄 수 없다 — 404")
    void 부여_사원_없음() {
        givenStep();
        given(employeeLookupPort.findNameByUserId(TARGET)).willReturn(null);

        assertThatThrownBy(() -> stepPermissionService.setPermission(
                new SetStepPermissionCommand(STEP_ID, TARGET, "VIEWER", REQUESTER, "USER")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("회수하면 프로젝트 권한 상속 등급을 돌려준다")
    void 회수() {
        givenStep();
        given(stepPermissionRepository.deleteByStepIdAndUserId(STEP_ID, TARGET)).willReturn(true);
        given(projectMemberLookupPort.findMember(PROJECT_ID, TARGET, REQUESTER, "USER"))
                .willReturn(Optional.of(new ProjectMemberLookupPort.Member(
                        TARGET, "김동훈", MemberPermission.VIEWER)));

        StepPermissionResult result = stepPermissionService.revokePermission(
                new RevokeStepPermissionCommand(STEP_ID, TARGET, REQUESTER, "USER"));

        assertThat(result.permission()).isEqualTo("VIEWER");
        assertThat(result.overridden()).isFalse();
    }

    @Test
    @DisplayName("자기 행은 회수도 못 한다 — 자기 NONE 을 지워 등급을 되찾는 우회로를 막는다 (INV-10)")
    void 회수_자기자신() {
        givenStep();

        assertThatThrownBy(() -> stepPermissionService.revokePermission(
                new RevokeStepPermissionCommand(STEP_ID, REQUESTER, REQUESTER, "USER")))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verify(stepPermissionRepository, Mockito.never())
                .deleteByStepIdAndUserId(any(), anyString());
    }

    @Test
    @DisplayName("지울 오버라이드 행이 없으면 404 다")
    void 회수_행_없음() {
        givenStep();
        given(stepPermissionRepository.deleteByStepIdAndUserId(STEP_ID, TARGET)).willReturn(false);

        assertThatThrownBy(() -> stepPermissionService.revokePermission(
                new RevokeStepPermissionCommand(STEP_ID, TARGET, REQUESTER, "USER")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("없는 스텝은 404 다")
    void 스텝_없음() {
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stepPermissionService.getPermissions(
                new StepPermissionListQuery(STEP_ID, REQUESTER, "USER")))
                .isInstanceOf(NotFoundException.class);

        Mockito.verifyNoInteractions(projectAccessUseCase);
    }

    private void givenStep() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        given(stepRepository.findById(STEP_ID)).willReturn(Optional.of(
                Step.restore(STEP_ID, PROJECT_ID, 7L, "제안서 작성", 1, 1, null, null, null,
                        StepStatus.NOT_STARTED, null, null, createdAt, createdAt, null)));
    }
}
