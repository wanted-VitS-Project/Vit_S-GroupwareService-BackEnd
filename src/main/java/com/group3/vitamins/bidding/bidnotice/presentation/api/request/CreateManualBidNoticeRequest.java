package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.group3.vitamins.bidding.bidnotice.application.command.CreateManualBidNoticeCommand;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

public record CreateManualBidNoticeRequest(
        @NotBlank @Size(max = 1000)
        @Schema(description = "공고명", example = "스마트시티 통합관제 플랫폼 구축 용역")
        String noticeName,
        @NotNull
        @Schema(description = "공고 유형", example = "SERVICE")
        BidNoticeType noticeType,
        @NotBlank @Size(max = 400)
        @Schema(description = "공고기관", example = "서울특별시")
        String noticeAgency,
        @Size(max = 400)
        @Schema(description = "수요기관", example = "서울특별시 정보화담당관", nullable = true)
        String demandAgency,
        @Schema(description = "국제입찰 구분", example = "DOMESTIC", nullable = true)
        InternationalBidType internationalBidType,
        @NotNull
        @Schema(description = "공고일시", example = "2026-08-11T09:00:00")
        LocalDateTime announcedAt,
        @Schema(description = "입찰개시일시", example = "2026-08-12T09:00:00", nullable = true)
        LocalDateTime bidStartAt,
        @NotNull
        @Schema(description = "입찰마감일시", example = "2026-08-20T18:00:00")
        LocalDateTime bidDeadlineAt,
        @Schema(description = "개찰일시", example = "2026-08-21T10:00:00", nullable = true)
        LocalDateTime openingAt,
        @DecimalMin("0")
        @Schema(description = "기초금액", example = "300000000", nullable = true)
        BigDecimal baseAmount,
        @DecimalMin("0")
        @Schema(description = "추정금액", example = "330000000", nullable = true)
        BigDecimal estimatedAmount,
        @Size(max = 100)
        @Schema(description = "입찰 방식", example = "전자입찰", nullable = true)
        String bidMethod,
        @Size(max = 100)
        @Schema(description = "계약 방식", example = "협상에 의한 계약", nullable = true)
        String contractMethod,
        @Size(max = 1000)
        @Schema(description = "참가 자격", example = "관련 사업 수행 실적 보유 업체", nullable = true)
        String participationQualificationText,
        @Size(max = 500)
        @Schema(description = "지역 제한", example = "서울특별시", nullable = true)
        String regionLimitText,
        @Size(max = 500)
        @Schema(description = "업종 제한", example = "소프트웨어사업자", nullable = true)
        String businessLimitText,
        @Schema(description = "공동수급 가능 여부", example = "false", nullable = true)
        Boolean jointContractAllowed,
        @Size(max = 500)
        @Schema(description = "공동수급 설명", example = "공동이행 방식 허용", nullable = true)
        String jointContractText,
        @Size(max = 100)
        @Schema(description = "평가 방식", example = "기술·가격 종합평가", nullable = true)
        String evaluationMethod,
        @Size(max = 1000)
        @Pattern(regexp = "https?://.+")
        @Schema(description = "공고 원문 URL", example = "https://example.org/notices/2026-001", nullable = true)
        String sourceUrl,
        @Size(max = 10)
        @Schema(description = "공개 첨부 링크 목록")
        List<@Valid ManualBidNoticeAttachmentRequest> attachments
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
