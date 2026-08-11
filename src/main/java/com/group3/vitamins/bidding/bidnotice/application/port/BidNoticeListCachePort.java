package com.group3.vitamins.bidding.bidnotice.application.port;

import com.group3.vitamins.bidding.bidnotice.application.query.SearchBidNoticesQuery;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeListResult;

import java.util.Optional;

public interface BidNoticeListCachePort {

    // 현재 회사와 검색 조건에 해당하는 캐시 값과 조회 당시 버전을 반환합니다.
    CacheLookup lookup(Long companyId, SearchBidNoticesQuery query);

    // 조회 중 무효화가 발생해도 새 버전을 오염시키지 않도록 조회 당시 버전에 저장합니다.
    void put(
            Long companyId,
            SearchBidNoticesQuery query,
            String version,
            BidNoticeListResult result
    );

    // 회사의 캐시 버전을 증가시키고 성공 여부를 반환합니다.
    boolean invalidate(Long companyId);

    record CacheLookup(String version, Optional<BidNoticeListResult> result) {

        public static CacheLookup unavailable() {
            return new CacheLookup(null, Optional.empty());
        }
    }
}
