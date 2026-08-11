package com.group3.vitamins.bidding.bidnotice.domain.model;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 직접 등록 공고에서 사용자가 입력하고 수정할 수 있는 내용을 묶습니다.
public record ManualBidNoticeData(
        String noticeName,
        BidNoticeType noticeType,
        String noticeAgency,
        String demandAgency,
        InternationalBidType internationalBidType,
        LocalDateTime announcedAt,
        LocalDateTime bidStartAt,
        LocalDateTime bidDeadlineAt,
        LocalDateTime openingAt,
        BigDecimal baseAmount,
        BigDecimal estimatedAmount,
        String bidMethod,
        String contractMethod,
        String participationQualificationText,
        String regionLimitText,
        String businessLimitText,
        Boolean jointContractAllowed,
        String jointContractText,
        String evaluationMethod,
        String sourceUrl,
        List<ManualBidNoticeAttachment> attachments
) {
    public ManualBidNoticeData {
        attachments = attachments == null
                ? List.of()
                : List.copyOf(attachments);
    }
}
