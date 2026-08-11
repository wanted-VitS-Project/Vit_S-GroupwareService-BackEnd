package com.group3.vitamins.bidding.bidnotice.presentation.api.response;

import com.group3.vitamins.bidding.bidnotice.application.result.ManualBidNoticeResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record ManualBidNoticeResponse(
        @Schema(description = "공고 ID") Long noticeId,
        @Schema(description = "직접 등록 식별자") String externalId,
        @Schema(description = "공고 차수") String noticeOrder,
        @Schema(description = "수집처 코드") String sourceCode,
        @Schema(description = "수집처 이름") String sourceName,
        @Schema(description = "공고 상태") String noticeStatus,
        @Schema(description = "공고명") String noticeName,
        @Schema(description = "공고 유형") String noticeType,
        @Schema(description = "공고기관") String noticeAgency,
        @Schema(description = "공고일시") LocalDateTime announcedAt,
        @Schema(description = "입찰마감일시") LocalDateTime bidDeadlineAt,
        @Schema(description = "첨부 링크 목록") List<AttachmentResponse> attachments,
        @Schema(description = "생성 시각") LocalDateTime createdAt,
        @Schema(description = "수정 시각", nullable = true) LocalDateTime updatedAt
) {

    // Application 결과를 직접 등록 API 응답으로 변환합니다.
    public static ManualBidNoticeResponse from(ManualBidNoticeResult result) {
        return new ManualBidNoticeResponse(
                result.noticeId(), result.externalId(), result.noticeOrder(),
                result.sourceCode(), result.sourceName(), result.noticeStatus(),
                result.noticeName(), result.noticeType(), result.noticeAgency(),
                result.announcedAt(), result.bidDeadlineAt(),
                result.attachments().stream().map(AttachmentResponse::from).toList(),
                result.createdAt(), result.updatedAt()
        );
    }

    public record AttachmentResponse(
            @Schema(description = "첨부 순번", example = "1") int attachmentOrder,
            @Schema(description = "첨부파일 표시명", example = "제안요청서.pdf") String fileName,
            @Schema(description = "첨부파일 공개 원문 URL", example = "https://example.org/notices/2026-001/rfp.pdf") String sourceUrl
    ) {
        private static AttachmentResponse from(
                ManualBidNoticeResult.Attachment attachment
        ) {
            return new AttachmentResponse(
                    attachment.attachmentOrder(),
                    attachment.fileName(),
                    attachment.sourceUrl()
            );
        }
    }
}
