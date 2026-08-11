package com.group3.vitamins.bidding.bidnotice.application.port;

import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeDetailResult;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListItemResult;

import java.util.List;
import java.util.Optional;

public interface BidNoticeQueryPort {

    // 현재 회사 상태와 결합한 공고 목록을 페이지 단위로 조회합니다.
    List<BidNoticeListItemResult> findAll(Long companyId, SearchBidNoticesQuery query);

    // 같은 필터 조건의 전체 공고 수를 조회합니다.
    long count(Long companyId, SearchBidNoticesQuery query);

    // 현재 회사가 수집한 공고의 상세 정보만 조회합니다.
    Optional<BidNoticeDetailResult> findDetail(Long companyId, Long noticeId);
}
