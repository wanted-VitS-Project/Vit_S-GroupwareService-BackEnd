package com.group3.vitamins.file.application;

import com.group3.vitamins.file.application.policy.FileAdminPolicy;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.result.AdminTreeProjectPageResult;
import com.group3.vitamins.file.application.result.AdminTreeStageProjection;
import com.group3.vitamins.file.application.result.AdminTreeStepProjection;
import com.group3.vitamins.file.application.result.CompanyFilePageResult;
import com.group3.vitamins.file.application.service.AdminFileTreeService;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AdminFileTreeService 전사 파일 트리(§14)")
class AdminFileTreeServiceTest {

    private static final long COMPANY = 7L;
    private static final String ADMIN = "ADMIN";
    private static final Long PROJECT = 100L;

    private FileQueryPort fileQueryPort;
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private FileAdminPolicy fileAdminPolicy;
    private AdminFileTreeService service;

    @BeforeEach
    void setUp() {
        fileQueryPort = Mockito.mock(FileQueryPort.class);
        currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        fileAdminPolicy = Mockito.mock(FileAdminPolicy.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY);
        service = new AdminFileTreeService(fileQueryPort, currentCompanyIdProvider, fileAdminPolicy);
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return t -> assertThat(t).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(expected);
    }

    @Test
    @DisplayName("§14.1 프로젝트 페이지 — 회사 스코프로 조회하고 totalPages 를 파생한다")
    void getProjects() {
        when(fileQueryPort.countAdminTreeProjects(COMPANY)).thenReturn(23L);
        when(fileQueryPort.findAdminTreeProjects(COMPANY, 10, 0L)).thenReturn(List.of());

        AdminTreeProjectPageResult r = service.getProjects(ADMIN, 0, 10);

        assertThat(r.page()).isZero();
        assertThat(r.size()).isEqualTo(10);
        assertThat(r.totalElements()).isEqualTo(23L);
        assertThat(r.totalPages()).isEqualTo(3); // ceil(23/10)
        verify(fileAdminPolicy).assertAdmin(ADMIN);
    }

    @Test
    @DisplayName("ADMIN 이 아니면 정책이 막는다 — 조회로 넘어가지 않는다")
    void nonAdminBlocked() {
        doThrow(new ForbiddenException(
                com.group3.vitamins.account.domain.exception.AccountErrorCode.ACC_ADMIN_REQUIRED))
                .when(fileAdminPolicy).assertAdmin("MEMBER");

        assertThatThrownBy(() -> service.getProjects("MEMBER", 0, 10))
                .isInstanceOf(ForbiddenException.class);
        verify(fileQueryPort, never()).countAdminTreeProjects(Mockito.anyLong());
    }

    @Test
    @DisplayName("§14.2 스테이지 — 미소속 스텝이 있으면 맨 뒤에 미분류 버킷(stageId=null)을 붙인다")
    void getStagesAppendsUnassignedBucket() {
        when(fileQueryPort.existsProjectInCompany(COMPANY, PROJECT)).thenReturn(true);
        when(fileQueryPort.findAdminTreeStages(COMPANY, PROJECT))
                .thenReturn(List.of(new AdminTreeStageProjection(1L, "스테이지1", 0)));
        when(fileQueryPort.existsUnassignedStep(COMPANY, PROJECT)).thenReturn(true);

        List<AdminTreeStageProjection> stages = service.getStages(ADMIN, PROJECT);

        assertThat(stages).hasSize(2);
        assertThat(stages.get(1).stageId()).isNull();
        assertThat(stages.get(1).name()).isEqualTo("미분류");
    }

    @Test
    @DisplayName("§14.2 미소속 스텝이 없으면 미분류 버킷을 붙이지 않는다")
    void getStagesNoBucket() {
        when(fileQueryPort.existsProjectInCompany(COMPANY, PROJECT)).thenReturn(true);
        when(fileQueryPort.findAdminTreeStages(COMPANY, PROJECT))
                .thenReturn(List.of(new AdminTreeStageProjection(1L, "스테이지1", 0)));
        when(fileQueryPort.existsUnassignedStep(COMPANY, PROJECT)).thenReturn(false);

        assertThat(service.getStages(ADMIN, PROJECT)).hasSize(1);
    }

    @Test
    @DisplayName("§14.2 회사에 없는 프로젝트면 PROJECT_NOT_FOUND")
    void getStagesProjectNotFound() {
        when(fileQueryPort.existsProjectInCompany(COMPANY, PROJECT)).thenReturn(false);

        assertThatThrownBy(() -> service.getStages(ADMIN, PROJECT))
                .satisfies(hasCode(ProjectErrorCode.PROJECT_NOT_FOUND));
        verify(fileQueryPort, never()).findAdminTreeStages(Mockito.anyLong(), Mockito.any());
    }

    @Test
    @DisplayName("§14.3 스텝 — stageId 를 그대로 넘긴다(null 이면 미분류)")
    void getStepsPassesStageId() {
        when(fileQueryPort.existsProjectInCompany(COMPANY, PROJECT)).thenReturn(true);
        when(fileQueryPort.findAdminTreeSteps(COMPANY, PROJECT, null))
                .thenReturn(List.of(new AdminTreeStepProjection(5L, "미소속스텝", 0, "IN_PROGRESS")));

        List<AdminTreeStepProjection> steps = service.getSteps(ADMIN, PROJECT, null);

        assertThat(steps).singleElement().extracting(AdminTreeStepProjection::stepId).isEqualTo(5L);
        verify(fileQueryPort).findAdminTreeSteps(COMPANY, PROJECT, null);
    }

    @Test
    @DisplayName("§14.4 회사에 없는 스텝이면 FILE_STEP_NOT_FOUND")
    void getStepFilesStepNotFound() {
        when(fileQueryPort.existsStepInCompany(COMPANY, 9L)).thenReturn(false);

        assertThatThrownBy(() -> service.getStepFiles(ADMIN, 9L, 0, 10))
                .satisfies(hasCode(FileErrorCode.FILE_STEP_NOT_FOUND));
        verify(fileQueryPort, never()).countAdminTreeStepFiles(Mockito.anyLong(), eq(9L));
    }

    @Test
    @DisplayName("§14.4 스텝 파일 — 빈 스텝도 페이지 메타를 채워 반환한다")
    void getStepFilesEmpty() {
        when(fileQueryPort.existsStepInCompany(COMPANY, 5L)).thenReturn(true);
        when(fileQueryPort.countAdminTreeStepFiles(COMPANY, 5L)).thenReturn(0L);
        when(fileQueryPort.findAdminTreeStepFiles(COMPANY, 5L, 10, 0L)).thenReturn(List.of());

        CompanyFilePageResult r = service.getStepFiles(ADMIN, 5L, 0, 10);

        assertThat(r.content()).isEmpty();
        assertThat(r.size()).isEqualTo(10);
        assertThat(r.totalElements()).isZero();
    }
}
