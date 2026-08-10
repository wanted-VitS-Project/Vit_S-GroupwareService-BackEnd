package com.group3.vitamins.bidding.collectioncondition.application.command;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;

import java.util.List;

// 기존 입찰 공고 수집 조건 수정에 필요한 입력값입니다.
public record UpdateCollectionConditionCommand(
        Long conditionId,
        String conditionName,
        List<BidNoticeType> noticeTypes,
        CollectionConditionFilter filters,
        Boolean active,
        String userId
) {
}