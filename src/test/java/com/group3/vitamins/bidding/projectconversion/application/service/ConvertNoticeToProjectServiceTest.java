package com.group3.vitamins.bidding.projectconversion.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.projectconversion.application.command.ConvertNoticeToProjectCommand;
import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeProjectAccessPort;
import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeProjectExistencePort;
import com.group3.vitamins.bidding.projectconversion.application.port.BidNoticeSummaryProjectLinkPort;
import com.group3.vitamins.bidding.projectconversion.application.port.BidReviewProjectLinkPort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.project.application.command.CreateProjectCommand;
import com.group3.vitamins.project.application.usecase.ProjectCommandUseCase;
import com.group3.vitamins.project.application.usecase.ProjectMemberCommandUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ConvertNoticeToProjectService - 1~4단계 공고 접근·검토·요약·중복전환 검증")
class ConvertNoticeToProjectServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 100L;
    private static final Long REVIEW_ID = 1L;
    private static final Long SUMMARY_ID = 9L;
    private static final String REQUESTER_USER_ID = "EMP001";

    private BidNoticeProjectAccessPort noticeAccessPort;
    private BidReviewProjectLinkPort reviewLinkPort;
    private BidNoticeSummaryProjectLinkPort summaryLinkPort;
    private BidNoticeProjectExistencePort noticeProjectExistencePort;
    private BiddingAccessPolicy biddingAccessPolicy;
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private ProjectCommandUseCase projectCommandUseCase;
    private ProjectMemberCommandUseCase projectMemberCommandUseCase;
    private ConvertNoticeToProjectService service;

    @BeforeEach
    void setUp() {
        noticeAccessPort = mock(BidNoticeProjectAccessPort.class);
        reviewLinkPort = mock(BidReviewProjectLinkPort.class);
        summaryLinkPort = mock(BidNoticeSummaryProjectLinkPort.class);
        noticeProjectExistencePort = mock(BidNoticeProjectExistencePort.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        currentCompanyIdProvider = mock(CurrentCompanyIdProvider.class);
        projectCommandUseCase = mock(ProjectCommandUseCase.class);
        projectMemberCommandUseCase = mock(ProjectMemberCommandUseCase.class);
        service = new ConvertNoticeToProjectService(
                noticeAccessPort, reviewLinkPort, summaryLinkPort, noticeProjectExistencePort,
                biddingAccessPolicy, currentCompanyIdProvider, projectCommandUseCase, projectMemberCommandUseCase
        );

        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(noticeAccessPort.isAccessible(COMPANY_ID, NOTICE_ID)).thenReturn(true);
        when(reviewLinkPort.findReview(REVIEW_ID)).thenReturn(Optional.of(
                new BidReviewProjectLinkPort.ReviewSnapshot(
                        REVIEW_ID, COMPANY_ID, NOTICE_ID, REQUESTER_USER_ID, "COMPLETED", null
                )
        ));
        when(noticeProjectExistencePort.existsForNotice(COMPANY_ID, NOTICE_ID)).thenReturn(false);
    }

    @Test
    @DisplayName("현재 회사가 접근할 수 없는 공고면 프로젝트를 만들지 않고 404를 던진다")
    void rejectsWhenNoticeNotAccessible() {
        when(noticeAccessPort.isAccessible(COMPANY_ID, NOTICE_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.convert(command()))
                .isInstanceOf(NotFoundException.class);

        verify(projectCommandUseCase, never()).createProject(any());
        verify(projectMemberCommandUseCase, never()).addMember(any());
    }

    @Test
    @DisplayName("reviewId 자체가 없으면 404를 던진다")
    void rejectsWhenReviewNotFound() {
        when(reviewLinkPort.findReview(REVIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convert(command()))
                .isInstanceOf(NotFoundException.class);

        verify(projectCommandUseCase, never()).createProject(any());
    }

    @Test
    @DisplayName("다른 회사 소유의 검토면 403을 던진다")
    void rejectsWhenReviewBelongsToDifferentCompany() {
        when(reviewLinkPort.findReview(REVIEW_ID)).thenReturn(Optional.of(
                new BidReviewProjectLinkPort.ReviewSnapshot(
                        REVIEW_ID, 999L, NOTICE_ID, REQUESTER_USER_ID, "COMPLETED", null
                )
        ));

        assertThatThrownBy(() -> service.convert(command()))
                .isInstanceOf(ForbiddenException.class);

        verify(projectCommandUseCase, never()).createProject(any());
    }

    @Test
    @DisplayName("요청자가 아닌 다른 사람이 요청한 검토면 403을 던진다")
    void rejectsWhenReviewRequestedByAnotherUser() {
        when(reviewLinkPort.findReview(REVIEW_ID)).thenReturn(Optional.of(
                new BidReviewProjectLinkPort.ReviewSnapshot(
                        REVIEW_ID, COMPANY_ID, NOTICE_ID, "EMP999", "COMPLETED", null
                )
        ));

        assertThatThrownBy(() -> service.convert(command()))
                .isInstanceOf(ForbiddenException.class);

        verify(projectCommandUseCase, never()).createProject(any());
    }

    @Test
    @DisplayName("다른 공고의 검토면 403을 던진다")
    void rejectsWhenReviewBelongsToDifferentNotice() {
        when(reviewLinkPort.findReview(REVIEW_ID)).thenReturn(Optional.of(
                new BidReviewProjectLinkPort.ReviewSnapshot(
                        REVIEW_ID, COMPANY_ID, 777L, REQUESTER_USER_ID, "COMPLETED", null
                )
        ));

        assertThatThrownBy(() -> service.convert(command()))
                .isInstanceOf(ForbiddenException.class);

        verify(projectCommandUseCase, never()).createProject(any());
    }

    @Test
    @DisplayName("아직 완료되지 않은(PROCESSING 등) 검토면 409를 던진다")
    void rejectsWhenReviewNotCompleted() {
        when(reviewLinkPort.findReview(REVIEW_ID)).thenReturn(Optional.of(
                new BidReviewProjectLinkPort.ReviewSnapshot(
                        REVIEW_ID, COMPANY_ID, NOTICE_ID, REQUESTER_USER_ID, "PROCESSING", null
                )
        ));

        assertThatThrownBy(() -> service.convert(command()))
                .isInstanceOf(ConflictException.class);

        verify(projectCommandUseCase, never()).createProject(any());
    }

    @Test
    @DisplayName("summaryId를 지정했는데 없으면 404를 던진다")
    void rejectsWhenSummaryNotFound() {
        when(summaryLinkPort.findSummary(COMPANY_ID, NOTICE_ID, SUMMARY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.convert(commandWithSummary()))
                .isInstanceOf(NotFoundException.class);

        verify(projectCommandUseCase, never()).createProject(any());
    }

    @Test
    @DisplayName("확정되지 않은 요약이면 409를 던진다")
    void rejectsWhenSummaryNotConfirmed() {
        when(summaryLinkPort.findSummary(COMPANY_ID, NOTICE_ID, SUMMARY_ID)).thenReturn(Optional.of(
                new BidNoticeSummaryProjectLinkPort.SummarySnapshot(SUMMARY_ID, false, null)
        ));

        assertThatThrownBy(() -> service.convert(commandWithSummary()))
                .isInstanceOf(ConflictException.class);

        verify(projectCommandUseCase, never()).createProject(any());
    }

    @Test
    @DisplayName("이미 다른 프로젝트에 연결된 요약이면 409를 던진다")
    void rejectsWhenSummaryAlreadyLinked() {
        when(summaryLinkPort.findSummary(COMPANY_ID, NOTICE_ID, SUMMARY_ID)).thenReturn(Optional.of(
                new BidNoticeSummaryProjectLinkPort.SummarySnapshot(SUMMARY_ID, true, 555L)
        ));

        assertThatThrownBy(() -> service.convert(commandWithSummary()))
                .isInstanceOf(ConflictException.class);

        verify(projectCommandUseCase, never()).createProject(any());
    }

    @Test
    @DisplayName("이 공고로 이미 프로젝트가 만들어져 있으면 409를 던지고 프로젝트를 새로 만들지 않는다")
    void rejectsWhenNoticeAlreadyConvertedToProject() {
        when(noticeProjectExistencePort.existsForNotice(COMPANY_ID, NOTICE_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.convert(command()))
                .isInstanceOf(ConflictException.class);

        verify(projectCommandUseCase, never()).createProject(any());
        verify(projectMemberCommandUseCase, never()).addMember(any());
    }

    @Test
    @DisplayName("요청자가 BIDDING 권한이 없으면 403을 던지고 프로젝트를 만들지 않는다")
    void rejectsWhenRequesterHasNoBiddingAccess() {
        doThrow(new ForbiddenException(
                com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode
                        .BIDDING_ACCESS_PERMISSION_REQUIRED
        )).when(biddingAccessPolicy).assertAccess(REQUESTER_USER_ID, "MEMBER");

        assertThatThrownBy(() -> service.convert(command()))
                .isInstanceOf(ForbiddenException.class);

        verify(projectCommandUseCase, never()).createProject(any());
        verify(projectMemberCommandUseCase, never()).addMember(any());
    }

    @Test
    @DisplayName("4번 선확인 통과 후 동시요청 경합으로 DB UNIQUE 제약을 위반하면 409로 변환한다")
    void convertsRaceConditionViolationTo409() {
        // 첫 호출(4번 선확인)은 false, catch 안에서 다시 확인할 때는 true - 그 사이에 경합자가
        // 실제로 커밋했다는 뜻이라 이때만 409로 변환해야 한다.
        when(noticeProjectExistencePort.existsForNotice(COMPANY_ID, NOTICE_ID)).thenReturn(false, true);
        when(projectCommandUseCase.createProject(any(CreateProjectCommand.class)))
                .thenThrow(new DataIntegrityViolationException("uk_project_bid_notice_company violated"));

        assertThatThrownBy(() -> service.convert(command()))
                .isInstanceOf(ConflictException.class);

        verify(projectMemberCommandUseCase, never()).addMember(any());
    }

    @Test
    @DisplayName("공고 중복 전환이 원인이 아닌 다른 무결성 위반이면 변환하지 않고 원인 그대로 던진다")
    void rethrowsIntegrityViolationWhenNotCausedByDuplicateNoticeConversion() {
        DataIntegrityViolationException unrelatedViolation =
                new DataIntegrityViolationException("fk_project_created_by violated");
        // 재확인해도 여전히 false - 이 공고로 만든 프로젝트가 진짜로 없다는 뜻이라, 경합이 아니라
        // 다른 무결성 위반(FK 등)이라고 판단하고 그대로 다시 던져야 한다.
        when(noticeProjectExistencePort.existsForNotice(COMPANY_ID, NOTICE_ID)).thenReturn(false, false);
        when(projectCommandUseCase.createProject(any(CreateProjectCommand.class)))
                .thenThrow(unrelatedViolation);

        assertThatThrownBy(() -> service.convert(command()))
                .isSameAs(unrelatedViolation);

        verify(projectMemberCommandUseCase, never()).addMember(any());
    }

    private ConvertNoticeToProjectCommand command() {
        return new ConvertNoticeToProjectCommand(
                NOTICE_ID, REVIEW_ID, null, "테스트 프로젝트", null, 500L,
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 12, 31),
                List.of(), REQUESTER_USER_ID, "MEMBER"
        );
    }

    private ConvertNoticeToProjectCommand commandWithSummary() {
        return new ConvertNoticeToProjectCommand(
                NOTICE_ID, REVIEW_ID, SUMMARY_ID, "테스트 프로젝트", null, 500L,
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 12, 31),
                List.of(), REQUESTER_USER_ID, "MEMBER"
        );
    }
}
