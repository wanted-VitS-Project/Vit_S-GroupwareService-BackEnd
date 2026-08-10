package com.group3.vitamins.bidding.collectioncondition.domain.model;

import java.util.List;

// 입찰 공고 수집에 사용하는 검색 필터를 묶습니다.
public record CollectionConditionFilter(
        List<String> keywords,
        List<String> regionCodes,
        List<String> industryCodes,
        Long minimumEstimatedPrice,
        Long maximumEstimatedPrice,
        boolean excludeClosed,
        InternationalBidType internationalBidType
) {

    public CollectionConditionFilter {
        keywords = immutableList(keywords);
        regionCodes = immutableList(regionCodes);
        industryCodes = immutableList(industryCodes);
    }

    // 외부에서 전달된 목록의 변경이 도메인 내부에 영향을 주지 않게 합니다.
    private static List<String> immutableList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}