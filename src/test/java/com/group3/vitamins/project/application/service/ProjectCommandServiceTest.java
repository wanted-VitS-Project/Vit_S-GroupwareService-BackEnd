package com.group3.vitamins.project.application.service;

import com.group3.vitamins.businesscategory.domain.exception.BusinessCategoryErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.project.application.command.CreateProjectCommand;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort;
import com.group3.vitamins.project.application.port.BusinessCategoryLookupPort.BusinessCategoryView;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.result.BusinessCategorySummary;
import com.group3.vitamins.project.application.result.ProjectResult;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.model.ProjectMember;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import com.group3.vitamins.project.domain.repository.ProjectBusinessCategoryRepository;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import com.group3.vitamins.project.domain.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProjectCommandService 프로젝트 생성")
class ProjectCommandServiceTest {

    private static final String REQUESTER_ID = "E2024001";
    private static final String REQUESTER_NAME = "김용준";
    private static final Long PROJECT_ID = 12L;
    private static final String VALID_NAME = "OO시 상수도 관리 용역";

    private ProjectRepository projectRepository;
    private ProjectMemberRepository projectMemberRepository;
    private ProjectBusinessCategoryRepository projectBusinessCategoryRepository;
    private BusinessCategoryLookupPort businessCategoryLookupPort;
    private EmployeeLookupPort employeeLookupPort;
    private ProjectCommandService projectCommandService;

    @BeforeEach
    void setUp() {
        projectRepository = Mockito.mock(ProjectRepository.class);
        projectMemberRepository = Mockito.mock(ProjectMemberRepository.class);
        projectBusinessCategoryRepository = Mockito.mock(ProjectBusinessCategoryRepository.class);
        businessCategoryLookupPort = Mockito.mock(BusinessCategoryLookupPort.class);
        employeeLookupPort = Mockito.mock(EmployeeLookupPort.class);
        projectCommandService = new ProjectCommandService(projectRepository, projectMemberRepository,
                projectBusinessCategoryRepository, businessCategoryLookupPort, employeeLookupPort);
    }

    /** 검증을 전부 통과하는 기본 커맨드. 개별 테스트가 필요한 필드만 바꿔 쓴다. */
    private CreateProjectCommand validCommand(String name, LocalDate startedOn, LocalDate endedOn,
                                               Long bidNoticeId, List<Long> businessCategoryIds) {
        return new CreateProjectCommand(bidNoticeId, name, "상수도 관리 시스템 고도화 용역", "OO시청",
                startedOn, endedOn, BigDecimal.valueOf(120_000_000), businessCategoryIds, REQUESTER_ID);
    }

    private CreateProjectCommand validCommand() {
        return validCommand(VALID_NAME, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31), null, List.of());
    }

    /** save 가 반환하는 영속 상태의 Project. 저장 후 응답 조립에 쓰인다. */
    private Project savedProject(CreateProjectCommand command) {
        return Project.restore(PROJECT_ID, command.bidNoticeId(), command.name(), command.description(),
                ProjectStatus.NOT_STARTED, command.clientName(), command.contractAmount(),
                command.startedOn(), command.endedOn(), null, null,
                command.requesterUserId(), LocalDateTime.of(2026, 8, 1, 10, 0), null);
    }

    /** 저장까지 도달하는 성공 경로에 필요한 공통 스텁. */
    private void stubHappyPath(CreateProjectCommand command) {
        when(projectRepository.save(any(Project.class))).thenReturn(savedProject(command));
        when(employeeLookupPort.findNameByUserId(REQUESTER_ID)).thenReturn(REQUESTER_NAME);
    }

    private Consumer<Throwable> hasCode(com.group3.vitamins.global.domain.common.error.ErrorCode expected) {
        return thrown -> {
            assertThat(thrown).isInstanceOf(DomainException.class);
            assertThat(((DomainException) thrown).getErrorCode()).isEqualTo(expected);
        };
    }

    @Nested
    @DisplayName("과업명 검증")
    class NameValidation {

        @Test
        @DisplayName("이름이 null 이면 PROJECT_NAME_REQUIRED")
        void rejectsNullName() {
            CreateProjectCommand command = validCommand(null, null, null, null, List.of());

            assertThatThrownBy(() -> projectCommandService.createProject(command))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_NAME_REQUIRED));
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("이름이 공백이면 PROJECT_NAME_REQUIRED")
        void rejectsBlankName() {
            CreateProjectCommand command = validCommand("   ", null, null, null, List.of());

            assertThatThrownBy(() -> projectCommandService.createProject(command))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_NAME_REQUIRED));
        }

        @Test
        @DisplayName("이름이 300자를 넘으면 PROJECT_NAME_TOO_LONG")
        void rejectsTooLongName() {
            String tooLong = "가".repeat(301);
            CreateProjectCommand command = validCommand(tooLong, null, null, null, List.of());

            assertThatThrownBy(() -> projectCommandService.createProject(command))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_NAME_TOO_LONG));
        }

        @Test
        @DisplayName("이름이 정확히 300자면 통과한다")
        void acceptsExactly300Chars() {
            String exactly300 = "가".repeat(300);
            CreateProjectCommand command = validCommand(exactly300, null, null, null, List.of());
            stubHappyPath(command);

            ProjectResult result = projectCommandService.createProject(command);

            assertThat(result.name()).isEqualTo(exactly300);
        }
    }

    @Nested
    @DisplayName("기간 검증")
    class DateRangeValidation {

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 PROJECT_DATE_RANGE_INVALID")
        void rejectsInvalidRange() {
            CreateProjectCommand command = validCommand(VALID_NAME,
                    LocalDate.of(2026, 12, 31), LocalDate.of(2026, 1, 1), null, List.of());

            assertThatThrownBy(() -> projectCommandService.createProject(command))
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_DATE_RANGE_INVALID));
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("시작일만 있으면 검증하지 않는다")
        void skipsValidationWhenEndedOnMissing() {
            CreateProjectCommand command = validCommand(VALID_NAME,
                    LocalDate.of(2026, 8, 1), null, null, List.of());
            stubHappyPath(command);

            ProjectResult result = projectCommandService.createProject(command);

            assertThat(result.startedOn()).isEqualTo(LocalDate.of(2026, 8, 1));
            assertThat(result.endedOn()).isNull();
        }

        @Test
        @DisplayName("종료일만 있으면 검증하지 않는다")
        void skipsValidationWhenStartedOnMissing() {
            CreateProjectCommand command = validCommand(VALID_NAME,
                    null, LocalDate.of(2026, 12, 31), null, List.of());
            stubHappyPath(command);

            ProjectResult result = projectCommandService.createProject(command);

            assertThat(result.startedOn()).isNull();
            assertThat(result.endedOn()).isEqualTo(LocalDate.of(2026, 12, 31));
        }

        @Test
        @DisplayName("시작일과 종료일이 같으면 통과한다")
        void acceptsEqualDates() {
            LocalDate sameDay = LocalDate.of(2026, 8, 1);
            CreateProjectCommand command = validCommand(VALID_NAME, sameDay, sameDay, null, List.of());
            stubHappyPath(command);

            ProjectResult result = projectCommandService.createProject(command);

            assertThat(result.startedOn()).isEqualTo(sameDay);
            assertThat(result.endedOn()).isEqualTo(sameDay);
        }
    }

    @Nested
    @DisplayName("공고 연결 검증")
    class BidNoticeValidation {

        @Test
        @DisplayName("bidNoticeId 가 없으면 중복 확인을 하지 않는다")
        void skipsCheckWhenBidNoticeIdMissing() {
            CreateProjectCommand command = validCommand();
            stubHappyPath(command);

            projectCommandService.createProject(command);

            verify(projectRepository, never()).findByBidNoticeId(anyLong());
        }

        @Test
        @DisplayName("이미 연결된 공고면 PROJECT_BID_NOTICE_ALREADY_LINKED")
        void rejectsAlreadyLinkedBidNotice() {
            CreateProjectCommand command = validCommand(VALID_NAME,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31), 5L, List.of());
            Project existing = savedProject(command);
            when(projectRepository.findByBidNoticeId(5L)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> projectCommandService.createProject(command))
                    .isInstanceOf(ConflictException.class)
                    .satisfies(hasCode(ProjectErrorCode.PROJECT_BID_NOTICE_ALREADY_LINKED));
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("연결 안 된 공고면 통과한다")
        void acceptsUnlinkedBidNotice() {
            CreateProjectCommand command = validCommand(VALID_NAME,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31), 5L, List.of());
            when(projectRepository.findByBidNoticeId(5L)).thenReturn(Optional.empty());
            stubHappyPath(command);

            ProjectResult result = projectCommandService.createProject(command);

            assertThat(result.bidNoticeId()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("사업 카테고리 해석")
    class CategoryResolution {

        @Test
        @DisplayName("categoryIds 가 없으면 조회하지 않고 빈 리스트를 반환한다")
        void returnsEmptyWhenNoCategoryIds() {
            CreateProjectCommand command = validCommand();
            stubHappyPath(command);

            ProjectResult result = projectCommandService.createProject(command);

            assertThat(result.businessCategories()).isEmpty();
            verify(businessCategoryLookupPort, never()).findByIds(any());
            verify(projectBusinessCategoryRepository, never()).linkAll(any(), any());
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 id 가 섞이면 BUSINESS_CATEGORY_NOT_FOUND")
        void rejectsMissingCategory() {
            CreateProjectCommand command = validCommand(VALID_NAME,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31), null, List.of(1L, 4L));
            when(businessCategoryLookupPort.findByIds(List.of(1L, 4L)))
                    .thenReturn(List.of(new BusinessCategoryView(1L, "환경", "ENV")));

            assertThatThrownBy(() -> projectCommandService.createProject(command))
                    .satisfies(hasCode(BusinessCategoryErrorCode.BUSINESS_CATEGORY_NOT_FOUND));
            verify(projectRepository, never()).save(any());
        }

        @Test
        @DisplayName("중복 id 는 distinct 처리 후 조회한다")
        void deduplicatesBeforeLookup() {
            CreateProjectCommand command = validCommand(VALID_NAME,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31), null, List.of(1L, 1L, 4L));
            when(businessCategoryLookupPort.findByIds(List.of(1L, 4L)))
                    .thenReturn(List.of(new BusinessCategoryView(1L, "환경", "ENV"),
                            new BusinessCategoryView(4L, "상하수도", "WATER")));
            stubHappyPath(command);

            ProjectResult result = projectCommandService.createProject(command);

            verify(businessCategoryLookupPort).findByIds(List.of(1L, 4L));
            assertThat(result.businessCategories()).hasSize(2);
        }

        @Test
        @DisplayName("전부 존재하면 요약으로 변환해 반환하고 연결 테이블에 링크한다")
        void resolvesAndLinksCategories() {
            CreateProjectCommand command = validCommand(VALID_NAME,
                    LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31), null, List.of(1L, 4L));
            when(businessCategoryLookupPort.findByIds(List.of(1L, 4L)))
                    .thenReturn(List.of(new BusinessCategoryView(1L, "환경", "ENV"),
                            new BusinessCategoryView(4L, "상하수도", "WATER")));
            stubHappyPath(command);

            ProjectResult result = projectCommandService.createProject(command);

            assertThat(result.businessCategories())
                    .containsExactly(new BusinessCategorySummary(1L, "환경", "ENV"),
                            new BusinessCategorySummary(4L, "상하수도", "WATER"));
            verify(projectBusinessCategoryRepository).linkAll(PROJECT_ID, List.of(1L, 4L));
        }
    }

    @Nested
    @DisplayName("생성 성공")
    class CreateSuccess {

        @Test
        @DisplayName("정상 요청이면 프로젝트를 저장하고 결과를 반환한다")
        void createsProjectAndReturnsResult() {
            CreateProjectCommand command = validCommand();
            stubHappyPath(command);

            ProjectResult result = projectCommandService.createProject(command);

            assertThat(result.projectId()).isEqualTo(PROJECT_ID);
            assertThat(result.name()).isEqualTo(VALID_NAME);
            assertThat(result.status()).isEqualTo("NOT_STARTED");
            assertThat(result.createdBy()).isEqualTo(new ProjectResult.CreatedBy(REQUESTER_ID, REQUESTER_NAME));
        }

        @Test
        @DisplayName("생성자는 자동으로 EDITOR 참여자로 등록된다")
        void registersRequesterAsEditor() {
            CreateProjectCommand command = validCommand();
            stubHappyPath(command);

            projectCommandService.createProject(command);

            ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
            verify(projectMemberRepository).save(captor.capture());
            ProjectMember member = captor.getValue();
            assertThat(member.getProjectId()).isEqualTo(PROJECT_ID);
            assertThat(member.getUserId()).isEqualTo(REQUESTER_ID);
            assertThat(member.getPermission()).isEqualTo(MemberPermission.EDITOR);
        }

        @Test
        @DisplayName("카테고리가 비어있으면 연결 테이블에 쓰지 않는다")
        void skipsLinkingWhenNoCategoriesRequested() {
            CreateProjectCommand command = validCommand();
            stubHappyPath(command);

            projectCommandService.createProject(command);

            verify(projectBusinessCategoryRepository, never()).linkAll(any(), any());
        }

        @Test
        @DisplayName("프로젝트를 먼저 저장해 얻은 id 로 참여자를 등록한다")
        void savesProjectBeforeMember() {
            CreateProjectCommand command = validCommand();
            stubHappyPath(command);

            projectCommandService.createProject(command);

            var inOrder = Mockito.inOrder(projectRepository, projectMemberRepository);
            inOrder.verify(projectRepository).save(any(Project.class));
            inOrder.verify(projectMemberRepository).save(any(ProjectMember.class));
        }
    }
}
