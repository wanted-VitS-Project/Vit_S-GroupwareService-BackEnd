package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.command.ChangeProjectStatusCommand;
import com.group3.vitamins.project.application.command.CloseProjectCommand;
import com.group3.vitamins.project.application.command.UpdateProjectCommand;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.result.ProjectCloseResult;
import com.group3.vitamins.project.application.result.ProjectStatusResult;
import com.group3.vitamins.project.application.result.ProjectUpdateResult;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.CloseReasonCode;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import com.group3.vitamins.project.domain.repository.ProjectBusinessCategoryRepository;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * 과업명 필수·길이, 계약금액 음수 같은 <b>형식 검증은 요청 DTO 의 Bean Validation</b> 으로 옮겨서
 * 여기서 테스트하지 않는다. 이 파일은 서비스에 남은 규칙(날짜 관계 · 상태 전이 · 종결 사유)만 다룬다.
 */
@ExtendWith(MockitoExtension.class)
class ProjectCommandServiceTest {

    @Mock private ProjectAccessUseCase projectAccessUseCase;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectBusinessCategoryRepository projectBusinessCategoryRepository;
    @Mock private BusinessCategoryLookupPort businessCategoryLookupPort;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private CurrentCompanyIdProvider currentCompanyIdProvider;

    @InjectMocks private ProjectCommandService projectCommandService;

    private static final LocalDate STARTED = LocalDate.of(2026, 8, 1);
    private static final LocalDate ENDED = LocalDate.of(2026, 12, 31);
    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void 회사_컨텍스트() {
        Mockito.lenient().when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    @DisplayName("받은 값으로 6필드를 전부 덮어쓴다")
    void 전체_덮어쓰기() {
        givenProject();

        ProjectUpdateResult result = projectCommandService.updateProject(updateCommand(
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
                updateCommand("새 과업", null, null, null, null, null));

        Project saved = captureSaved();
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getClientName()).isNull();
        assertThat(saved.getStartedOn()).isNull();
        assertThat(saved.getEndedOn()).isNull();
        assertThat(saved.getContractAmount()).isNull();
    }

    @Test
    @DisplayName("수정으로 상태는 바뀌지 않는다")
    void 상태는_불변() {
        givenProject();

        projectCommandService.updateProject(
                updateCommand("새 과업", null, null, STARTED, ENDED, null));

        assertThat(captureSaved().getStatus()).isEqualTo(ProjectStatus.NOT_STARTED);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 400 이다 — 두 필드 관계라 애노테이션으로 못 막는다")
    void 날짜_역전() {
        givenProject();

        assertThatThrownBy(() -> projectCommandService.updateProject(
                updateCommand("새 과업", null, null, ENDED, STARTED, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("시작일");
    }

    @Test
    @DisplayName("상태를 바꾼다 — 역방향도 허용한다")
    void 상태_변경() {
        givenProject();

        ProjectStatusResult result = projectCommandService.changeStatus(
                new ChangeProjectStatusCommand(12L, "IN_PROGRESS", "E2024001", "USER"));

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(captureSaved().getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("상태 변경으로 CLOSED 를 설정할 수 없다 — 사유 없는 종결을 막는다")
    void 상태변경_CLOSED_거부() {
        givenProject();

        assertThatThrownBy(() -> projectCommandService.changeStatus(
                new ChangeProjectStatusCommand(12L, "CLOSED", "E2024001", "USER")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("정의되지 않은 상태 값은 400 이다")
    void 상태_오타() {
        givenProject();

        assertThatThrownBy(() -> projectCommandService.changeStatus(
                new ChangeProjectStatusCommand(12L, "DONE", "E2024001", "USER")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("COMPLETED 를 IN_PROGRESS 로 되돌릴 수 있다 — 역방향을 막지 않는다 (PRJ-003)")
    void 역방향_전이() {
        givenProject(ProjectStatus.COMPLETED, null, null, null);

        ProjectStatusResult result = projectCommandService.changeStatus(
                new ChangeProjectStatusCommand(12L, "IN_PROGRESS", "E2024001", "USER"));

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(captureSaved().getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("CLOSED 에서 벗어나면 종결 정보도 함께 지워진다 — 진행 중인데 종결 사유가 남으면 안 된다")
    void 종결_해제() {
        LocalDateTime closedAt = LocalDateTime.of(2026, 8, 5, 12, 0);
        givenProject(ProjectStatus.CLOSED, CloseReasonCode.NOT_SELECTED, "기술평가 2순위로 탈락", closedAt);

        projectCommandService.changeStatus(
                new ChangeProjectStatusCommand(12L, "IN_PROGRESS", "E2024001", "USER"));

        Project saved = captureSaved();
        assertThat(saved.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(saved.getCloseReasonCode()).isNull();
        assertThat(saved.getCloseReasonNote()).isNull();
        assertThat(saved.getClosedAt()).isNull();
    }

    @Test
    @DisplayName("사유를 붙여 종결한다 — deletedAt 은 건드리지 않는다")
    void 종결() {
        givenProject();

        ProjectCloseResult result = projectCommandService.closeProject(new CloseProjectCommand(
                12L, "NOT_SELECTED", "기술평가 2순위로 탈락", "E2024001", "USER"));

        Project saved = captureSaved();
        assertThat(result.status()).isEqualTo("CLOSED");
        assertThat(result.closedAt()).isNotNull();
        assertThat(saved.getCloseReasonCode()).isEqualTo(CloseReasonCode.NOT_SELECTED);
        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("사유 코드가 없으면 CLOSE_REASON_REQUIRED, 틀리면 CLOSE_REASON_INVALID 다")
    void 종결_사유_검증() {
        givenProject();

        assertThatThrownBy(() -> projectCommandService.closeProject(
                new CloseProjectCommand(12L, null, null, "E2024001", "USER")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("종결 사유를 선택");

        assertThatThrownBy(() -> projectCommandService.closeProject(
                new CloseProjectCommand(12L, "LOST", null, "E2024001", "USER")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("허용되지 않은");
    }

    @Test
    @DisplayName("편집 권한이 없으면 세 API 모두 저장까지 가지 않는다")
    void 권한_거부() {
        Mockito.doThrow(new ForbiddenException(ProjectErrorCode.PROJECT_EDIT_DENIED))
                .when(projectAccessUseCase).requireEditable(12L, "E2024001", "USER");

        assertThatThrownBy(() -> projectCommandService.updateProject(
                updateCommand("새 과업", null, null, STARTED, ENDED, null)))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> projectCommandService.changeStatus(
                new ChangeProjectStatusCommand(12L, "IN_PROGRESS", "E2024001", "USER")))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> projectCommandService.closeProject(new CloseProjectCommand(
                12L, "NOT_SELECTED", null, "E2024001", "USER")))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verifyNoInteractions(projectRepository);
    }

    private UpdateProjectCommand updateCommand(String name, String description, String clientName,
                                               LocalDate startedOn, LocalDate endedOn,
                                               BigDecimal contractAmount) {
        return new UpdateProjectCommand(12L, name, description, clientName,
                startedOn, endedOn, contractAmount, "E2024001", "USER");
    }

    /** 기존 프로젝트 한 건. 검증이 먼저 터지는 케이스는 save 까지 가지 않는다. */
    private void givenProject() {
        givenProject(ProjectStatus.NOT_STARTED, null, null, null);
    }

    /** 상태·종결 정보를 지정한 기존 프로젝트 한 건. */
    private void givenProject(ProjectStatus status, CloseReasonCode closeReasonCode,
                              String closeReasonNote, LocalDateTime closedAt) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        Project existing = Project.restore(12L, COMPANY_ID, null, "기존 과업", "기존 설명",
                status, "OO시청", new BigDecimal("100000000"), STARTED, ENDED,
                closeReasonCode, closeReasonNote, closedAt, "E2024001", createdAt, createdAt, null);

        given(projectRepository.findById(12L, COMPANY_ID)).willReturn(Optional.of(existing));
        Mockito.lenient().when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Project captureSaved() {
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        Mockito.verify(projectRepository).save(captor.capture());
        return captor.getValue();
    }
}
