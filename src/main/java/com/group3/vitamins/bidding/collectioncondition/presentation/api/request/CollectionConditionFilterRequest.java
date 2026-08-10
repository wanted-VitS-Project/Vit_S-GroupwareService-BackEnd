package com.group3.vitamins.bidding.collectioncondition.presentation.api.request;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CollectionConditionFilterRequest(

        @Schema(description = "검색 키워드 목록", example = "[\"스마트시티\", \"통합관제\"]")
        List<String> keywords,

        @Schema(description = "나라장터 지역 코드 목록", example = "[\"11\", \"41\"]")
        List<String> regionCodes,

        @Schema(description = "나라장터 업종 코드 목록", example = "[\"6202\"]")
        List<String> industryCodes,

        @Schema(description = "최소 추정가격", example = "100000000")
        Long minimumEstimatedPrice,

        @Schema(description = "최대 추정가격", example = "1000000000")
        Long maximumEstimatedPrice,

        @Schema(description = "마감된 공고 제외 여부", example = "true")
        boolean excludeClosed,

        @Schema(description = "국내·국제 입찰 구분", example = "DOMESTIC")
        InternationalBidType internationalBidType
) {

    // HTTP 필터 요청을 수집 조건 도메인 값 객체로 변환합니다.
    public CollectionConditionFilter toDomain() {
        return new CollectionConditionFilter(
                keywords,
                regionCodes,
                industryCodes,
                minimumEstimatedPrice,
                maximumEstimatedPrice,
                excludeClosed,
                internationalBidType
        );
    }
}