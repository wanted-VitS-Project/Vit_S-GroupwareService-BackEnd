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
        @NotBlank(message = "BIDDING_INVALID_MANUAL_NOTICE|공고명을 입력해 주세요.")
        @Size(max = 1000, message = "BIDDING_INVALID_MANUAL_NOTICE|공고명은 1,000자를 넘을 수 없습니다.")
        @Schema(description = "공고명", example = "스마트시티 통합관제 플랫폼 구축 용역")
        String noticeName,
        @NotNull(message = "BIDDING_INVALID_MANUAL_NOTICE|공고 유형을 선택해 주세요.")
        @Schema(description = "공고 유형", example = "SERVICE")
        BidNoticeType noticeType,
        @NotBlank(message = "BIDDING_INVALID_MANUAL_NOTICE|공고기관을 입력해 주세요.")
        @Size(max = 400, message = "BIDDING_INVALID_MANUAL_NOTICE|공고기관명은 400자를 넘을 수 없습니다.")
        @Schema(description = "공고기관", example = "서울특별시")
        String noticeAgency,
        @Size(max = 400, message = "BIDDING_INVALID_MANUAL_NOTICE|수요기관명은 400자를 넘을 수 없습니다.")
        @Schema(description = "수요기관", example = "서울특별시 정보화담당관", nullable = true)
        String demandAgency,
        @Schema(description = "국제입찰 구분", example = "DOMESTIC", nullable = true)
        InternationalBidType internationalBidType,
        @NotNull(message = "BIDDING_INVALID_MANUAL_NOTICE|공고일시를 입력해 주세요.")
        @Schema(description = "공고일시", example = "2026-08-11T09:00:00")
        LocalDateTime announcedAt,
        @Schema(description = "입찰개시일시", example = "2026-08-12T09:00:00", nullable = true)
        LocalDateTime bidStartAt,
        @NotNull(message = "BIDDING_INVALID_MANUAL_NOTICE|입찰마감일시를 입력해 주세요.")
        @Schema(description = "입찰마감일시", example = "2026-08-20T18:00:00")
        LocalDateTime bidDeadlineAt,
        @Schema(description = "개찰일시", example = "2026-08-21T10:00:00", nullable = true)
        LocalDateTime openingAt,
        @DecimalMin(value = "0", message = "BIDDING_INVALID_MANUAL_NOTICE|기초금액은 0 이상이어야 합니다.")
        @Schema(description = "기초금액", example = "300000000", nullable = true)
        BigDecimal baseAmount,
        @DecimalMin(value = "0", message = "BIDDING_INVALID_MANUAL_NOTICE|추정금액은 0 이상이어야 합니다.")
        @Schema(description = "추정금액", example = "330000000", nullable = true)
        BigDecimal estimatedAmount,
        @Size(max = 100, message = "BIDDING_INVALID_MANUAL_NOTICE|입찰 방식은 100자를 넘을 수 없습니다.")
        @Schema(description = "입찰 방식", example = "전자입찰", nullable = true)
        String bidMethod,
        @Size(max = 100, message = "BIDDING_INVALID_MANUAL_NOTICE|계약 방식은 100자를 넘을 수 없습니다.")
        @Schema(description = "계약 방식", example = "협상에 의한 계약", nullable = true)
        String contractMethod,
        @Size(max = 1000, message = "BIDDING_INVALID_MANUAL_NOTICE|참가 자격은 1,000자를 넘을 수 없습니다.")
        @Schema(description = "참가 자격", example = "관련 사업 수행 실적 보유 업체", nullable = true)
        String participationQualificationText,
        @Size(max = 500, message = "BIDDING_INVALID_MANUAL_NOTICE|지역 제한은 500자를 넘을 수 없습니다.")
        @Schema(description = "지역 제한", example = "서울특별시", nullable = true)
        String regionLimitText,
        @Size(max = 500, message = "BIDDING_INVALID_MANUAL_NOTICE|업종 제한은 500자를 넘을 수 없습니다.")
        @Schema(description = "업종 제한", example = "소프트웨어사업자", nullable = true)
        String businessLimitText,
        @Schema(description = "공동수급 가능 여부", example = "false", nullable = true)
        Boolean jointContractAllowed,
        @Size(max = 500, message = "BIDDING_INVALID_MANUAL_NOTICE|공동수급 설명은 500자를 넘을 수 없습니다.")
        @Schema(description = "공동수급 설명", example = "공동이행 방식 허용", nullable = true)
        String jointContractText,
        @Size(max = 100, message = "BIDDING_INVALID_MANUAL_NOTICE|평가 방식은 100자를 넘을 수 없습니다.")
        @Schema(description = "평가 방식", example = "기술·가격 종합평가", nullable = true)
        String evaluationMethod,
        @Size(max = 1000, message = "BIDDING_INVALID_MANUAL_NOTICE|공고 원문 URL은 1,000자를 넘을 수 없습니다.")
        @Pattern(
                regexp = "https?://.+",
                message = "BIDDING_INVALID_MANUAL_NOTICE|공고 원문 URL 형식이 올바르지 않습니다. http:// 또는 https://로 시작해야 합니다."
        )
        @Schema(description = "공고 원문 URL", example = "https://example.org/notices/2026-001", nullable = true)
        String sourceUrl,
        @Size(max = 10, message = "BIDDING_INVALID_MANUAL_NOTICE|공개 첨부 링크는 최대 10개까지 등록할 수 있습니다.")
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
