package com.group3.vitamins.bidding.bidnotice.presentation.api.request;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.InternationalBidType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "직접 등록 공고 부분 수정 요청")
public record UpdateManualBidNoticeRequest(
        @Schema(description = "공고명. 생략 가능하지만 명시적 null은 불가", example = "스마트시티 구축 용역") String noticeName,
        @Schema(description = "공고 유형. 생략 가능하지만 명시적 null은 불가", example = "SERVICE") BidNoticeType noticeType,
        @Schema(description = "공고기관. 생략 가능하지만 명시적 null은 불가", example = "서울특별시") String noticeAgency,
        @Schema(description = "수요기관. null이면 기존 값 해제", nullable = true) String demandAgency,
        @Schema(description = "국제입찰 구분. null이면 기존 값 해제", nullable = true) InternationalBidType internationalBidType,
        @Schema(description = "공고일시. 생략 가능하지만 명시적 null은 불가", example = "2026-08-11T09:00:00") LocalDateTime announcedAt,
        @Schema(description = "입찰개시일시. null이면 기존 값 해제", nullable = true) LocalDateTime bidStartAt,
        @Schema(description = "입찰마감일시. 생략 가능하지만 명시적 null은 불가", example = "2026-08-20T18:00:00") LocalDateTime bidDeadlineAt,
        @Schema(description = "개찰일시. null이면 기존 값 해제", nullable = true) LocalDateTime openingAt,
        @Schema(description = "기초금액. null이면 기존 값 해제", nullable = true) BigDecimal baseAmount,
        @Schema(description = "추정금액. null이면 기존 값 해제", nullable = true) BigDecimal estimatedAmount,
        @Schema(description = "입찰 방식. null이면 기존 값 해제", nullable = true) String bidMethod,
        @Schema(description = "계약 방식. null이면 기존 값 해제", nullable = true) String contractMethod,
        @Schema(description = "참가 자격. null이면 기존 값 해제", nullable = true) String participationQualificationText,
        @Schema(description = "지역 제한. null이면 기존 값 해제", nullable = true) String regionLimitText,
        @Schema(description = "업종 제한. null이면 기존 값 해제", nullable = true) String businessLimitText,
        @Schema(description = "공동수급 가능 여부. null이면 기존 값 해제", nullable = true) Boolean jointContractAllowed,
        @Schema(description = "공동수급 설명. null이면 기존 값 해제", nullable = true) String jointContractText,
        @Schema(description = "평가 방식. null이면 기존 값 해제", nullable = true) String evaluationMethod,
        @Schema(description = "공고 원문 URL. null이면 기존 값 해제", nullable = true) String sourceUrl,
        @Schema(description = "생략 시 유지, 빈 배열이면 전체 삭제, 배열이면 전체 교체")
        List<ManualBidNoticeAttachmentRequest> attachments
) {
}
