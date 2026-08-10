package com.group3.vitamins.bidding.collectioncondition.presentation.api.request;

import com.group3.vitamins.bidding.collectioncondition.application.command.UpdateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UpdateCollectionConditionRequest(

        @Schema(description = "수집 조건명", example = "수도권 스마트시티 공사·용역")
        String conditionName,

        @Schema(
                description = "수집할 공고 종류",
                example = "[\"CONSTRUCTION\", \"SERVICE\"]"
        )
        List<BidNoticeType> noticeTypes,

        @Schema(description = "나라장터 검색 필터")
        CollectionConditionFilterRequest filters,

        @Schema(description = "수집 조건 활성화 여부", example = "true")
        Boolean isActive
) {

    // Path의 조건 ID와 인증 사용자를 수정 Command로 변환합니다.
    public UpdateCollectionConditionCommand toCommand(
            Long conditionId,
            String userId,
            String role
    ) {
        return new UpdateCollectionConditionCommand(
                conditionId,
                conditionName,
                noticeTypes,
                filters == null ? null : filters.toDomain(),
                isActive,
                userId,
                role
        );
    }
}
