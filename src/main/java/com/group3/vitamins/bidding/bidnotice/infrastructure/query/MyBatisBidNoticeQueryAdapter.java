package com.group3.vitamins.bidding.bidnotice.infrastructure.query;

import com.group3.vitamins.bidding.bidnotice.application.port.BidNoticeQueryPort;
import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeDetailResult;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListItemResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MyBatisBidNoticeQueryAdapter implements BidNoticeQueryPort {

    private final BidNoticeQueryMapper mapper;

    @Override
    public List<BidNoticeListItemResult> findAll(Long companyId, SearchBidNoticesQuery query) {
        return mapper.findAll(companyId, query, query.page() * query.size());
    }

    @Override
    public long count(Long companyId, SearchBidNoticesQuery query) {
        return mapper.count(companyId, query);
    }

    // 상세 본문이 존재할 때만 첨부 목록을 추가해 완성된 조회 결과를 반환합니다.
    @Override
    public Optional<BidNoticeDetailResult> findDetail(Long companyId, Long noticeId) {
        BidNoticeDetailRow detail = mapper.findDetail(companyId, noticeId);
        if (detail == null) {
            return Optional.empty();
        }
        return Optional.of(new BidNoticeDetailResult(
                detail.noticeId(), detail.externalId(), detail.noticeOrder(), detail.noticeName(),
                detail.noticeType(), detail.externalNoticeStatus(), detail.noticeAgency(),
                detail.demandAgency(), detail.noticeStatus(), detail.dismissReason(), detail.projectId(),
                detail.sourceCode(), detail.sourceName(), detail.sourceUrl(), detail.hasAttachment(),
                detail.announcedAt(), detail.bidStartAt(), detail.questionDeadlineAt(),
                detail.applicationDeadlineAt(), detail.bidDeadlineAt(), detail.openingAt(), detail.dDay(),
                detail.baseAmount(), detail.estimatedAmount(), detail.priceRangeText(),
                detail.minimumBidRateText(), detail.participationQualificationText(),
                detail.regionLimitText(), detail.businessLimitText(), detail.jointContractAllowed(),
                detail.jointContractText(), detail.contractMethod(), detail.evaluationMethod(),
                mapper.findAttachments(noticeId)
        ));
    }
}
