package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.group3.vitamins.bidding.bidnotice.application.command.CreateManualBidNoticeCommand;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

public record CreateManualBidNoticeRequest(
        @Schema(description = "공고명", example = "스마트시티 통합관제 플랫폼 구축 용역")
        String noticeName,
        @Schema(description = "공고 유형", example = "SERVICE")
        BidNoticeType noticeType,
        @Schema(description = "공고기관", example = "서울특별시")
        String noticeAgency,
        @Schema(description = "수요기관", nullable = true)
        String demandAgency,
        @Schema(description = "국제입찰 구분", nullable = true)
        InternationalBidType internationalBidType,
        @Schema(description = "공고일시")
        LocalDateTime announcedAt,
        @Schema(description = "입찰개시일시", nullable = true)
        LocalDateTime bidStartAt,
        @Schema(description = "입찰마감일시")
        LocalDateTime bidDeadlineAt,
        @Schema(description = "개찰일시", nullable = true)
        LocalDateTime openingAt,
        @Schema(description = "기초금액", nullable = true)
        BigDecimal baseAmount,
        @Schema(description = "추정금액", nullable = true)
        BigDecimal estimatedAmount,
        @Schema(description = "입찰 방식", nullable = true)
        String bidMethod,
        @Schema(description = "계약 방식", nullable = true)
        String contractMethod,
        @Schema(description = "참가 자격", nullable = true)
        String participationQualificationText,
        @Schema(description = "지역 제한", nullable = true)
        String regionLimitText,
        @Schema(description = "업종 제한", nullable = true)
        String businessLimitText,
        @Schema(description = "공동수급 가능 여부", nullable = true)
        Boolean jointContractAllowed,
        @Schema(description = "공동수급 설명", nullable = true)
        String jointContractText,
        @Schema(description = "평가 방식", nullable = true)
        String evaluationMethod,
        @Schema(description = "공고 원문 URL", nullable = true)
        String sourceUrl,
        @Schema(description = "공개 첨부 링크 목록")
        List<ManualBidNoticeAttachmentRequest> attachments
) {

    // HTTP 요청과 인증 사용자를 직접 등록 Command로 변환합니다.
    public CreateManualBidNoticeCommand toCommand(String userId, String role) {
        List<ManualBidNoticeAttachmentRequest> safeAttachments =
                attachments == null ? List.of() : attachments;

        return new CreateManualBidNoticeCommand(
                noticeName, noticeType, noticeAgency, demandAgency,
                internationalBidType, announcedAt, bidStartAt,
                bidDeadlineAt, openingAt, baseAmount, estimatedAmount,
                bidMethod, contractMethod, participationQualificationText,
                regionLimitText, businessLimitText, jointContractAllowed,
                jointContractText, evaluationMethod, sourceUrl,
                IntStream.range(0, safeAttachments.size())
                        .mapToObj(index -> safeAttachments.get(index)
                                .toDomain(index + 1))
                        .toList(),
                userId, role
        );
    }
}