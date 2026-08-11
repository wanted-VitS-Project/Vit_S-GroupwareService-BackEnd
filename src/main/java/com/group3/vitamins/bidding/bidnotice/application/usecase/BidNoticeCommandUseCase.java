package com.group3.vitamins.bidding.bidnotice.application.usecase;

import com.group3.vitamins.bidding.bidnotice.application.command.CreateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.UpdateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.result.ManualBidNoticeResult;

// 직접 등록 입찰 공고의 생성과 부분 수정을 제공하는 유스케이스 계약입니다.
public interface BidNoticeCommandUseCase {

    // 현재 회사 소유의 직접 등록 공고를 생성합니다.
    ManualBidNoticeResult create(CreateManualBidNoticeCommand command);

    // 현재 회사가 직접 등록한 공고의 전달된 필드만 수정합니다.
    ManualBidNoticeResult update(UpdateManualBidNoticeCommand command);
}
