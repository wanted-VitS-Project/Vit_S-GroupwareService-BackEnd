package com.group3.vitamins.bidding.collectioncondition.presentation.api.response;

import com.group3.vitamins.bidding.collectioncondition.application.result.CollectionConditionResult;
import com.group3.vitamins.bidding.collectioncondition.presentation.api.response.CollectionConditionResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record CollectionConditionListResponse(

        @Schema(description = "수집 조건 목록")
        List<CollectionConditionResponse> content
) {

    // 수집 조건 결과 목록을 API 목록 응답으로 변환합니다.
    public static CollectionConditionListResponse from(
            List<CollectionConditionResult> results
    ) {
        return new CollectionConditionListResponse(
                results.stream()
                        .map(CollectionConditionResponse::from)
                        .toList()
        );
    }
}