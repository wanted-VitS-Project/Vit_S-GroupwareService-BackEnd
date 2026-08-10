package com.group3.vitamins.bidding.collectionrun.application.usecase;

import com.group3.vitamins.bidding.collectionrun.application.command.StartCollectionRunCommand;
import com.group3.vitamins.bidding.collectionrun.application.query.GetCollectionRunQuery;
import com.group3.vitamins.bidding.collectionrun.application.result.CollectionRunResult;

public interface CollectionRunUseCase {

    // 현재 회사의 수집 조건으로 비동기 수집 실행을 요청합니다.
    CollectionRunResult start(StartCollectionRunCommand command);

    // 현재 회사가 소유한 수집 실행 결과를 조회합니다.
    CollectionRunResult get(GetCollectionRunQuery query);
}
