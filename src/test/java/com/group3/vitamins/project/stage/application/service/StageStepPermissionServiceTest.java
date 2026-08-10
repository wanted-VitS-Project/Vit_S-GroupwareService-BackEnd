package com.group3.vitamins.project.stage.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.stage.application.command.ApplyStagePermissionCommand;
import com.group3.vitamins.project.stage.application.port.StepPermissionBulkPort;
import com.group3.vitamins.project.stage.application.result.StageStepPermissionResult;
import com.group3.vitamins.project.stage.domain.model.Stage;
import com.group3.vitamins.project.stage.domain.repository.StagePermissionDefaultRepository;
import com.group3.vitamins.project.stage.domain.repository.StageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/** 하위 스텝 권한 일괄 적용 (STG-004). 기본값 저장은 항상, 기존 스텝 전개는 옵션이다. */
@ExtendWith(MockitoExtension.class)
class StageStepPermissionServiceTest {

    @Mock private StageRepository stageRepository;
    @Mock private StagePermissionDefaultRepository stagePermissionDefaultRepository;
    @Mock private StepPermissionBulkPort stepPermissionBulkPort;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private ProjectAccessUseCase projectAccessUseCase;

    @InjectMocks private StageStepPermissionService stageStepPermissionService;

    private static final Long STAGE_ID = 7L;
    private static final Long PROJECT_ID = 3L;
    private static final String REQUESTER = "E2024001";
    private static final String TARGET = "E2024007";

    @Test
    @DisplayName("기본값을 저장하고 기존 하위 스텝에도 적용한다")
    void 적용() {
        givenStage();
        given(employeeLookupPort.findNameByUserId(TARGET)).willReturn("김동훈");
        given(stepPermissionBulkPort.applyToStage(STAGE_ID, TARGET, MemberPermission.EDITOR))
                .willReturn(3);

        StageStepPermissionResult result = stageStepPermissionService.apply(
                command("EDITOR", true));

        assertThat(result.permission()).isEqualTo("EDITOR");
        assertThat(result.appliedStepCount()).isEqualTo(3);
        Mockito.verify(stagePermissionDefaultRepository).save(
                eq(STAGE_ID), eq(TARGET), eq(MemberPermission.EDITOR), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("applyToExistingSteps=false 면 기본값만 저장하고 기존 스텝은 안 건드린다")
    void 기본값만_저장() {
        givenStage();
        given(employeeLookupPort.findNameByUserId(TARGET)).willReturn("김동훈");

        StageStepPermissionResult result = stageStepPermissionService.apply(
                command("VIEWER", false));

        assertThat(result.appliedStepCount()).isZero();
        Mockito.verify(stagePermissionDefaultRepository).save(
                eq(STAGE_ID), eq(TARGET), eq(MemberPermission.VIEWER), any(LocalDateTime.class));
        Mockito.verifyNoInteractions(stepPermissionBulkPort);
    }

    @Test
    @DisplayName("자기 자신의 권한은 일괄로도 못 바꾼다 — 403")
    void 자기자신() {
        givenStage();

        assertThatThrownBy(() -> stageStepPermissionService.apply(
                new ApplyStagePermissionCommand(
                        STAGE_ID, REQUESTER, "EDITOR", true, REQUESTER, "USER")))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verifyNoInteractions(stagePermissionDefaultRepository, stepPermissionBulkPort);
    }

    @Test
    @DisplayName("없는 권한 등급은 400 이다 — null 이어도 500 이 아니다")
    void 등급_오타() {
        givenStage();

        assertThatThrownBy(() -> stageStepPermissionService.apply(command(null, true)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("없는 사원에게는 못 준다 — 404")
    void 사원_없음() {
        givenStage();
        given(employeeLookupPort.findNameByUserId(TARGET)).willReturn(null);

        assertThatThrownBy(() -> stageStepPermissionService.apply(command("EDITOR", true)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("없는 스테이지는 404 다 — 권한 판정까지 가지 않는다")
    void 스테이지_없음() {
        given(stageRepository.findById(STAGE_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stageStepPermissionService.apply(command("EDITOR", true)))
                .isInstanceOf(NotFoundException.class);

        Mockito.verifyNoInteractions(projectAccessUseCase);
    }

    private ApplyStagePermissionCommand command(String permission, boolean applyToExistingSteps) {
        return new ApplyStagePermissionCommand(
                STAGE_ID, TARGET, permission, applyToExistingSteps, REQUESTER, "USER");
    }

    private void givenStage() {
        given(stageRepository.findById(STAGE_ID)).willReturn(Optional.of(
                Stage.restore(STAGE_ID, PROJECT_ID, "요구분석", 1,
                        LocalDateTime.of(2026, 8, 1, 9, 0), null)));
    }
}
