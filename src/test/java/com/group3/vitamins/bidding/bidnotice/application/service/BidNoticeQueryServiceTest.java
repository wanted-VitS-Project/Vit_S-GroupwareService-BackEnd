package com.group3.vitamins.bidding.bidnotice.application.service;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeQueryPort;
import com.group3.vitamins.bidding.bidnotice.application.query.GetBidNoticeDetailQuery;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeDetailResult;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidNoticeQueryServiceTest {

    private static final Long COMPANY_ID = 10L;

    @Mock private BidNoticeQueryPort queryPort;
    @Mock private CurrentCompanyIdProvider companyIdProvider;
    @Mock private BiddingAccessPolicy accessPolicy;
    private BidNoticeQueryService service;

    @BeforeEach
    void setUp() {
        service = new BidNoticeQueryService(queryPort, companyIdProvider, accessPolicy);
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
