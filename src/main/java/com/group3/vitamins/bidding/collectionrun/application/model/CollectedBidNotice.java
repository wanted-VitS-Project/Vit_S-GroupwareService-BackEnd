package com.group3.vitamins.bidding.collectionrun.application.model;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 외부 수집처의 응답을 정규화한 입찰 공고입니다.
public record CollectedBidNotice(
        String externalId,
        String noticeOrder,
        BidNoticeType noticeType,
        String noticeName,
        String noticeAgency,
        String demandAgency,
        String externalNoticeStatus,
        String internationalBidType,
        LocalDateTime announcedAt,
        LocalDateTime bidStartAt,
        LocalDateTime bidDeadlineAt,
        LocalDateTime openingAt,
        BigDecimal baseAmount,
        BigDecimal estimatedAmount,
        String bidMethod,
        String contractMethod,
        String participationQualificationText,
        Boolean jointContractAllowed,
        String jointContractText,
        String sourceUrl,
        List<Attachment> attachments
) {

    public CollectedBidNotice {
        attachments = attachments == null
                ? List.of()
                : List.copyOf(attachments);
    }

    // 공고에 저장할 수 있는 외부 첨부파일이 있는지 확인합니다.
    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    // 수집처에서 제공한 공고 첨부파일입니다.
    public record Attachment(
            int order,
            String fileName,
            String sourceUrl
    ) {
    }
}