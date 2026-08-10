package com.group3.vitamins.bidding.bidnotice.application.service;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeQueryPort;
import com.group3.vitamins.bidding.bidnotice.application.query.GetBidNoticeDetailQuery;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeDetailResult;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListItemResult;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidNoticeQueryServiceTest {

    private static final Long COMPANY_ID = 10L;

    @Mock private BidNoticeQueryPort queryPort;
    @Mock private CurrentCompanyIdProvider companyIdProvider;
    @Mock private BiddingAccessPolicy accessPolicy;
    private BidNoticeQueryService service;
    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2026-08-11T00:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        service = new BidNoticeQueryService(queryPort, companyIdProvider, accessPolicy, clock);
    }

    @Test
    void rejectsPageThatWouldOverflowOffset() {
        SearchBidNoticesQuery query = new SearchBidNoticesQuery(
                null, null, null, null, null, null, null, null,
                "ANNOUNCED_DESC", Integer.MAX_VALUE / 100 + 1, 100,
                "EMP001", "ADMIN"
        );

        assertThatThrownBy(() -> service.handle(query))
                .isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(((ValidationException) exception).getErrorCode())
                        .isEqualTo(BiddingErrorCode.BIDDING_INVALID_NOTICE_QUERY));

        verifyNoInteractions(queryPort, companyIdProvider, accessPolicy);
    }

    @Test
    void calculatesListDDayWithApplicationClock() {
        SearchBidNoticesQuery query = new SearchBidNoticesQuery(
                null, null, null, null, null, null, null, null,
                "ANNOUNCED_DESC", 0, 20, "EMP001", "ADMIN"
        );
        BidNoticeListItemResult item = new BidNoticeListItemResult(
                1L, "공고", "NARA", "나라장터", null, "기관", null, null,
                BigDecimal.ONE, BigDecimal.TEN, null,
                LocalDateTime.of(2026, 8, 14, 18, 0), null,
                false, "COLLECTED", null
        );
        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(queryPort.findAll(COMPANY_ID, query)).thenReturn(List.of(item));
        when(queryPort.count(COMPANY_ID, query)).thenReturn(1L);

        var result = service.handle(query);

        assertThat(result.content().get(0).dDay()).isEqualTo(3);
    }

    @Test
    void rejectsListWhenAccessIsDeniedWithoutCallingQueryPort() {
        SearchBidNoticesQuery query = new SearchBidNoticesQuery(
                null, null, null, null, null, null, null, null,
                "ANNOUNCED_DESC", 0, 20, "EMP001", "USER"
        );
        doThrow(new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED))
                .when(accessPolicy).assertAccess("EMP001", "USER");

        assertThatThrownBy(() -> service.handle(query)).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(queryPort, companyIdProvider);
    }

    @Test
    void rejectsDetailWhenAccessIsDeniedWithoutCallingQueryPort() {
        doThrow(new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED))
                .when(accessPolicy).assertAccess("EMP001", "USER");

        assertThatThrownBy(() -> service.handle(
                new GetBidNoticeDetailQuery(100L, "EMP001", "USER")
        )).isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(queryPort, companyIdProvider);
    }

    @Test
    void listQueryUsesCurrentCompanyId() {
        SearchBidNoticesQuery query = new SearchBidNoticesQuery(
                null, null, null, null, null, null, null, null,
                "ANNOUNCED_DESC", 0, 20, "EMP001", "ADMIN"
        );
        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(queryPort.findAll(COMPANY_ID, query)).thenReturn(List.of());
        when(queryPort.count(COMPANY_ID, query)).thenReturn(0L);

        service.handle(query);

        verify(queryPort).findAll(COMPANY_ID, query);
        verify(queryPort).count(COMPANY_ID, query);
    }

    @Test
    void inaccessibleCompanyNoticeIsHiddenAsNotFound() {
        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(queryPort.findDetail(COMPANY_ID, 100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(
                new GetBidNoticeDetailQuery(100L, "EMP001", "ADMIN")
        )).isInstanceOf(NotFoundException.class);

        verify(queryPort).findDetail(COMPANY_ID, 100L);
    }
}
