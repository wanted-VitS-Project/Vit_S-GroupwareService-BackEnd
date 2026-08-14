package com.group3.vitamins.bidding.bidnotice.application.service;

import com.group3.vitamins.bidding.bidnotice.application.command.CompleteBidNoticeAttachmentUploadCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.CreateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.DismissBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.FavoriteBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.PatchField;
import com.group3.vitamins.bidding.bidnotice.application.command.RestoreBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.StartBidNoticeAttachmentUploadCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.UnfavoriteBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.UpdateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeCommandPort;
import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeCommandPort.PendingAttachmentUpload;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeAttachmentUploadCompleteResult;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeAttachmentUploadStartResult;
import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeStatusHistoryPort;
import com.group3.vitamins.bidding.bidnotice.application.port.CompanyBidNoticeStatePort;
import com.group3.vitamins.bidding.bidnotice.application.support.ManualBidNoticeDedupKeyGenerator;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNotice;
import com.group3.vitamins.bidding.bidnotice.domain.model.BidNoticeCompanyStatus;
import com.group3.vitamins.bidding.bidnotice.domain.model.BidNoticeStatusHistory;
import com.group3.vitamins.bidding.bidnotice.domain.model.CompanyBidNoticeState;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeData;
import com.group3.vitamins.bidding.bidnotice.domain.event.BidNoticeListChangedEvent;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeCommandService 직접 등록 공고 관리")
class BidNoticeCommandServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long NOTICE_ID = 100L;
    private static final String USER_ID = "EMP001";
    private static final LocalDateTime ANNOUNCED_AT =
            LocalDateTime.of(2026, 8, 11, 9, 0);
    private static final LocalDateTime DEADLINE_AT =
            LocalDateTime.of(2026, 8, 20, 18, 0);

    private BidNoticeCommandPort commandPort;
    private CompanyBidNoticeStatePort companyStatePort;
    private BidNoticeStatusHistoryPort statusHistoryPort;
    private CurrentCompanyIdProvider companyIdProvider;
    private BiddingAccessPolicy biddingAccessPolicy;
    private DomainEventPublisher eventPublisher;
    private FileStoragePort fileStoragePort;
    private BidNoticeCommandService service;

    @BeforeEach
    void setUp() {
        commandPort = mock(BidNoticeCommandPort.class);
        companyStatePort = mock(CompanyBidNoticeStatePort.class);
        statusHistoryPort = mock(BidNoticeStatusHistoryPort.class);
        companyIdProvider = mock(CurrentCompanyIdProvider.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        eventPublisher = mock(DomainEventPublisher.class);
        fileStoragePort = mock(FileStoragePort.class);

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(commandPort.findManualSourceId()).thenReturn(Optional.of(3L));
        when(commandPort.save(any(ManualBidNotice.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        service = new BidNoticeCommandService(
                commandPort,
                companyStatePort,
                statusHistoryPort,
                companyIdProvider,
                biddingAccessPolicy,
                new ManualBidNoticeDedupKeyGenerator(),
                eventPublisher,
                fileStoragePort
        );
    }

    @Test
    @DisplayName("현재 회사 소유로 직접 등록 공고와 최초 회사 상태를 함께 저장한다")
    void createsManualNoticeForCurrentCompany() {
        service.create(validCreateCommand("https://example.org/notice"));

        ArgumentCaptor<ManualBidNotice> captor =
                ArgumentCaptor.forClass(ManualBidNotice.class);
        verify(commandPort).save(captor.capture());

        ManualBidNotice saved = captor.getValue();
        assertThat(saved.getOwnerCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(saved.getExternalId()).startsWith("MANUAL-");
        assertThat(saved.getManualDedupKey()).hasSize(64);
        assertThat(saved.getData().attachments().get(0).attachmentOrder()).isEqualTo(1);
        verify(companyStatePort).observeManualRegistration(
                eq(COMPANY_ID),
                eq(NOTICE_ID),
                any(LocalDateTime.class)
        );
        verify(eventPublisher).publish(new BidNoticeListChangedEvent(COMPANY_ID));
    }

    @Test
    @DisplayName("같은 중복 키의 활성 직접 등록 공고가 있으면 등록을 거부한다")
    void rejectsDuplicatedManualNotice() {
        when(commandPort.existsActiveDuplicate(eq(COMPANY_ID), anyString(), isNull()))
                .thenReturn(true);

        assertError(
                () -> service.create(validCreateCommand("https://example.org/notice")),
                BiddingErrorCode.BIDDING_MANUAL_NOTICE_DUPLICATED
        );

        verify(commandPort, never()).save(any());
        verifyNoInteractions(companyStatePort);
    }

    @Test
    @DisplayName("서명 정보가 포함된 임시 URL은 직접 등록에서 거부한다")
    void rejectsSignedTemporaryUrl() {
        assertError(
                () -> service.create(validCreateCommand(
                        "https://example.org/notice?X-Amz-Signature=secret"
                )),
                BiddingErrorCode.BIDDING_INVALID_MANUAL_NOTICE
        );

        verify(commandPort, never()).existsActiveDuplicate(anyLong(), anyString(), any());
        verify(commandPort, never()).save(any());
    }

    @Test
    @DisplayName("PATCH의 명시적 null은 선택 필드를 해제하고 생략 필드는 유지한다")
    void clearsExplicitNullAndKeepsAbsentFields() {
        ManualBidNotice existing = existingNotice();
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existing));

        service.update(updateDemandAgencyToNull());

        assertThat(existing.getData().demandAgency()).isNull();
        assertThat(existing.getData().noticeName()).isEqualTo("스마트시티 구축 용역");
        verify(commandPort).existsActiveDuplicate(
                eq(COMPANY_ID),
                anyString(),
                eq(NOTICE_ID)
        );
        verify(eventPublisher).publish(new BidNoticeListChangedEvent(COMPANY_ID));
    }

    @Test
    @DisplayName("PATCH에서 첨부를 생략하면 기존 첨부를 유지한다")
    void keepsAttachmentsWhenOmitted() {
        ManualBidNotice existing = existingNoticeWithAttachments();
        List<ManualBidNoticeAttachment> expectedAttachments =
                List.copyOf(existing.getData().attachments());
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existing));

        service.update(updateDemandAgencyToNull());

        assertThat(captureSavedNotice().getData().attachments())
                .containsExactlyElementsOf(expectedAttachments);
    }

    @Test
    @DisplayName("PATCH에서 빈 첨부 배열을 전달하면 기존 첨부를 모두 제거한다")
    void removesAttachmentsWhenEmptyArrayProvided() {
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existingNoticeWithAttachments()));

        service.update(updateAttachments(List.of()));

        assertThat(captureSavedNotice().getData().attachments()).isEmpty();
    }

    @Test
    @DisplayName("PATCH에서 첨부 배열을 전달하면 요청 순서로 전체 교체한다")
    void replacesAttachmentsInRequestOrder() {
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existingNoticeWithAttachments()));
        List<ManualBidNoticeAttachment> replacements = List.of(
                new ManualBidNoticeAttachment(7, "기술제안서.pdf", "https://example.org/tech.pdf"),
                new ManualBidNoticeAttachment(8, "가격제안서.pdf", "https://example.org/price.pdf")
        );

        service.update(updateAttachments(replacements));

        List<ManualBidNoticeAttachment> savedAttachments =
                captureSavedNotice().getData().attachments();
        assertThat(savedAttachments)
                .extracting(ManualBidNoticeAttachment::attachmentOrder)
                .containsExactly(1, 2);
        assertThat(savedAttachments)
                .extracting(ManualBidNoticeAttachment::fileName)
                .containsExactly("기술제안서.pdf", "가격제안서.pdf");
    }

    @Test
    @DisplayName("공용 외부 수집 공고는 직접 등록 수정 API로 변경할 수 없다")
    void rejectsEditingExternalNotice() {
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.empty());
        when(commandPort.existsExternalNotice(NOTICE_ID)).thenReturn(true);

        assertError(
                () -> service.update(updateDemandAgencyToNull()),
                BiddingErrorCode.BIDDING_NOTICE_EDIT_NOT_ALLOWED
        );

        verify(commandPort, never()).save(any());
    }

    @Test
    @DisplayName("현재 회사의 수집 공고를 제외하고 상태 변경 이력을 저장한다")
    void dismissesCompanyNoticeAndSavesHistory() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(collectedState()));

        var result = service.dismiss(new DismissBidNoticeCommand(
                NOTICE_ID, " 사업 범위와 맞지 않음 ", USER_ID, "ADMIN"
        ));

        ArgumentCaptor<CompanyBidNoticeState> stateCaptor =
                ArgumentCaptor.forClass(CompanyBidNoticeState.class);
        verify(companyStatePort).update(stateCaptor.capture());
        assertThat(stateCaptor.getValue().status()).isEqualTo(BidNoticeCompanyStatus.DISMISSED);
        assertThat(stateCaptor.getValue().dismissReason()).isEqualTo("사업 범위와 맞지 않음");

        ArgumentCaptor<BidNoticeStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(BidNoticeStatusHistory.class);
        verify(statusHistoryPort).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().previousStatus()).isEqualTo(BidNoticeCompanyStatus.COLLECTED);
        assertThat(historyCaptor.getValue().changedStatus()).isEqualTo(BidNoticeCompanyStatus.DISMISSED);
        assertThat(historyCaptor.getValue().changedBy()).isEqualTo(USER_ID);
        assertThat(result.noticeStatus()).isEqualTo("DISMISSED");
        verify(eventPublisher).publish(new BidNoticeListChangedEvent(COMPANY_ID));
    }

    @Test
    @DisplayName("현재 회사가 제외한 공고를 복구하고 제외 사유를 제거한다")
    void restoresDismissedCompanyNotice() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(dismissedState()));

        var result = service.restore(new RestoreBidNoticeCommand(
                NOTICE_ID, USER_ID, "ADMIN"
        ));

        ArgumentCaptor<CompanyBidNoticeState> stateCaptor =
                ArgumentCaptor.forClass(CompanyBidNoticeState.class);
        verify(companyStatePort).update(stateCaptor.capture());
        assertThat(stateCaptor.getValue().status()).isEqualTo(BidNoticeCompanyStatus.COLLECTED);
        assertThat(stateCaptor.getValue().dismissReason()).isNull();
        assertThat(result.noticeStatus()).isEqualTo("COLLECTED");
        assertThat(result.dismissReason()).isNull();
        verify(statusHistoryPort).save(any(BidNoticeStatusHistory.class));
        verify(eventPublisher).publish(new BidNoticeListChangedEvent(COMPANY_ID));
    }

    @Test
    @DisplayName("현재 회사에서 조회되지 않는 타 회사 공고는 제외할 수 없다")
    void rejectsDismissingNoticeOwnedByAnotherCompany() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.empty());

        assertError(
                () -> service.dismiss(new DismissBidNoticeCommand(
                        NOTICE_ID, "제외 사유", USER_ID, "ADMIN"
                )),
                BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
        );

        verify(companyStatePort, never()).update(any());
        verifyNoInteractions(statusHistoryPort);
    }

    @Test
    @DisplayName("이미 제외된 공고의 중복 제외를 거부한다")
    void rejectsDismissingAlreadyDismissedNotice() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(dismissedState()));

        assertError(
                () -> service.dismiss(new DismissBidNoticeCommand(
                        NOTICE_ID, "다시 제외", USER_ID, "ADMIN"
                )),
                BiddingErrorCode.BIDDING_NOTICE_ALREADY_DISMISSED
        );

        verify(companyStatePort, never()).update(any());
        verifyNoInteractions(statusHistoryPort);
    }

    @Test
    @DisplayName("제외되지 않은 공고의 복구를 거부한다")
    void rejectsRestoringCollectedNotice() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(collectedState()));

        assertError(
                () -> service.restore(new RestoreBidNoticeCommand(
                        NOTICE_ID, USER_ID, "ADMIN"
                )),
                BiddingErrorCode.BIDDING_NOTICE_NOT_DISMISSED
        );

        verify(companyStatePort, never()).update(any());
        verifyNoInteractions(statusHistoryPort);
    }

    @Test
    @DisplayName("현재 회사 공용 관심 목록에 공고를 등록한다")
    void favoritesCompanyNotice() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(collectedState()));

        var result = service.favorite(new FavoriteBidNoticeCommand(NOTICE_ID, USER_ID, "ADMIN"));

        ArgumentCaptor<CompanyBidNoticeState> stateCaptor =
                ArgumentCaptor.forClass(CompanyBidNoticeState.class);
        verify(companyStatePort).update(stateCaptor.capture());
        assertThat(stateCaptor.getValue().isFavorite()).isTrue();
        assertThat(result.isFavorite()).isTrue();
        verify(eventPublisher).publish(new BidNoticeListChangedEvent(COMPANY_ID));
    }

    @Test
    @DisplayName("이미 관심 등록된 공고를 다시 등록하면 409를 던진다")
    void rejectsFavoritingAlreadyFavoritedNotice() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(collectedState().markFavorite(ANNOUNCED_AT)));

        assertError(
                () -> service.favorite(new FavoriteBidNoticeCommand(NOTICE_ID, USER_ID, "ADMIN")),
                BiddingErrorCode.BIDDING_NOTICE_ALREADY_FAVORITED
        );

        verify(companyStatePort, never()).update(any());
    }

    @Test
    @DisplayName("관심 등록된 공고를 해제한다")
    void unfavoritesCompanyNotice() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(collectedState().markFavorite(ANNOUNCED_AT)));

        var result = service.unfavorite(new UnfavoriteBidNoticeCommand(NOTICE_ID, USER_ID, "ADMIN"));

        ArgumentCaptor<CompanyBidNoticeState> stateCaptor =
                ArgumentCaptor.forClass(CompanyBidNoticeState.class);
        verify(companyStatePort).update(stateCaptor.capture());
        assertThat(stateCaptor.getValue().isFavorite()).isFalse();
        assertThat(result.isFavorite()).isFalse();
        verify(eventPublisher).publish(new BidNoticeListChangedEvent(COMPANY_ID));
    }

    @Test
    @DisplayName("관심 등록되지 않은 공고를 해제하면 409를 던진다")
    void rejectsUnfavoritingNotFavoritedNotice() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(collectedState()));

        assertError(
                () -> service.unfavorite(new UnfavoriteBidNoticeCommand(NOTICE_ID, USER_ID, "ADMIN")),
                BiddingErrorCode.BIDDING_NOTICE_NOT_FAVORITED
        );

        verify(companyStatePort, never()).update(any());
    }

    @Test
    @DisplayName("제외(DISMISSED)된 공고도 관심 등록할 수 있다 - notice_status와 독립적이다")
    void favoritesDismissedNoticeToo() {
        when(companyStatePort.findForUpdate(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(dismissedState()));

        var result = service.favorite(new FavoriteBidNoticeCommand(NOTICE_ID, USER_ID, "ADMIN"));

        assertThat(result.isFavorite()).isTrue();
        assertThat(result.noticeStatus()).isEqualTo("DISMISSED");
    }

    @Test
    @DisplayName("직접 등록 공고에 업로드 슬롯을 만들고 presigned URL을 발급한다")
    void startsAttachmentUpload() {
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existingNotice()));
        when(commandPort.countActiveAttachments(NOTICE_ID)).thenReturn(0L);
        when(commandPort.createPendingUpload(
                eq(NOTICE_ID), eq("제안요청서.pdf"), anyString(), eq(1024L), eq("application/pdf"), any()
        )).thenReturn(new PendingAttachmentUpload(501L, "제안요청서.pdf", "companies/10/bidding/notices/100/attachments/x", 1024L, true));
        Instant expiresAt = Instant.parse("2026-08-14T12:10:00Z");
        when(fileStoragePort.presignUpload(anyString(), eq("application/pdf"), eq(1024L)))
                .thenReturn(new FileStoragePort.PresignedUrl("https://s3.example/upload", expiresAt));

        BidNoticeAttachmentUploadStartResult result = service.startAttachmentUpload(
                new StartBidNoticeAttachmentUploadCommand(
                        NOTICE_ID, "제안요청서.pdf", "application/pdf", 1024L, USER_ID, "ADMIN"
                )
        );

        assertThat(result.attachmentId()).isEqualTo(501L);
        assertThat(result.uploadUrl()).isEqualTo("https://s3.example/upload");
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    @DisplayName("첨부가 이미 10개면 업로드 슬롯을 만들지 않는다")
    void rejectsStartingUploadWhenAttachmentLimitReached() {
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existingNotice()));
        when(commandPort.countActiveAttachments(NOTICE_ID)).thenReturn(10L);

        assertError(
                () -> service.startAttachmentUpload(new StartBidNoticeAttachmentUploadCommand(
                        NOTICE_ID, "제안요청서.pdf", "application/pdf", 1024L, USER_ID, "ADMIN"
                )),
                BiddingErrorCode.BIDDING_MANUAL_NOTICE_ATTACHMENT_LIMIT_EXCEEDED
        );

        verify(commandPort, never()).createPendingUpload(any(), any(), any(), anyLong(), any(), any());
        verifyNoInteractions(fileStoragePort);
    }

    @Test
    @DisplayName("실행 파일 확장자는 업로드 요청 자체를 거부한다")
    void rejectsBlockedExtensionOnUploadStart() {
        assertError(
                () -> service.startAttachmentUpload(new StartBidNoticeAttachmentUploadCommand(
                        NOTICE_ID, "설치파일.exe", "application/x-msdownload", 1024L, USER_ID, "ADMIN"
                )),
                BiddingErrorCode.BIDDING_INVALID_MANUAL_NOTICE
        );

        verifyNoInteractions(commandPort, fileStoragePort);
    }

    @Test
    @DisplayName("저장소 HEAD 검증에 성공하면 업로드를 완료 처리한다")
    void completesAttachmentUpload() {
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existingNotice()));
        when(commandPort.findPendingUpload(NOTICE_ID, 501L)).thenReturn(Optional.of(
                new PendingAttachmentUpload(501L, "제안요청서.pdf", "companies/10/bidding/notices/100/attachments/x", 1024L, true)
        ));
        when(fileStoragePort.head("companies/10/bidding/notices/100/attachments/x"))
                .thenReturn(Optional.of(new FileStoragePort.StoredObject(1024L)));

        BidNoticeAttachmentUploadCompleteResult result = service.completeAttachmentUpload(
                new CompleteBidNoticeAttachmentUploadCommand(NOTICE_ID, 501L, USER_ID, "ADMIN")
        );

        assertThat(result.attachmentId()).isEqualTo(501L);
        assertThat(result.fileName()).isEqualTo("제안요청서.pdf");
        assertThat(result.sizeBytes()).isEqualTo(1024L);
        verify(commandPort).completeUpload(eq(501L), eq(1024L), any());
        verify(commandPort, never()).failUploadInNewTransaction(any(), any());
    }

    @Test
    @DisplayName("저장소에 객체가 없으면 실패로 종료하고 409를 던진다")
    void failsUploadWhenObjectMissing() {
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existingNotice()));
        when(commandPort.findPendingUpload(NOTICE_ID, 501L)).thenReturn(Optional.of(
                new PendingAttachmentUpload(501L, "제안요청서.pdf", "companies/10/bidding/notices/100/attachments/x", 1024L, true)
        ));
        when(fileStoragePort.head("companies/10/bidding/notices/100/attachments/x"))
                .thenReturn(Optional.empty());

        assertError(
                () -> service.completeAttachmentUpload(
                        new CompleteBidNoticeAttachmentUploadCommand(NOTICE_ID, 501L, USER_ID, "ADMIN")
                ),
                BiddingErrorCode.BIDDING_MANUAL_NOTICE_ATTACHMENT_OBJECT_NOT_FOUND
        );

        verify(commandPort).failUploadInNewTransaction(eq(501L), any());
        verify(commandPort, never()).completeUpload(any(), anyLong(), any());
    }

    @Test
    @DisplayName("저장된 객체 크기가 요청과 다르면 실패로 종료하고 409를 던진다")
    void failsUploadWhenSizeMismatches() {
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existingNotice()));
        when(commandPort.findPendingUpload(NOTICE_ID, 501L)).thenReturn(Optional.of(
                new PendingAttachmentUpload(501L, "제안요청서.pdf", "companies/10/bidding/notices/100/attachments/x", 1024L, true)
        ));
        when(fileStoragePort.head("companies/10/bidding/notices/100/attachments/x"))
                .thenReturn(Optional.of(new FileStoragePort.StoredObject(999L)));

        assertError(
                () -> service.completeAttachmentUpload(
                        new CompleteBidNoticeAttachmentUploadCommand(NOTICE_ID, 501L, USER_ID, "ADMIN")
                ),
                BiddingErrorCode.BIDDING_MANUAL_NOTICE_ATTACHMENT_SIZE_MISMATCH
        );

        verify(commandPort).failUploadInNewTransaction(eq(501L), any());
    }

    @Test
    @DisplayName("이미 완료 처리된 업로드를 다시 완료 통보하면 409를 던진다")
    void rejectsCompletingAlreadyCompletedUpload() {
        when(commandPort.findOwnedManualNotice(COMPANY_ID, NOTICE_ID))
                .thenReturn(Optional.of(existingNotice()));
        when(commandPort.findPendingUpload(NOTICE_ID, 501L)).thenReturn(Optional.of(
                new PendingAttachmentUpload(501L, "제안요청서.pdf", "companies/10/bidding/notices/100/attachments/x", 1024L, false)
        ));

        assertError(
                () -> service.completeAttachmentUpload(
                        new CompleteBidNoticeAttachmentUploadCommand(NOTICE_ID, 501L, USER_ID, "ADMIN")
                ),
                BiddingErrorCode.BIDDING_MANUAL_NOTICE_ATTACHMENT_ALREADY_COMPLETED
        );

        verifyNoInteractions(fileStoragePort);
    }

    // 명세의 필수값과 대표 선택값을 포함한 유효한 등록 명령을 만듭니다.
    private CreateManualBidNoticeCommand validCreateCommand(String sourceUrl) {
        return new CreateManualBidNoticeCommand(
                " 스마트시티 구축 용역 ",
                BidNoticeType.SERVICE,
                " 서울특별시 ",
                "정보화담당관",
                InternationalBidType.DOMESTIC,
                ANNOUNCED_AT,
                null,
                DEADLINE_AT,
                null,
                new BigDecimal("300000000"),
                null,
                "전자입찰",
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                sourceUrl,
                List.of(new ManualBidNoticeAttachment(
                        9,
                        " 제안요청서.pdf ",
                        "https://example.org/rfp.pdf"
                )),
                USER_ID,
                "ADMIN"
        );
    }

    // 선택 필드 하나를 null로 해제하는 PATCH 명령을 만듭니다.
    private UpdateManualBidNoticeCommand updateDemandAgencyToNull() {
        return new UpdateManualBidNoticeCommand(
                NOTICE_ID,
                null,
                null,
                null,
                PatchField.of(null),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                USER_ID,
                "ADMIN"
        );
    }

    // 첨부 필드만 전달된 PATCH 명령을 만듭니다.
    private UpdateManualBidNoticeCommand updateAttachments(
            List<ManualBidNoticeAttachment> attachments
    ) {
        return new UpdateManualBidNoticeCommand(
                NOTICE_ID,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                null, null, PatchField.of(attachments), USER_ID, "ADMIN"
        );
    }

    // 수정 테스트에 사용할 현재 회사 소유 직접 등록 공고를 복원합니다.
    private ManualBidNotice existingNotice() {
        return ManualBidNotice.restore(
                NOTICE_ID,
                COMPANY_ID,
                3L,
                "MANUAL-existing",
                "00",
                "a".repeat(64),
                new ManualBidNoticeData(
                        "스마트시티 구축 용역",
                        BidNoticeType.SERVICE,
                        "서울특별시",
                        "기존 담당관",
                        InternationalBidType.DOMESTIC,
                        ANNOUNCED_AT,
                        null,
                        DEADLINE_AT,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        null,
                        null,
                        null,
                        List.of()
                ),
                "COLLECTED",
                USER_ID,
                ANNOUNCED_AT,
                null
        );
    }

    private ManualBidNotice existingNoticeWithAttachments() {
        ManualBidNotice existing = existingNotice();
        return ManualBidNotice.restore(
                existing.getNoticeId(), existing.getOwnerCompanyId(),
                existing.getCrawlSourceId(), existing.getExternalId(),
                existing.getNoticeOrder(), existing.getManualDedupKey(),
                new ManualBidNoticeData(
                        existing.getData().noticeName(), existing.getData().noticeType(),
                        existing.getData().noticeAgency(), existing.getData().demandAgency(),
                        existing.getData().internationalBidType(), existing.getData().announcedAt(),
                        existing.getData().bidStartAt(), existing.getData().bidDeadlineAt(),
                        existing.getData().openingAt(), existing.getData().baseAmount(),
                        existing.getData().estimatedAmount(), existing.getData().bidMethod(),
                        existing.getData().contractMethod(),
                        existing.getData().participationQualificationText(),
                        existing.getData().regionLimitText(), existing.getData().businessLimitText(),
                        existing.getData().jointContractAllowed(), existing.getData().jointContractText(),
                        existing.getData().evaluationMethod(), existing.getData().sourceUrl(),
                        List.of(new ManualBidNoticeAttachment(
                                1, "기존첨부.pdf", "https://example.org/old.pdf"
                        ))
                ),
                existing.getNoticeStatus(), existing.getCreatedBy(),
                existing.getCreatedAt(), existing.getUpdatedAt()
        );
    }

    private CompanyBidNoticeState collectedState() {
        return new CompanyBidNoticeState(
                COMPANY_ID,
                NOTICE_ID,
                BidNoticeCompanyStatus.COLLECTED,
                null,
                false,
                ANNOUNCED_AT
        );
    }

    private CompanyBidNoticeState dismissedState() {
        return new CompanyBidNoticeState(
                COMPANY_ID,
                NOTICE_ID,
                BidNoticeCompanyStatus.DISMISSED,
                "기존 제외 사유",
                false,
                ANNOUNCED_AT
        );
    }

    private ManualBidNotice captureSavedNotice() {
        ArgumentCaptor<ManualBidNotice> captor =
                ArgumentCaptor.forClass(ManualBidNotice.class);
        verify(commandPort).save(captor.capture());
        return captor.getValue();
    }

    // 저장 전 도메인 객체에 DB 생성 ID가 반영된 상황을 모의합니다.
    private ManualBidNotice withId(ManualBidNotice notice) {
        if (notice.getNoticeId() != null) {
            return notice;
        }
        return ManualBidNotice.restore(
                NOTICE_ID,
                notice.getOwnerCompanyId(),
                notice.getCrawlSourceId(),
                notice.getExternalId(),
                notice.getNoticeOrder(),
                notice.getManualDedupKey(),
                notice.getData(),
                notice.getNoticeStatus(),
                notice.getCreatedBy(),
                notice.getCreatedAt(),
                notice.getUpdatedAt()
        );
    }

    private void assertError(Runnable action, BiddingErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(DomainException.class)
                .satisfies(exception -> assertThat(
                        ((DomainException) exception).getErrorCode()
                ).isEqualTo(expected));
    }
}
