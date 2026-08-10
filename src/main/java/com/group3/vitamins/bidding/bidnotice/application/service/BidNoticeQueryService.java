package com.group3.vitamins.bidding.bidnotice.application.service;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeQueryPort;
import com.group3.vitamins.bidding.bidnotice.application.query.GetBidNoticeDetailQuery;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.*;
import com.group3.vitamins.bidding.bidnotice.application.usecase.BidNoticeQueryUseCase;
import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BidNoticeQueryService implements BidNoticeQueryUseCase {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORTS = Set.of(
            "ANNOUNCED_DESC", "DEADLINE_ASC", "AMOUNT_DESC"
    );
    private static final Set<String> ALLOWED_STATUSES = Set.of("COLLECTED", "DISMISSED");

    private final BidNoticeQueryPort queryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final BiddingAccessPolicy biddingAccessPolicy;

    // 현재 회사에 귀속된 공고만 검증된 조건과 페이지 범위로 조회합니다.
    @Override
    public BidNoticeListResult handle(SearchBidNoticesQuery query) {
        validateListQuery(query);
        biddingAccessPolicy.assertAccess(query.userId(), query.role());
        Long companyId = currentCompanyIdProvider.currentCompanyId();

        List<BidNoticeListItemResult> content = queryPort.findAll(companyId, query);
        long totalElements = queryPort.count(companyId, query);
        int totalPages = (int) Math.ceil((double) totalElements / query.size());
        return new BidNoticeListResult(
                content, totalElements, totalPages, query.page(), query.size()
        );
    }

    // 다른 회사 공고를 404로 숨기면서 단건 상세를 조회합니다.
    @Override
    public BidNoticeDetailResult handle(GetBidNoticeDetailQuery query) {
        if (query == null || query.noticeId() == null || query.noticeId() <= 0
                || isBlank(query.userId())) {
            throw invalidQuery();
        }
        biddingAccessPolicy.assertAccess(query.userId(), query.role());
        Long companyId = currentCompanyIdProvider.currentCompanyId();
        return queryPort.findDetail(companyId, query.noticeId())
                .orElseThrow(() -> new NotFoundException(
                        BiddingErrorCode.BIDDING_NOTICE_NOT_FOUND
                ));
    }

    private void validateListQuery(SearchBidNoticesQuery query) {
        if (query == null || query.page() < 0 || query.size() <= 0
                || query.size() > MAX_PAGE_SIZE || isBlank(query.userId())
                || (query.startDate() != null && query.endDate() != null
                && query.startDate().isAfter(query.endDate()))
                || (query.businessCategoryId() != null && query.businessCategoryId() <= 0)
                || (!isBlank(query.noticeStatus())
                && !ALLOWED_STATUSES.contains(query.noticeStatus()))
                || (!isBlank(query.sort()) && !ALLOWED_SORTS.contains(query.sort()))) {
            throw invalidQuery();
        }
    }

    private ValidationException invalidQuery() {
        return new ValidationException(BiddingErrorCode.BIDDING_INVALID_NOTICE_QUERY);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
