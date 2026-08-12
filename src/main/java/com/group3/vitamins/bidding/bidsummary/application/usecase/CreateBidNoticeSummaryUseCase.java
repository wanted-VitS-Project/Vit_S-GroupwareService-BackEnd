package com.group3.vitamins.bidding.bidsummary.application.usecase;

import com.group3.vitamins.bidding.bidsummary.application.command.CreateBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.result.CreateBidNoticeSummaryResult;

public interface CreateBidNoticeSummaryUseCase {

    // 입찰 공고 AI 요약 요청을 생성합니다.
    CreateBidNoticeSummaryResult create(CreateBidNoticeSummaryCommand command);
}