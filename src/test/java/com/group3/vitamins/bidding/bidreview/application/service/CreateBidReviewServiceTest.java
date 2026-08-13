package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.command.CreateBidReviewCommand;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCommandPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCompanyDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewReferenceFilePort;
import com.group3.vitamins.bidding.bidreview.application.result.CreateBidReviewResult;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocumentRole;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("CreateBidReviewService 입찰 문서 검토 요청")
class CreateBidReviewServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 20L;
    private static final Long ATTACHMENT_ID = 30L;
    private static final Long REFERENCE_FILE_ID = 40L;
    private static final Long COMPANY_DOCUMENT_VERSION_ID = 50L;
    private static final String USER_ID = "EMP001";
    private static final String ROLE = "MEMBER";
    private static final String PROMPT = "기준 문서와 비교해서 금액·일정 관련 리스크를 짚어줘.";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    private BidReviewCommandPort commandPort;
    private BidReviewNoticeDocumentPort noticeDocumentPort;
    private BidReviewReferenceFilePort referenceFilePort;
    private BidReviewCompanyDocumentPort companyDocumentPort;
    private BiddingAccessPolicy biddingAccessPolicy;
    private CreateBidReviewService service;

    @BeforeEach
    void setUp() {
        commandPort = mock(BidReviewCommandPort.class);
        noticeDocumentPort = mock(BidReviewNoticeDocumentPort.class);
        referenceFilePort = mock(BidReviewReferenceFilePort.class);
        companyDocumentPort = mock(BidReviewCompanyDocumentPort.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        CurrentCompanyIdProvider companyIdProvider = mock(CurrentCompanyIdProvider.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-12T01:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );

        service = new CreateBidReviewService(
                commandPort,
                noticeDocumentPort,
                referenceFilePort,
                companyDocumentPort,
                biddingAccessPolicy,
                companyIdProvider,
                clock
        );

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(noticeDocumentPort.findAccessibleNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(new BidReviewNoticeDocumentPort.NoticeSnapshot(
                        NOTICE_ID, "스마트시티 통합관제 플랫폼 구축 용역", null
                )));
        when(noticeDocumentPort.findAttachments(COMPANY_ID, NOTICE_ID, List.of(ATTACHMENT_ID)))
                .thenReturn(List.of(new BidReviewNoticeDocumentPort.AttachmentSnapshot(
                        ATTACHMENT_ID, NOTICE_ID, "제안요청서.pdf", "https://example.org/rfp.pdf"
                )));
        when(referenceFilePort.findAccessibleFiles(COMPANY_ID, List.of(REFERENCE_FILE_ID)))
                .thenReturn(List.of(readyReference()));
        when(commandPort.existsProcessing(COMPANY_ID, NOTICE_ID, USER_ID))
                .thenReturn(false);
        when(commandPort.savePendingWithDocumentsAndOutbox(any(), any()))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    }

    @Test
    @DisplayName("공고 첨부와 사내 기준자료를 선택해 검토를 PENDING으로 생성한다")
    void createsPendingReviewWithSelectedDocuments() {
        CreateBidReviewResult result = service.create(command(
                List.of(ATTACHMENT_ID), List.of(REFERENCE_FILE_ID)
        ));

        assertThat(result.reviewId()).isEqualTo(100L);
        assertThat(result.reviewStatus()).isEqualTo("PENDING");

        ArgumentCaptor<List<BidReviewDocument>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(commandPort).savePendingWithDocumentsAndOutbox(any(), documentsCaptor.capture());

        List<BidReviewDocument> documents = documentsCaptor.getValue();
        assertThat(documents).hasSize(2);
        assertThat(documents)
                .extracting(BidReviewDocument::documentRole)
                .containsExactlyInAnyOrder(
                        BidReviewDocumentRole.BID_ATTACHMENT,
                        BidReviewDocumentRole.INTERNAL_REFERENCE
                );
    }

    @Test
    @DisplayName("다른 회사 소속이거나 제외된 공고는 찾을 수 없는 것으로 처리한다")
    void rejectsWhenNoticeNotAccessibleForCompany() {
        when(noticeDocumentPort.findAccessibleNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(command(
                List.of(ATTACHMENT_ID), List.of()
        )))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND));
        verify(commandPort, never()).savePendingWithDocumentsAndOutbox(any(), any());
    }

    @Test
    @DisplayName("다른 공고이거나 다른 회사 소속인 첨부파일이 섞이면 요청 전체를 거부한다")
    void rejectsWhenAttachmentNotAccessibleForCompany() {
        when(noticeDocumentPort.findAttachments(COMPANY_ID, NOTICE_ID, List.of(ATTACHMENT_ID)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create(command(
                List.of(ATTACHMENT_ID), List.of()
        )))
                .isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_NOTICE_ATTACHMENT_NOT_FOUND));
        verify(commandPort, never()).savePendingWithDocumentsAndOutbox(any(), any());
    }

    @Test
    @DisplayName("다른 회사 소속인 사내 기준자료가 섞이면 접근을 거부한다")
    void rejectsWhenReferenceFileNotAccessibleForCompany() {
        when(referenceFilePort.findAccessibleFiles(COMPANY_ID, List.of(REFERENCE_FILE_ID)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create(command(
                List.of(ATTACHMENT_ID), List.of(REFERENCE_FILE_ID)
        )))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat(((ForbiddenException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_DOCUMENT_ACCESS_DENIED));
        verify(commandPort, never()).savePendingWithDocumentsAndOutbox(any(), any());
    }

    @Test
    @DisplayName("업로드나 인덱싱이 끝나지 않은 사내 기준자료는 선택할 수 없다")
    void rejectsWhenReferenceFileNotReady() {
        when(referenceFilePort.findAccessibleFiles(COMPANY_ID, List.of(REFERENCE_FILE_ID)))
                .thenReturn(List.of(new BidReviewReferenceFilePort.ReferenceFileSnapshot(
                        REFERENCE_FILE_ID, "회사소개서.pdf", "COMPLETED", "PROCESSING"
                )));

        assertThatThrownBy(() -> service.create(command(
                List.of(ATTACHMENT_ID), List.of(REFERENCE_FILE_ID)
        )))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_DOCUMENT_NOT_READY));
        verify(commandPort, never()).savePendingWithDocumentsAndOutbox(any(), any());
    }

    @Test
    @DisplayName("사내 문서함 참조를 선택하면 COMPANY_DOCUMENT_REFERENCE 문서로 함께 생성한다")
    void createsPendingReviewWithCompanyDocumentReference() {
        when(companyDocumentPort.findAccessibleDocuments(List.of(COMPANY_DOCUMENT_VERSION_ID)))
                .thenReturn(List.of(readyCompanyDocument()));

        CreateBidReviewResult result = service.create(command(
                List.of(ATTACHMENT_ID), List.of(), List.of(COMPANY_DOCUMENT_VERSION_ID)
        ));

        assertThat(result.reviewId()).isEqualTo(100L);

        ArgumentCaptor<List<BidReviewDocument>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(commandPort).savePendingWithDocumentsAndOutbox(any(), documentsCaptor.capture());

        List<BidReviewDocument> documents = documentsCaptor.getValue();
        assertThat(documents)
                .extracting(BidReviewDocument::documentRole)
                .containsExactlyInAnyOrder(
                        BidReviewDocumentRole.BID_ATTACHMENT,
                        BidReviewDocumentRole.COMPANY_DOCUMENT_REFERENCE
                );
        assertThat(documents)
                .filteredOn(document -> document.documentRole() == BidReviewDocumentRole.COMPANY_DOCUMENT_REFERENCE)
                .extracting(BidReviewDocument::companyDocumentVersionId)
                .containsExactly(COMPANY_DOCUMENT_VERSION_ID);
    }

    @Test
    @DisplayName("다른 회사 소속이거나 미완료된 사내 문서함 참조가 섞이면 접근을 거부한다")
    void rejectsWhenCompanyDocumentNotAccessibleForCompany() {
        when(companyDocumentPort.findAccessibleDocuments(List.of(COMPANY_DOCUMENT_VERSION_ID)))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create(command(
                List.of(ATTACHMENT_ID), List.of(), List.of(COMPANY_DOCUMENT_VERSION_ID)
        )))
                .isInstanceOf(ForbiddenException.class)
                .satisfies(exception -> assertThat(((ForbiddenException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_DOCUMENT_ACCESS_DENIED));
        verify(commandPort, never()).savePendingWithDocumentsAndOutbox(any(), any());
    }

    @Test
    @DisplayName("같은 회사·공고·요청자의 활성 검토가 있으면 사전 검사에서 거부한다")
    void rejectsWhenActiveReviewAlreadyExists() {
        when(commandPort.existsProcessing(COMPANY_ID, NOTICE_ID, USER_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(command(
                List.of(ATTACHMENT_ID), List.of()
        )))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_ALREADY_PROCESSING));
        verify(commandPort, never()).savePendingWithDocumentsAndOutbox(any(), any());
    }

    @Test
    @DisplayName("동시 요청이 활성 검토 유니크 제약과 충돌하면 처리 중 오류로 변환한다")
    void translatesConcurrentActiveProcessingConflict() {
        doThrow(new DataIntegrityViolationException(
                "Duplicate entry for key 'uk_bid_review_active_processing'"
        )).when(commandPort).savePendingWithDocumentsAndOutbox(any(), any());

        assertThatThrownBy(() -> service.create(command(
                List.of(ATTACHMENT_ID), List.of()
        )))
                .isInstanceOf(ConflictException.class)
                .satisfies(exception -> assertThat(((ConflictException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_ALREADY_PROCESSING));
    }

    @Test
    @DisplayName("활성 검토 제약과 무관한 무결성 위반은 그대로 전파한다")
    void propagatesUnrelatedIntegrityViolation() {
        DataIntegrityViolationException failure =
                new DataIntegrityViolationException("another_constraint");
        doThrow(failure).when(commandPort)
                .savePendingWithDocumentsAndOutbox(any(), any());

        assertThatThrownBy(() -> service.create(command(
                List.of(ATTACHMENT_ID), List.of()
        )))
                .isSameAs(failure);
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 포트를 호출하지 않고 즉시 차단한다")
    void rejectsAccessDeniedBeforeTouchingPorts() {
        ForbiddenException accessDenied = new ForbiddenException(
                BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED
        );
        doThrow(accessDenied).when(biddingAccessPolicy).assertAccess(USER_ID, ROLE);

        assertThatThrownBy(() -> service.create(command(
                List.of(ATTACHMENT_ID), List.of()
        )))
                .isSameAs(accessDenied);
        verify(noticeDocumentPort, never()).findAccessibleNotice(any(), any());
        verify(commandPort, never()).savePendingWithDocumentsAndOutbox(any(), any());
    }

    @Test
    @DisplayName("공고 첨부를 하나도 선택하지 않으면 요청 자체를 거부한다")
    void rejectsInvalidRequestWithoutAttachments() {
        assertThatThrownBy(() -> service.create(command(List.of(), List.of())))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_INVALID_REVIEW_REQUEST));
        verify(commandPort, never()).savePendingWithDocumentsAndOutbox(any(), any());
    }

    private CreateBidReviewCommand command(
            List<Long> bidAttachmentIds,
            List<Long> referenceFileIds
    ) {
        return command(bidAttachmentIds, referenceFileIds, List.of());
    }

    private CreateBidReviewCommand command(
            List<Long> bidAttachmentIds,
            List<Long> referenceFileIds,
            List<Long> companyDocumentVersionIds
    ) {
        return new CreateBidReviewCommand(
                NOTICE_ID,
                bidAttachmentIds,
                referenceFileIds,
                companyDocumentVersionIds,
                PROMPT,
                USER_ID,
                ROLE
        );
    }

    private BidReviewReferenceFilePort.ReferenceFileSnapshot readyReference() {
        return new BidReviewReferenceFilePort.ReferenceFileSnapshot(
                REFERENCE_FILE_ID, "회사소개서.pdf", "COMPLETED", "COMPLETED"
        );
    }

    private BidReviewCompanyDocumentPort.CompanyDocumentReferenceSnapshot readyCompanyDocument() {
        return new BidReviewCompanyDocumentPort.CompanyDocumentReferenceSnapshot(
                COMPANY_DOCUMENT_VERSION_ID, "재무제표.xlsx"
        );
    }

    private BidReview withId(BidReview review) {
        return new BidReview(
                100L,
                review.companyId(),
                review.noticeId(),
                review.requestedBy(),
                review.projectId(),
                review.prompt(),
                review.reviewStatus(),
                review.processingAttemptId(),
                review.retryCount(),
                review.result(),
                review.errorCode(),
                review.errorMessage(),
                review.completedAt(),
                review.expiresAt(),
                review.abandonedAt(),
                review.cleanupStartedAt(),
                review.cleanupCompletedAt(),
                NOW,
                NOW
        );
    }
}
