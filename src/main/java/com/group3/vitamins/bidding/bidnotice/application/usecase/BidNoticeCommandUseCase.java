package com.group3.vitamins.bidding.bidnotice.application.usecase;

import com.group3.vitamins.bidding.bidnotice.application.command.CompleteBidNoticeAttachmentUploadCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.CreateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.FavoriteBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.StartBidNoticeAttachmentUploadCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.UnfavoriteBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.UpdateManualBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.DismissBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.command.RestoreBidNoticeCommand;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeAttachmentUploadCompleteResult;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeAttachmentUploadStartResult;
import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeStatusResult;
import com.group3.vitamins.bidding.bidnotice.application.result.ManualBidNoticeResult;

// 직접 등록 입찰 공고의 생성과 부분 수정을 제공하는 유스케이스 계약입니다.
public interface BidNoticeCommandUseCase {

    // 현재 회사 소유의 직접 등록 공고를 생성합니다.
    ManualBidNoticeResult create(CreateManualBidNoticeCommand command);

    // 현재 회사가 직접 등록한 공고의 전달된 필드만 수정합니다.
    ManualBidNoticeResult update(UpdateManualBidNoticeCommand command);

    BidNoticeStatusResult dismiss(DismissBidNoticeCommand command);

    BidNoticeStatusResult restore(RestoreBidNoticeCommand command);

    // 현재 회사 공용 관심 목록에 공고를 등록/해제합니다(어느 직원이 눌러도 회사 전체에 동일하게 반영).
    BidNoticeStatusResult favorite(FavoriteBidNoticeCommand command);

    BidNoticeStatusResult unfavorite(UnfavoriteBidNoticeCommand command);

    // 직접 등록 공고에 실제 파일을 업로드할 첨부 슬롯을 만들고 presigned PUT URL을 발급합니다.
    BidNoticeAttachmentUploadStartResult startAttachmentUpload(StartBidNoticeAttachmentUploadCommand command);

    // 업로드 완료를 통보받아 저장소 HEAD로 검증하고 첨부를 사용 가능 상태로 반영합니다.
    BidNoticeAttachmentUploadCompleteResult completeAttachmentUpload(CompleteBidNoticeAttachmentUploadCommand command);
}
