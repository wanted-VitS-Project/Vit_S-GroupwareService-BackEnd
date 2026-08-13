package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.command.DeleteProjectCommand;
import com.group3.vitamins.project.application.command.LinkBusinessCategoriesCommand;
import com.group3.vitamins.project.application.command.UnlinkBusinessCategoryCommand;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.port.StageCascadePort;
import com.group3.vitamins.project.application.port.StepCascadePort;
import com.group3.vitamins.project.application.port.StepStatLookupPort;
import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.application.result.ProjectCategoryResult;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import com.group3.vitamins.project.domain.repository.ProjectBusinessCategoryRepository;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

/** 사업 카테고리 연결·해제 (#52) 와 프로젝트 삭제 (#51). 생성·수정은 ProjectCommandServiceTest 소관이다. */
@ExtendWith(MockitoExtension.class)
class ProjectCategoryAndDeleteServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectBusinessCategoryRepository projectBusinessCategoryRepository;
    @Mock private BusinessCategoryLookupPort businessCategoryLookupPort;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private StepStatLookupPort stepStatLookupPort;
    @Mock private StepCascadePort stepCascadePort;
    @Mock private StageCascadePort stageCascadePort;
    @Mock private ProjectAccessUseCase projectAccessUseCase;
    @Mock private CurrentCompanyIdProvider currentCompanyIdProvider;

    @InjectMocks private ProjectCommandService projectCommandService;

    private static final Long PROJECT_ID = 12L;
    private static final String REQUESTER = "E2024001";
    private static final Long COMPANY_ID = 1L;

    @BeforeEach
    void 회사_컨텍스트() {
        Mockito.lenient().when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    // ────────────────────────────── 카테고리 연결 ──────────────────────────────

    @Test
    @DisplayName("연결 후 전체 카테고리를 돌려준다 — 방금 추가한 것만이 아니다")
    void 연결() {
        given(businessCategoryLookupPort.findByIds(List.of(4L), COMPANY_ID)).willReturn(List.of(
                new BusinessCategoryLookupPort.BusinessCategoryView(4L, "상하수도", null)));
        given(projectBusinessCategoryRepository.findCategoryIds(PROJECT_ID))
                .willReturn(List.of(1L), List.of(1L, 4L));
        given(businessCategoryLookupPort.findRefsByIds(List.of(1L, 4L), COMPANY_ID)).willReturn(List.of(
                new BusinessCategoryLookupPort.BusinessCategoryRef(1L, "환경", "ENV", false),
                new BusinessCategoryLookupPort.BusinessCategoryRef(4L, "상하수도", null, false)));

        ProjectCategoryResult result = projectCommandService.linkBusinessCategories(
                new LinkBusinessCategoriesCommand(PROJECT_ID, List.of(4L), REQUESTER, "USER"));

        assertThat(result.businessCategories()).hasSize(2);
        Mockito.verify(projectBusinessCategoryRepository).linkAll(PROJECT_ID, List.of(4L));
    }

    @Test
    @DisplayName("이미 연결돼 있던 카테고리가 삭제됐어도 새 연결은 성공하고 deleted 로 내려간다")
    void 연결_기존_카테고리_삭제됨() {
        given(businessCategoryLookupPort.findByIds(List.of(4L), COMPANY_ID)).willReturn(List.of(
                new BusinessCategoryLookupPort.BusinessCategoryView(4L, "상하수도", null)));
        given(projectBusinessCategoryRepository.findCategoryIds(PROJECT_ID))
                .willReturn(List.of(1L), List.of(1L, 4L));
        // 1번은 그 사이 삭제됐다 — 응답 조회가 검증용 findByIds 를 타면 개수가 안 맞아 404 가 난다.
        given(businessCategoryLookupPort.findRefsByIds(List.of(1L, 4L), COMPANY_ID)).willReturn(List.of(
                new BusinessCategoryLookupPort.BusinessCategoryRef(1L, "환경", "ENV", true),
                new BusinessCategoryLookupPort.BusinessCategoryRef(4L, "상하수도", null, false)));

        ProjectCategoryResult result = projectCommandService.linkBusinessCategories(
                new LinkBusinessCategoriesCommand(PROJECT_ID, List.of(4L), REQUESTER, "USER"));

        assertThat(result.businessCategories())
                .extracting(BusinessCategorySummary::categoryId, BusinessCategorySummary::deleted)
                .containsExactly(tuple(1L, true), tuple(4L, false));
        Mockito.verify(projectBusinessCategoryRepository).linkAll(PROJECT_ID, List.of(4L));
    }

    @Test
    @DisplayName("빈 목록은 400 이다")
    void 연결_빈_목록() {
        assertThatThrownBy(() -> projectCommandService.linkBusinessCategories(
                new LinkBusinessCategoriesCommand(PROJECT_ID, List.of(), REQUESTER, "USER")))
                .isInstanceOf(ValidationException.class);

        Mockito.verifyNoInteractions(projectBusinessCategoryRepository);
    }

    @Test
    @DisplayName("이미 연결된 카테고리가 섞이면 409 다")
    void 연결_중복() {
        given(businessCategoryLookupPort.findByIds(List.of(1L), COMPANY_ID)).willReturn(List.of(
                new BusinessCategoryLookupPort.BusinessCategoryView(1L, "환경", "ENV")));
        given(projectBusinessCategoryRepository.findCategoryIds(PROJECT_ID))
                .willReturn(List.of(1L));

        assertThatThrownBy(() -> projectCommandService.linkBusinessCategories(
                new LinkBusinessCategoriesCommand(PROJECT_ID, List.of(1L), REQUESTER, "USER")))
                .isInstanceOf(ConflictException.class);

        Mockito.verify(projectBusinessCategoryRepository, Mockito.never())
                .linkAll(any(), anyList());
    }

    @Test
    @DisplayName("없는 카테고리는 404 다")
    void 연결_카테고리_없음() {
        given(businessCategoryLookupPort.findByIds(List.of(99L), COMPANY_ID)).willReturn(List.of());

        assertThatThrownBy(() -> projectCommandService.linkBusinessCategories(
                new LinkBusinessCategoriesCommand(PROJECT_ID, List.of(99L), REQUESTER, "USER")))
                .isInstanceOf(NotFoundException.class);
    }

    // ────────────────────────────── 카테고리 해제 ──────────────────────────────

    @Test
    @DisplayName("연결 행이 없으면 404 다")
    void 해제_연결_없음() {
        given(projectBusinessCategoryRepository.unlink(PROJECT_ID, 4L)).willReturn(false);

        assertThatThrownBy(() -> projectCommandService.unlinkBusinessCategory(
                new UnlinkBusinessCategoryCommand(PROJECT_ID, 4L, REQUESTER, "USER")))
                .isInstanceOf(NotFoundException.class);
    }

    // ────────────────────────────── 프로젝트 삭제 ──────────────────────────────

    @Test
    @DisplayName("진행 전 + 스텝 0개면 확인 없이 삭제하고 공고 연결을 비운다")
    void 삭제() {
        givenProject(ProjectStatus.NOT_STARTED, 7L);
        givenStepCount(0);

        projectCommandService.deleteProject(deleteCommand(false));

        Project saved = captureSaved();
        assertThat(saved.getDeletedAt()).isNotNull();
        // 안 비우면 uk_project_bid_notice 때문에 그 공고로 프로젝트를 다시 못 만든다.
        assertThat(saved.getBidNoticeId()).isNull();

        // 스텝이 0개여도 이것들은 남는다 — 프로젝트는 복구가 없으니 전부 죽은 행이다 (DELETE.md §2-2).
        // ⚠️ 순서까지 못 박는다. save() 를 앞으로 옮기면 하위 정리의 벌크 UPDATE
        //    (@Modifying(clearAutomatically = true)) 가 flush 없이 컨텍스트를 비워
        //    프로젝트는 안 지워지고 하위만 지워진다. 순서를 안 보면 그 리팩터링이 통과한다.
        InOrder 순서 = Mockito.inOrder(
                stepCascadePort, stageCascadePort, projectMemberRepository,
                projectBusinessCategoryRepository, projectRepository);
        순서.verify(stepCascadePort).deleteByProjectId(PROJECT_ID, REQUESTER);
        순서.verify(stageCascadePort).deleteByProjectId(PROJECT_ID);
        순서.verify(projectMemberRepository).deleteByProjectId(PROJECT_ID);
        순서.verify(projectBusinessCategoryRepository).deleteByProjectId(PROJECT_ID);
        순서.verify(projectRepository).save(any());
    }

    @Test
    @DisplayName("스텝이 남아 있으면 확인을 요구하고 아무것도 건드리지 않는다")
    void 삭제_확인_요구() {
        givenProject(ProjectStatus.NOT_STARTED, null);
        givenStepCount(3);

        assertThatThrownBy(() -> projectCommandService.deleteProject(deleteCommand(false)))
                .isInstanceOf(ConflictException.class)
                // 몇 개가 날아가는지 모르면 사용자가 확인 버튼을 누를 근거가 없다.
                .hasMessageContaining("스텝 3개");

        Mockito.verifyNoInteractions(stepCascadePort, stageCascadePort);
        Mockito.verify(projectMemberRepository, Mockito.never()).deleteByProjectId(any());
        Mockito.verify(projectBusinessCategoryRepository, Mockito.never()).deleteByProjectId(any());
        Mockito.verify(projectRepository, Mockito.never()).save(any(Project.class));
    }

    @Test
    @DisplayName("진행 중이면 스텝이 0개여도 확인을 요구한다")
    void 삭제_진행중_확인_요구() {
        givenProject(ProjectStatus.IN_PROGRESS, null);
        givenStepCount(0);

        assertThatThrownBy(() -> projectCommandService.deleteProject(deleteCommand(false)))
                .isInstanceOf(ConflictException.class);

        Mockito.verify(projectRepository, Mockito.never()).save(any(Project.class));
    }

    @Test
    @DisplayName("confirm=true 면 진행 중이고 스텝이 남아 있어도 하위까지 지운다")
    void 삭제_확인_후_강제() {
        givenProject(ProjectStatus.IN_PROGRESS, null);
        givenStepCount(3);

        projectCommandService.deleteProject(deleteCommand(true));

        // ⚠️ 확인은 게이트가 아니라 되묻기다 — confirm 이 붙으면 상태·스텝 수와 무관하게 통과해야 한다.
        Mockito.verify(stepCascadePort).deleteByProjectId(PROJECT_ID, REQUESTER);
        assertThat(captureSaved().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("없는 프로젝트는 404 다")
    void 삭제_프로젝트_없음() {
        given(projectRepository.findById(PROJECT_ID, COMPANY_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectCommandService.deleteProject(deleteCommand(false)))
                .isInstanceOf(NotFoundException.class);
    }

    private DeleteProjectCommand deleteCommand(boolean confirm) {
        return new DeleteProjectCommand(PROJECT_ID, REQUESTER, "USER", confirm);
    }

    private void givenStepCount(int totalCount) {
        given(stepStatLookupPort.countByProjectId(PROJECT_ID))
                .willReturn(new StepStatLookupPort.StepStatView(totalCount, 0));
    }

    private void givenProject(ProjectStatus status, Long bidNoticeId) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        given(projectRepository.findById(PROJECT_ID, COMPANY_ID)).willReturn(Optional.of(
                Project.restore(PROJECT_ID, COMPANY_ID, bidNoticeId, "하수관로 정비", null, status,
                        "○○시청", BigDecimal.TEN, null, null, null, null, null, 1,
                        REQUESTER, createdAt, createdAt, null)));
        Mockito.lenient().when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Project captureSaved() {
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        Mockito.verify(projectRepository).save(captor.capture());
        return captor.getValue();
    }
}
