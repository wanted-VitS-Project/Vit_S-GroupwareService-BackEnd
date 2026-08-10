package com.group3.vitamins.bidding.collectioncondition.application.usecase;

import com.group3.vitamins.bidding.collectioncondition.application.command.CreateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.application.command.UpdateCollectionConditionCommand;
import com.group3.vitamins.bidding.collectioncondition.application.result.CollectionConditionResult;

import java.util.List;

public interface CollectionConditionUseCase {

    // 현재 회사에서 접근 권한이 있는 사용자의 수집 조건만 조회합니다.
    List<CollectionConditionResult> getAll(String userId, String role);

    // 새로운 수집 조건을 등록합니다.
    CollectionConditionResult create(CreateCollectionConditionCommand command);

    // 기존 수집 조건의 수정 가능한 정보를 변경합니다.
    CollectionConditionResult update(UpdateCollectionConditionCommand command);


}
