package com.group3.vitamins.bidding.collectioncondition.presentation.api.response;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CollectionConditionFilterResponse(

        @Schema(description = "공고 검색 키워드 목록")
        List<String> keywords,

        @Schema(description = "지역 코드 목록")
        List<String> regionCodes,

        @Schema(description = "업종 코드 목록")
        List<String> industryCodes,

        @Schema(description = "최소 추정가격")
        Long minimumEstimatedPrice,

        @Schema(description = "최대 추정가격")
        Long maximumEstimatedPrice,

        @Schema(description = "마감 공고 제외 여부")
        boolean excludeClosed,

        @Schema(description = "국내·국제 입찰 구분")
        InternationalBidType internationalBidType
) {

    // 도메인의 필터 값을 API 응답 형식으로 변환합니다.
    public static CollectionConditionFilterResponse from(
            CollectionConditionFilter filter
    ) {
        return new CollectionConditionFilterResponse(
                filter.keywords(),
                filter.regionCodes(),
                filter.industryCodes(),
                filter.minimumEstimatedPrice(),
                filter.maximumEstimatedPrice(),
                filter.excludeClosed(),
                filter.internationalBidType()
        );
    }
}