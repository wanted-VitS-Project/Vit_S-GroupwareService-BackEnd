package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.command.UpdateProjectCommand;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.result.ProjectUpdateResult;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import com.group3.vitamins.project.domain.repository.ProjectBusinessCategoryRepository;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ProjectCommandServiceTest {

    @Mock private ProjectAccessUseCase projectAccessUseCase;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectBusinessCategoryRepository projectBusinessCategoryRepository;
    @Mock private BusinessCategoryLookupPort businessCategoryLookupPort;
    @Mock private EmployeeLookupPort employeeLookupPort;

    @InjectMocks private ProjectCommandService projectCommandService;

    private static final LocalDate STARTED = LocalDate.of(2026, 8, 1);
    private static final LocalDate ENDED = LocalDate.of(2026, 12, 31);

    @Test
    @DisplayName("받은 값으로 6필드를 전부 덮어쓴다")
    void 전체_덮어쓰기() {
        givenProject();

        ProjectUpdateResult result = projectCommandService.updateProject(command(
                "새 과업", "새 설명", "XX시청", STARTED, ENDED, new BigDecimal("135000000")));

        Project saved = captureSaved();
        assertThat(saved.getName()).isEqualTo("새 과업");
        assertThat(saved.getDescription()).isEqualTo("새 설명");
        assertThat(saved.getClientName()).isEqualTo("XX시청");
        assertThat(saved.getContractAmount()).isEqualByComparingTo("135000000");
        assertThat(result.updatedAt()).isNotNull();
    }

    @Test
    @DisplayName("null 로 온 필드는 비워진다 — 생략이 아니라 해제다")
    void null은_해제() {
        givenProject();

        projectCommandService.updateProject(
                command("새 과업", null, null, null, null, null));

        Project saved = captureSaved();
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getClientName()).isNull();
        assertThat(saved.getStartedOn()).isNull();
        assertThat(saved.getEndedOn()).isNull();
        assertThat(saved.getContractAmount()).isNull();
    }

    @Test
    @DisplayName("상태·종결사유는 수정으로 바뀌지 않는다")
    void 상태는_불변() {
        givenProject();

        projectCommandService.updateProject(
                command("새 과업", null, null, STARTED, ENDED, null));

        assertThat(captureSaved().getStatus()).isEqualTo(ProjectStatus.NOT_STARTED);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 400 이다")
    void 날짜_역전() {
        givenProject();

        assertThatThrownBy(() -> projectCommandService.updateProject(
                command("새 과업", null, null, ENDED, STARTED, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("시작일");
    }

    @Test
    @DisplayName("계약금액이 음수면 400 이다")
    void 금액_음수() {
        givenProject();

        assertThatThrownBy(() -> projectCommandService.updateProject(
                command("새 과업", null, null, null, null, new BigDecimal("-1"))))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("과업명이 없거나 300자를 넘으면 400 이다")
    void 과업명_검증() {
        givenProject();

        assertThatThrownBy(() -> projectCommandService.updateProject(
                command(null, null, null, null, null, null)))
                .isInstanceOf(ValidationException.class);

        assertThatThrownBy(() -> projectCommandService.updateProject(
                command("가".repeat(301), null, null, null, null, null)))
                .isInstanceOf(ValidationException.class);
    }

    private UpdateProjectCommand command(String name, String description, String clientName,
                                         LocalDate startedOn, LocalDate endedOn,
                                         BigDecimal contractAmount) {
        return new UpdateProjectCommand(12L, name, description, clientName,
                startedOn, endedOn, contractAmount, "E2024001", "USER");
    }

    /** 기존 프로젝트 한 건. 과업명 검증이 먼저 터지는 케이스는 save 까지 가지 않는다. */
    private void givenProject() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        Project existing = Project.restore(12L, null, "기존 과업", "기존 설명",
                ProjectStatus.NOT_STARTED, "OO시청", new BigDecimal("100000000"), STARTED, ENDED,
                null, null, "E2024001", createdAt, createdAt, null);

        given(projectRepository.findById(12L)).willReturn(Optional.of(existing));
        Mockito.lenient().when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Project captureSaved() {
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        Mockito.verify(projectRepository).save(captor.capture());
        return captor.getValue();
    }
}
