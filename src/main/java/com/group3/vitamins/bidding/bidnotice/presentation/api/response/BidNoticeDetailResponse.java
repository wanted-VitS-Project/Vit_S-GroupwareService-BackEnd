package com.group3.vitamins.bidding.bidnotice.presentation.api.response;

import com.group3.vitamins.bidding.bidnotice.application.result.BidNoticeDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "입찰 공고 상세 응답")
public record BidNoticeDetailResponse(
        Long noticeId, String externalId, String noticeOrder, String noticeName,
        String noticeType, String externalNoticeStatus, String noticeAgency,
        String demandAgency, String noticeStatus, String dismissReason, Long projectId,
        String sourceCode, String sourceName, String sourceUrl, boolean hasAttachment,
        LocalDateTime announcedAt, LocalDateTime bidStartAt,
        LocalDateTime questionDeadlineAt, LocalDateTime applicationDeadlineAt,
        LocalDateTime bidDeadlineAt, LocalDateTime openingAt, Integer dDay,
        BigDecimal baseAmount, BigDecimal estimatedAmount, String priceRangeText,
        String minimumBidRateText, String participationQualificationText,
        String regionLimitText, String businessLimitText, Boolean jointContractAllowed,
        String jointContractText, String contractMethod, String evaluationMethod,
        List<Attachment> attachments
) {
    public record Attachment(Short attachmentOrder, String fileName, String sourceUrl) {
        static Attachment from(BidNoticeDetailResult.Attachment result) {
            return new Attachment(result.attachmentOrder(), result.fileName(), result.sourceUrl());
        }
    }

    public static BidNoticeDetailResponse from(BidNoticeDetailResult result) {
        return new BidNoticeDetailResponse(
                result.noticeId(), result.externalId(), result.noticeOrder(), result.noticeName(),
                result.noticeType(), result.externalNoticeStatus(), result.noticeAgency(),
                result.demandAgency(), result.noticeStatus(), result.dismissReason(), result.projectId(),
                result.sourceCode(), result.sourceName(), result.sourceUrl(), result.hasAttachment(),
                result.announcedAt(), result.bidStartAt(), result.questionDeadlineAt(),
                result.applicationDeadlineAt(), result.bidDeadlineAt(), result.openingAt(), result.dDay(),
                result.baseAmount(), result.estimatedAmount(), result.priceRangeText(),
                result.minimumBidRateText(), result.participationQualificationText(),
                result.regionLimitText(), result.businessLimitText(), result.jointContractAllowed(),
                result.jointContractText(), result.contractMethod(), result.evaluationMethod(),
                result.attachments().stream().map(Attachment::from).toList()
        );
    }
}
