package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * 과업명 필수·길이, 계약금액 음수 같은 <b>형식 검증은 요청 DTO 의 Bean Validation</b> 으로 옮겨서
 * 여기서 테스트하지 않는다. 이 파일은 서비스에 남은 규칙(날짜 관계 · 상태 전이 · 종결 사유 · 낙관락)만 다룬다.
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
    private static final Long PROJECT_ID = 12L;
    private static final int VERSION = 1;

    @BeforeEach
    void 회사_컨텍스트() {
        Mockito.lenient().when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    // ────────────────────────────── 수정 ──────────────────────────────

    @Test
    @DisplayName("받은 값으로 6필드를 전부 덮어쓴다 — 응답 version 은 +1 된다")
    void 전체_덮어쓰기() {
        givenProject();

        ProjectUpdateResult result = projectCommandService.updateProject(updateCommand(
                "새 과업", "새 설명", "XX시청", STARTED, ENDED, new BigDecimal("135000000")));

        Updated updated = captureUpdated();
        assertThat(updated.name()).isEqualTo("새 과업");
        assertThat(updated.description()).isEqualTo("새 설명");
        assertThat(updated.clientName()).isEqualTo("XX시청");
        assertThat(updated.contractAmount()).isEqualByComparingTo("135000000");
        assertThat(updated.expectedVersion()).isEqualTo(VERSION);
        assertThat(result.updatedAt()).isNotNull();
        assertThat(result.version()).isEqualTo(VERSION + 1);
    }

    @Test
    @DisplayName("null 로 온 필드는 비워진다 — 생략이 아니라 해제다")
    void null은_해제() {
        givenProject();

        projectCommandService.updateProject(
                updateCommand("새 과업", null, null, null, null, null));

        Updated updated = captureUpdated();
        assertThat(updated.description()).isNull();
        assertThat(updated.clientName()).isNull();
        assertThat(updated.startedOn()).isNull();
        assertThat(updated.endedOn()).isNull();
        assertThat(updated.contractAmount()).isNull();
    }

    @Test
    @DisplayName("수정은 상태를 건드리지 않는다 — 상태 전용 UPDATE 를 부르지 않는다")
    void 상태는_불변() {
        givenProject();

        projectCommandService.updateProject(
                updateCommand("새 과업", null, null, STARTED, ENDED, null));

        Mockito.verify(projectRepository, Mockito.never()).changeStatusIfVersionMatches(
                anyLong(), anyLong(), any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    @DisplayName("내가 본 뒤 남이 먼저 저장했으면 409 다 — 조건부 UPDATE 가 0행을 돌려준다")
    void 수정_버전_충돌() {
        givenProjectFound(ProjectStatus.NOT_STARTED, null, null, null, VERSION);
        given(projectRepository.updateIfVersionMatches(
                eq(PROJECT_ID), eq(COMPANY_ID), anyString(), any(), any(),
                any(), any(), any(), any(), eq(VERSION)))
                .willReturn(0);

        assertThatThrownBy(() -> projectCommandService.updateProject(
                updateCommand("새 과업", null, null, STARTED, ENDED, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("덮어쓰기는 DB 현재 버전을 조건으로 써서 통과한다 — 요청이 든 낡은 버전은 무시한다")
    void 수정_덮어쓰기() {
        // DB 는 이미 v5 인데 요청은 v1 을 들고 왔다 (= 그냥 저장하면 409 나는 상황)
        givenProjectFound(ProjectStatus.NOT_STARTED, null, null, null, 5);
        given(projectRepository.updateIfVersionMatches(
                eq(PROJECT_ID), eq(COMPANY_ID), anyString(), any(), any(),
                any(), any(), any(), any(), eq(5)))
                .willReturn(1);

        ProjectUpdateResult result = projectCommandService.updateProject(
                new UpdateProjectCommand(PROJECT_ID, "새 과업", null, null, STARTED, ENDED, null,
                        VERSION, true, "E2024001", "USER"));

        assertThat(result.version()).isEqualTo(6);
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 400 이다 — 두 필드 관계라 애노테이션으로 못 막는다")
    void 날짜_역전() {
        givenProjectFound(ProjectStatus.NOT_STARTED, null, null, null, VERSION);

        assertThatThrownBy(() -> projectCommandService.updateProject(
                updateCommand("새 과업", null, null, ENDED, STARTED, null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("시작일");
    }

    // ────────────────────────────── 상태 변경 ──────────────────────────────

    @Test
    @DisplayName("상태를 바꾼다 — 역방향도 허용하고 version 은 +1 된다")
    void 상태_변경() {
        givenProject();

        ProjectStatusResult result = projectCommandService.changeStatus(status("IN_PROGRESS"));

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(result.version()).isEqualTo(VERSION + 1);
        assertThat(captureStatusChanged().status()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("상태 변경도 내가 본 버전이 낡았으면 409 다")
    void 상태_버전_충돌() {
        givenProjectFound(ProjectStatus.NOT_STARTED, null, null, null, VERSION);
        given(projectRepository.changeStatusIfVersionMatches(
                anyLong(), anyLong(), any(), any(), any(), any(), any(), anyInt()))
                .willReturn(0);

        assertThatThrownBy(() -> projectCommandService.changeStatus(status("IN_PROGRESS")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("상태 변경으로 CLOSED 를 설정할 수 없다 — 사유 없는 종결을 막는다")
    void 상태변경_CLOSED_거부() {
        givenProjectFound(ProjectStatus.NOT_STARTED, null, null, null, VERSION);

        assertThatThrownBy(() -> projectCommandService.changeStatus(status("CLOSED")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("정의되지 않은 상태 값은 400 이다")
    void 상태_오타() {
        givenProjectFound(ProjectStatus.NOT_STARTED, null, null, null, VERSION);

        assertThatThrownBy(() -> projectCommandService.changeStatus(status("DONE")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("COMPLETED 를 IN_PROGRESS 로 되돌릴 수 있다 — 역방향을 막지 않는다 (PRJ-003)")
    void 역방향_전이() {
        givenProject(ProjectStatus.COMPLETED, null, null, null);

        ProjectStatusResult result = projectCommandService.changeStatus(status("IN_PROGRESS"));

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(captureStatusChanged().status()).isEqualTo(ProjectStatus.IN_PROGRESS);
    }

    /**
     * ⚠️ 종결 정보를 UPDATE 에 안 실으면 <b>상태만 바뀌고 종결 사유·일시가 DB 에 남는다.</b>
     * 도메인 {@code changeStatus} 는 null 로 만들지만 그 결과를 SQL 에 넘기지 않으면 소용이 없다 —
     * 예외도 안 나고 응답도 정상이라 조회 화면을 봐야만 드러난다.
     */
    @Test
    @DisplayName("CLOSED 에서 벗어나면 종결 정보도 UPDATE 에 null 로 실려 간다")
    void 종결_해제() {
        LocalDateTime closedAt = LocalDateTime.of(2026, 8, 5, 12, 0);
        givenProject(ProjectStatus.CLOSED, CloseReasonCode.NOT_SELECTED, "기술평가 2순위로 탈락", closedAt);

        projectCommandService.changeStatus(status("IN_PROGRESS"));

        StatusChanged changed = captureStatusChanged();
        assertThat(changed.status()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(changed.closeReasonCode()).isNull();
        assertThat(changed.closeReasonNote()).isNull();
        assertThat(changed.closedAt()).isNull();
    }

    // ────────────────────────────── 종결 ──────────────────────────────

    /** 종결은 사유가 필수라 두 번 눌러도 같은 결과다 — 낙관락을 걸지 않는다 (`CONCURRENCY.md`). */
    @Test
    @DisplayName("사유를 붙여 종결한다 — deletedAt 은 건드리지 않는다")
    void 종결() {
        givenProject();

        ProjectCloseResult result = projectCommandService.closeProject(new CloseProjectCommand(
                PROJECT_ID, "NOT_SELECTED", "기술평가 2순위로 탈락", "E2024001", "USER"));

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
                new CloseProjectCommand(PROJECT_ID, null, null, "E2024001", "USER")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("종결 사유를 선택");

        assertThatThrownBy(() -> projectCommandService.closeProject(
                new CloseProjectCommand(PROJECT_ID, "LOST", null, "E2024001", "USER")))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("허용되지 않은");
    }

    @Test
    @DisplayName("편집 권한이 없으면 세 API 모두 저장까지 가지 않는다")
    void 권한_거부() {
        Mockito.doThrow(new ForbiddenException(ProjectErrorCode.PROJECT_EDIT_DENIED))
                .when(projectAccessUseCase).requireEditable(PROJECT_ID, "E2024001", "USER");

        assertThatThrownBy(() -> projectCommandService.updateProject(
                updateCommand("새 과업", null, null, STARTED, ENDED, null)))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> projectCommandService.changeStatus(status("IN_PROGRESS")))
                .isInstanceOf(ForbiddenException.class);

        assertThatThrownBy(() -> projectCommandService.closeProject(new CloseProjectCommand(
                PROJECT_ID, "NOT_SELECTED", null, "E2024001", "USER")))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verifyNoInteractions(projectRepository);
    }

    // ────────────────────────────── 헬퍼 ──────────────────────────────

    /** 조건부 UPDATE 에 실제로 실려 간 값. save() 를 안 쓰므로 엔티티를 캡처할 수 없다. */
    private record Updated(String name, String description, String clientName,
                           LocalDate startedOn, LocalDate endedOn, BigDecimal contractAmount,
                           int expectedVersion) {
    }

    private record StatusChanged(ProjectStatus status, CloseReasonCode closeReasonCode,
                                 String closeReasonNote, LocalDateTime closedAt) {
    }

    private Updated captureUpdated() {
        ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> clientName = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDate> startedOn = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDate> endedOn = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<BigDecimal> contractAmount = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<Integer> expectedVersion = ArgumentCaptor.forClass(Integer.class);

        Mockito.verify(projectRepository).updateIfVersionMatches(
                eq(PROJECT_ID), eq(COMPANY_ID), name.capture(), description.capture(),
                clientName.capture(), startedOn.capture(), endedOn.capture(),
                contractAmount.capture(), any(), expectedVersion.capture());

        return new Updated(name.getValue(), description.getValue(), clientName.getValue(),
                startedOn.getValue(), endedOn.getValue(), contractAmount.getValue(),
                expectedVersion.getValue());
    }

    private StatusChanged captureStatusChanged() {
        ArgumentCaptor<ProjectStatus> status = ArgumentCaptor.forClass(ProjectStatus.class);
        ArgumentCaptor<CloseReasonCode> closeReasonCode =
                ArgumentCaptor.forClass(CloseReasonCode.class);
        ArgumentCaptor<String> closeReasonNote = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> closedAt = ArgumentCaptor.forClass(LocalDateTime.class);

        Mockito.verify(projectRepository).changeStatusIfVersionMatches(
                eq(PROJECT_ID), eq(COMPANY_ID), status.capture(), closeReasonCode.capture(),
                closeReasonNote.capture(), closedAt.capture(), any(), anyInt());

        return new StatusChanged(status.getValue(), closeReasonCode.getValue(),
                closeReasonNote.getValue(), closedAt.getValue());
    }

    private UpdateProjectCommand updateCommand(String name, String description, String clientName,
                                               LocalDate startedOn, LocalDate endedOn,
                                               BigDecimal contractAmount) {
        return new UpdateProjectCommand(PROJECT_ID, name, description, clientName,
                startedOn, endedOn, contractAmount, VERSION, false, "E2024001", "USER");
    }

    private ChangeProjectStatusCommand status(String status) {
        return new ChangeProjectStatusCommand(
                PROJECT_ID, status, VERSION, false, "E2024001", "USER");
    }

    /** 기존 프로젝트 한 건. 검증이 먼저 터지는 케이스는 저장까지 가지 않는다. */
    private void givenProject() {
        givenProject(ProjectStatus.NOT_STARTED, null, null, null);
    }

    /** 상태·종결 정보를 지정한 기존 프로젝트 한 건. 조건부 UPDATE 는 1행 성공으로 둔다. */
    private void givenProject(ProjectStatus status, CloseReasonCode closeReasonCode,
                              String closeReasonNote, LocalDateTime closedAt) {
        givenProjectFound(status, closeReasonCode, closeReasonNote, closedAt, VERSION);

        Mockito.lenient().when(projectRepository.updateIfVersionMatches(
                        anyLong(), anyLong(), any(), any(), any(),
                        any(), any(), any(), any(), anyInt()))
                .thenReturn(1);
        Mockito.lenient().when(projectRepository.changeStatusIfVersionMatches(
                        anyLong(), anyLong(), any(), any(), any(), any(), any(), anyInt()))
                .thenReturn(1);
        // 종결·삭제 경로는 여전히 save() 를 쓴다 — 낙관락은 수정·상태변경에만 건다.
        Mockito.lenient().when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** 조회만 세팅한다. 조건부 UPDATE 스텁을 직접 지정하는 케이스용. */
    private void givenProjectFound(ProjectStatus status, CloseReasonCode closeReasonCode,
                                   String closeReasonNote, LocalDateTime closedAt, int version) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        Project existing = Project.restore(PROJECT_ID, COMPANY_ID, null, "기존 과업", "기존 설명",
                status, "OO시청", new BigDecimal("100000000"), STARTED, ENDED,
                closeReasonCode, closeReasonNote, closedAt, version,
                "E2024001", createdAt, createdAt, null);

        given(projectRepository.findById(PROJECT_ID, COMPANY_ID)).willReturn(Optional.of(existing));
    }

    private Project captureSaved() {
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        Mockito.verify(projectRepository).save(captor.capture());
        return captor.getValue();
    }
}
