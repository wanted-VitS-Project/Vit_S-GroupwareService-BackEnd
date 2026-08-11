package com.group3.vitamins.bidding.bidnotice.application.command;

import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeAttachment;
import com.group3.vitamins.bidding.bidnotice.domain.model.ManualBidNoticeData;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 현재 회사에 직접 등록할 입찰 공고 입력값입니다.
public record CreateManualBidNoticeCommand(
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
        List<ManualBidNoticeAttachment> attachments,
        String userId,
        String role
) {
    // 서비스가 검증할 직접 등록 공고 내용으로 변환합니다.
    public ManualBidNoticeData toData() {
        return new ManualBidNoticeData(
                noticeName,
                noticeType,
                noticeAgency,
                demandAgency,
                internationalBidType,
                announcedAt,
                bidStartAt,
                bidDeadlineAt,
                openingAt,
                baseAmount,
                estimatedAmount,
                bidMethod,
                contractMethod,
                participationQualificationText,
                regionLimitText,
                businessLimitText,
                jointContractAllowed,
                jointContractText,
                evaluationMethod,
                sourceUrl,
                attachments
        );
    }
}
