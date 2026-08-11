package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "직접 등록 공고 부분 수정 요청")
public record UpdateManualBidNoticeRequest(
        @Schema(nullable = true) String noticeName,
        @Schema(nullable = true) BidNoticeType noticeType,
        @Schema(nullable = true) String noticeAgency,
        @Schema(nullable = true) String demandAgency,
        @Schema(nullable = true) InternationalBidType internationalBidType,
        @Schema(nullable = true) LocalDateTime announcedAt,
        @Schema(nullable = true) LocalDateTime bidStartAt,
        @Schema(nullable = true) LocalDateTime bidDeadlineAt,
        @Schema(nullable = true) LocalDateTime openingAt,
        @Schema(nullable = true) BigDecimal baseAmount,
        @Schema(nullable = true) BigDecimal estimatedAmount,
        @Schema(nullable = true) String bidMethod,
        @Schema(nullable = true) String contractMethod,
        @Schema(nullable = true) String participationQualificationText,
        @Schema(nullable = true) String regionLimitText,
        @Schema(nullable = true) String businessLimitText,
        @Schema(nullable = true) Boolean jointContractAllowed,
        @Schema(nullable = true) String jointContractText,
        @Schema(nullable = true) String evaluationMethod,
        @Schema(nullable = true) String sourceUrl,
        @Schema(nullable = true)
        List<ManualBidNoticeAttachmentRequest> attachments
) {
}