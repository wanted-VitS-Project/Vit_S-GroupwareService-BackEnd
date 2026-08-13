package com.group3.vitamins.bidding.bidreview.application.service;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewCompanyDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewNoticeDocumentPort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewReferenceFilePort;
import com.group3.vitamins.bidding.bidreview.application.port.BidReviewWorkerPort;
import com.group3.vitamins.bidding.bidreview.application.query.GetBidReviewJobQuery;
import com.group3.vitamins.bidding.bidreview.application.result.BidReviewJobResult;
import com.group3.vitamins.bidding.bidreview.domain.exception.BidReviewErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("BidReviewJobQueryService worker 작업 조회")
class BidReviewJobQueryServiceTest {

    private static final Long REVIEW_ID = 71L;
    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 1L;
    private static final String ATTEMPT_ID = "4b0f03bb-c04d-4ff0-997b-3ff762cbfe22";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 13, 9, 0);

    private BidReviewWorkerPort workerPort;
    private BidReviewNoticeDocumentPort noticeDocumentPort;
    private BidReviewReferenceFilePort referenceFilePort;
    private BidReviewCompanyDocumentPort companyDocumentPort;
    private BidReviewJobQueryService service;

    @BeforeEach
    void setUp() {
        workerPort = mock(BidReviewWorkerPort.class);
        noticeDocumentPort = mock(BidReviewNoticeDocumentPort.class);
        referenceFilePort = mock(BidReviewReferenceFilePort.class);
        companyDocumentPort = mock(BidReviewCompanyDocumentPort.class);
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-13T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new BidReviewJobQueryService(
                workerPort, noticeDocumentPort, referenceFilePort, companyDocumentPort, clock
        );
    }

    @Test
    @DisplayName("현재 attemptId와 일치하는 작업을 점유해 첨부·참조파일 다운로드 정보를 조립한다")
    void claimsCurrentJob() {
        when(workerPort.claimJob(REVIEW_ID, ATTEMPT_ID, NOW))
                .thenReturn(Optional.of(new BidReviewWorkerPort.ClaimedJob(
                        REVIEW_ID, COMPANY_ID, NOTICE_ID, ATTEMPT_ID, "재정 상태를 검토해줘.",
                        List.of(
                                new BidReviewWorkerPort.JobDocument(
                                        "BID_ATTACHMENT", 31L, null, null, "제안요청서.pdf"
                                ),
                                new BidReviewWorkerPort.JobDocument(
                                        "INTERNAL_REFERENCE", null, 501L, null, "원가계산_기준.pdf"
                                ),
                                new BidReviewWorkerPort.JobDocument(
                                        "COMPANY_DOCUMENT_REFERENCE", null, null, 9001L, "재무제표.xlsx"
                                )
                        )
                )));
        when(noticeDocumentPort.findAccessibleNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(new BidReviewNoticeDocumentPort.NoticeSnapshot(
                        NOTICE_ID, "스마트시티 통합관제 용역"
                )));
        when(noticeDocumentPort.findAttachments(COMPANY_ID, NOTICE_ID, List.of(31L)))
                .thenReturn(List.of(new BidReviewNoticeDocumentPort.AttachmentSnapshot(
                        31L, NOTICE_ID, "제안요청서.pdf", "https://nara.example/31.pdf"
                )));
        when(referenceFilePort.findDownloadableFiles(COMPANY_ID, List.of(501L)))
                .thenReturn(List.of(new BidReviewReferenceFilePort.DownloadableReferenceFile(
                        501L, "원가계산_기준.pdf", "https://s3.example/501.pdf?sig=..."
                )));
        when(companyDocumentPort.findDownloadableDocuments(COMPANY_ID, List.of(9001L)))
                .thenReturn(List.of(new BidReviewCompanyDocumentPort.DownloadableCompanyDocument(
                        9001L, "재무제표.xlsx", "https://s3.example/9001.xlsx?sig=..."
                )));

        BidReviewJobResult result = service.handle(new GetBidReviewJobQuery(REVIEW_ID, ATTEMPT_ID));

        assertThat(result.reviewId()).isEqualTo(REVIEW_ID);
        assertThat(result.companyId()).isEqualTo(COMPANY_ID);
        assertThat(result.noticeId()).isEqualTo(NOTICE_ID);
        assertThat(result.noticeName()).isEqualTo("스마트시티 통합관제 용역");
        assertThat(result.attachments()).hasSize(1);
        assertThat(result.attachments().get(0).sourceUrl()).isEqualTo("https://nara.example/31.pdf");
        assertThat(result.referenceFiles()).hasSize(1);
        assertThat(result.referenceFiles().get(0).downloadUrl()).isEqualTo("https://s3.example/501.pdf?sig=...");
        assertThat(result.companyDocuments()).hasSize(1);
        assertThat(result.companyDocuments().get(0).downloadUrl()).isEqualTo("https://s3.example/9001.xlsx?sig=...");
    }

    @Test
    @DisplayName("현재 시도와 일치하는 작업이 없으면 NotFoundException을 던진다")
    void rejectsMissingJob() {
        when(workerPort.claimJob(REVIEW_ID, ATTEMPT_ID, NOW)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(
                new GetBidReviewJobQuery(REVIEW_ID, ATTEMPT_ID)
        )).isInstanceOf(NotFoundException.class)
                .satisfies(exception -> assertThat(((NotFoundException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_REVIEW_JOB_NOT_FOUND));

        verifyNoInteractions(noticeDocumentPort, referenceFilePort, companyDocumentPort);
    }

    @Test
    @DisplayName("잘못된 경로 값은 Port 호출 전에 거부한다")
    void rejectsInvalidQuery() {
        assertThatThrownBy(() -> service.handle(
                new GetBidReviewJobQuery(0L, "not-a-uuid")
        )).isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                        .isEqualTo(BidReviewErrorCode.BIDDING_INVALID_REVIEW_REQUEST));

        verifyNoInteractions(workerPort, noticeDocumentPort, referenceFilePort, companyDocumentPort);
    }

    @Test
    @DisplayName("null Query는 Port 호출 전에 거부한다")
    void rejectsNullQuery() {
        assertThatThrownBy(() -> service.handle(null))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(workerPort, noticeDocumentPort, referenceFilePort, companyDocumentPort);
    }
}
