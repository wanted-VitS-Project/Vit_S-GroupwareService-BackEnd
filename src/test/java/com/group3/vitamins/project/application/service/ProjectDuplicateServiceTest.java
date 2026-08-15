package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.command.DuplicateProjectCommand;
import com.group3.vitamins.project.application.port.BlockClonePort;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.port.StageCascadePort;
import com.group3.vitamins.project.application.port.StageClonePort;
import com.group3.vitamins.project.application.port.StepCascadePort;
import com.group3.vitamins.project.application.port.StepClonePort;
import com.group3.vitamins.project.application.port.StepStatLookupPort;
import com.group3.vitamins.project.application.result.ProjectDuplicateResult;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.model.ProjectMember;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * 프로젝트 복제 (PRJ-018). 생성 자체의 검증(날짜 관계·카테고리 존재·공고 중복)은 생성 경로를 그대로 타므로
 * {@code ProjectCommandServiceTest} 소관이고, 여기서는 <b>복제에만 있는 규칙</b>을 본다 —
 * 계층 복사 순서 · 권한 기준 · 상한 검사 시점 · 원본 값 비승계.
 */
@ExtendWith(MockitoExtension.class)
class ProjectDuplicateServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private ProjectBusinessCategoryRepository projectBusinessCategoryRepository;
    @Mock private BusinessCategoryLookupPort businessCategoryLookupPort;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private StepStatLookupPort stepStatLookupPort;
    @Mock private StepCascadePort stepCascadePort;
    @Mock private StageCascadePort stageCascadePort;
    @Mock private StageClonePort stageClonePort;
    @Mock private StepClonePort stepClonePort;
    @Mock private BlockClonePort blockClonePort;
    @Mock private ProjectAccessUseCase projectAccessUseCase;
    @Mock private CurrentCompanyIdProvider currentCompanyIdProvider;

    @InjectMocks private ProjectCommandService projectCommandService;

    private static final Long SOURCE_ID = 12L;
    private static final Long CLONE_ID = 31L;
    private static final Long COMPANY_ID = 1L;
    private static final String REQUESTER = "E2024001";
    private static final String ROLE = "USER";

    /** 원본 stageId → 복제본 stageId. 값이 키와 겹치지 않게 잡아 잘못 흘러가면 드러나게 한다. */
    private static final Map<Long, Long> STAGE_ID_MAP = Map.of(101L, 201L, 102L, 202L);
    private static final Map<Long, Long> STEP_ID_MAP = Map.of(301L, 401L, 302L, 402L);

    @BeforeEach
    void 회사_컨텍스트() {
        Mockito.lenient().when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    // ────────────────────────────── 복사 순서 ──────────────────────────────

    @Test
    @DisplayName("스테이지 → 스텝 → 블록 순으로 부르고, 앞 계층의 id 매핑을 뒤에 그대로 넘긴다")
    void 복사_순서와_매핑_전달() {
        givenCloneChain(new BlockClonePort.ClonedBlocks(40, 0));

        projectCommandService.duplicateProject(command(null));

        InOrder order = Mockito.inOrder(stageClonePort, stepClonePort, blockClonePort);
        order.verify(stageClonePort).cloneStages(SOURCE_ID, CLONE_ID);
        order.verify(stepClonePort).cloneSteps(SOURCE_ID, CLONE_ID, STAGE_ID_MAP);
        order.verify(blockClonePort).cloneBlocks(STEP_ID_MAP, REQUESTER);
    }

    @Test
    @DisplayName("복사 수량과 건너뛴 수를 응답에 담는다 — 건너뛴 블록이 조용히 사라지면 안 된다")
    void 복사_수량() {
        givenCloneChain(new BlockClonePort.ClonedBlocks(40, 2));

        ProjectDuplicateResult result = projectCommandService.duplicateProject(command(null));

        assertThat(result.sourceProjectId()).isEqualTo(SOURCE_ID);
        assertThat(result.project().projectId()).isEqualTo(CLONE_ID);
        assertThat(result.copied().stages()).isEqualTo(STAGE_ID_MAP.size());
        assertThat(result.copied().steps()).isEqualTo(STEP_ID_MAP.size());
        assertThat(result.copied().blocks()).isEqualTo(40);
        assertThat(result.skipped().blocks()).isEqualTo(2);
    }

    // ────────────────────────────── 권한 ──────────────────────────────

    @Test
    @DisplayName("원본에는 참여 자격만 요구한다 — 원본을 안 바꾸므로 편집 권한을 묻지 않는다")
    void 참여자면_복제할_수_있다() {
        givenCloneChain(new BlockClonePort.ClonedBlocks(0, 0));

        projectCommandService.duplicateProject(command(null));

        Mockito.verify(projectAccessUseCase).requireAccess(SOURCE_ID, REQUESTER, ROLE);
        Mockito.verify(projectAccessUseCase, Mockito.never())
                .requireEditable(anyLong(), anyString(), anyString());
    }

    // ────────────────────────────── 상한 ──────────────────────────────

    @Test
    @DisplayName("블록이 300개를 넘으면 400 이고, 복사를 시작하기 전에 거절한다")
    void 상한_초과() {
        given(blockClonePort.countBlocks(SOURCE_ID)).willReturn(301);

        // 코드까지 본다 — 메시지의 "301" 만 보면 다른 검증 오류가 같은 숫자를 담아도 통과한다.
        // 프론트는 이 코드로 「원본을 줄여 달라」 안내를 분기한다.
        assertThatThrownBy(() -> projectCommandService.duplicateProject(command(null)))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("301")
                .extracting(e -> ((ValidationException) e).getErrorCode())
                .isEqualTo(ProjectErrorCode.PROJECT_DUPLICATE_TOO_LARGE);

        // 프로젝트도 만들지 않는다 — 다 만든 뒤 거절하면 그만큼 헛일하고 롤백한다
        Mockito.verify(projectRepository, Mockito.never()).save(any(Project.class));
        Mockito.verify(stageClonePort, Mockito.never()).cloneStages(anyLong(), anyLong());
    }

    @Test
    @DisplayName("정확히 300개는 통과한다 — 상한은 초과일 때만 막는다")
    void 상한_경계() {
        givenCloneChain(new BlockClonePort.ClonedBlocks(300, 0));
        given(blockClonePort.countBlocks(SOURCE_ID)).willReturn(300);

        assertThat(projectCommandService.duplicateProject(command(null)).copied().blocks())
                .isEqualTo(300);
    }

    // ────────────────────────────── 원본 값 비승계 ──────────────────────────────

    @Test
    @DisplayName("공고는 요청에 담긴 값만 연결된다 — 안 보내면 null 이다 (원본 공고를 승계하면 UNIQUE 위반)")
    void 공고_비승계() {
        givenCloneChain(new BlockClonePort.ClonedBlocks(0, 0));

        projectCommandService.duplicateProject(command(null));

        assertThat(captureSavedProject().getBidNoticeId()).isNull();
        // 승계하지 않으므로 원본 공고의 중복 검사를 돌릴 이유가 없다
        Mockito.verify(projectRepository, Mockito.never()).findByBidNoticeId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("복제본은 NOT_STARTED 로 시작하고 요청자가 EDITOR 참여자가 된다")
    void 복제본_초기_상태() {
        givenCloneChain(new BlockClonePort.ClonedBlocks(0, 0));

        projectCommandService.duplicateProject(command(null));

        assertThat(captureSavedProject().getStatus()).isEqualTo(ProjectStatus.NOT_STARTED);

        ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
        Mockito.verify(projectMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(REQUESTER);
    }

    @Test
    @DisplayName("복제는 원본을 한 글자도 바꾸지 않는다 — 삭제·정리 경로를 부르지 않는다")
    void 원본_불변() {
        givenCloneChain(new BlockClonePort.ClonedBlocks(0, 0));

        projectCommandService.duplicateProject(command(null));

        Mockito.verifyNoInteractions(stepCascadePort, stageCascadePort, stepStatLookupPort);
    }

    // ────────────────────────────── 헬퍼 ──────────────────────────────

    /** 생성 + 복사 3계층을 성공 경로로 세팅한다. 저장된 프로젝트에는 DB 가 준 것처럼 id 를 붙여 돌려준다. */
    private void givenCloneChain(BlockClonePort.ClonedBlocks blocks) {
        Mockito.lenient().when(blockClonePort.countBlocks(SOURCE_ID)).thenReturn(0);
        Mockito.lenient().when(projectRepository.save(any(Project.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        Mockito.lenient().when(employeeLookupPort.findNameByUserId(REQUESTER)).thenReturn("김용준");
        Mockito.lenient().when(stageClonePort.cloneStages(SOURCE_ID, CLONE_ID))
                .thenReturn(STAGE_ID_MAP);
        Mockito.lenient().when(stepClonePort.cloneSteps(SOURCE_ID, CLONE_ID, STAGE_ID_MAP))
                .thenReturn(STEP_ID_MAP);
        Mockito.lenient().when(blockClonePort.cloneBlocks(STEP_ID_MAP, REQUESTER))
                .thenReturn(blocks);
    }

    /** 저장 직후 상태를 재현한다 — projectId 가 없으면 하위 계층이 어디로 복사할지 알 수 없다. */
    private Project withId(Project saved) {
        return Project.restore(CLONE_ID, COMPANY_ID, saved.getBidNoticeId(), saved.getName(),
                saved.getDescription(), saved.getStatus(), saved.getClientName(),
                saved.getContractAmount(), saved.getStartedOn(), saved.getEndedOn(),
                null, null, null, saved.getVersion(), saved.getCreatedBy(),
                saved.getCreatedAt(), saved.getUpdatedAt(), null);
    }

    private Project captureSavedProject() {
        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        Mockito.verify(projectRepository).save(captor.capture());
        return captor.getValue();
    }

    private DuplicateProjectCommand command(Long bidNoticeId) {
        return new DuplicateProjectCommand(SOURCE_ID, bidNoticeId,
                "OO시 상수도 관리 용역 (2차)", null, "OO시청",
                LocalDate.of(2027, 1, 1), LocalDate.of(2027, 6, 30), null,
                List.of(), REQUESTER, ROLE);
    }
}
