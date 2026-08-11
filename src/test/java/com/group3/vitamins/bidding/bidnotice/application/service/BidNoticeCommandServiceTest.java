package com.group3.vitamins.bidding.bidnotice.application.service;

import com.group3.vitamins.bidding.bidnotice.application.command.CreateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.PatchField;
import com.group3.vitamins.bidding.bidnotice.application.command.UpdateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeCommandPort;
import com.group3.vitamins.bidding.bidnotice.application.port.CompanyBidNoticeStatePort;
import com.group3.vitamins.bidding.bidnotice.application.support.ManualBidNoticeDedupKeyGenerator;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNotice;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeData;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
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
    private CurrentCompanyIdProvider companyIdProvider;
    private BiddingAccessPolicy biddingAccessPolicy;
    private BidNoticeCommandService service;

    @BeforeEach
    void setUp() {
        commandPort = mock(BidNoticeCommandPort.class);
        companyStatePort = mock(CompanyBidNoticeStatePort.class);
        companyIdProvider = mock(CurrentCompanyIdProvider.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(commandPort.findManualSourceId()).thenReturn(Optional.of(3L));
        when(commandPort.save(any(ManualBidNotice.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        service = new BidNoticeCommandService(
                commandPort,
                companyStatePort,
                companyIdProvider,
                biddingAccessPolicy,
                new ManualBidNoticeDedupKeyGenerator()
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
