package com.group3.vitamins.bidding.bidsummary.application.usecase;

import com.group3.vitamins.bidding.bidsummary.application.query
        .GetBidNoticeSummaryJobQuery;
import com.group3.vitamins.bidding.bidsummary.application.result
        .BidNoticeSummaryJobResult;

public interface GetBidNoticeSummaryJobUseCase {

    // 현재 시도와 일치하는 AI 요약 작업을 조회하고 점유합니다.
    BidNoticeSummaryJobResult handle(GetBidNoticeSummaryJobQuery query);
}