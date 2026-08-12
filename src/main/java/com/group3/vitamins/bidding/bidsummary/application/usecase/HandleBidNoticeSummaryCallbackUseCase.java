package com.group3.vitamins.bidding.bidsummary.application.usecase;

import com.group3.vitamins.bidding.bidsummary.application.command
        .HandleBidNoticeSummaryCallbackCommand;
import com.group3.vitamins.bidding.bidsummary.application.result
        .BidNoticeSummaryCallbackResult;

public interface HandleBidNoticeSummaryCallbackUseCase {

    // Python worker가 전달한 완료 또는 실패 결과를 반영합니다.
    BidNoticeSummaryCallbackResult handle(
            HandleBidNoticeSummaryCallbackCommand command
    );
}