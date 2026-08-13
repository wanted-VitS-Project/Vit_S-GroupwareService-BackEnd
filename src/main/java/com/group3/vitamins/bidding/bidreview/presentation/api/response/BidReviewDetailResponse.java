package com.group3.vitamins.bidding.bidreview.presentation.api.response;

import com.group3.vitamins.bidding.bidreview.application.result.BidReviewDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record BidReviewDetailResponse(

        @Schema(description = "검토 ID", example = "71")
        Long reviewId,

        @Schema(description = "입찰 공고 ID", example = "1")
        Long noticeId,

        @Schema(description = "요청 당시 사용자 프롬프트")
        String prompt,

        @Schema(description = "검토 상태", example = "COMPLETED")
        String reviewStatus,

        @Schema(description = "근거 번호를 포함한 검토 결과. 완료 전에는 null", nullable = true)
        String result,

        @Schema(description = "실패 사유. 실패가 아니면 null", nullable = true)
        String errorMessage,

        @Schema(description = "요청 시각")
        LocalDateTime requestedAt,

        @Schema(description = "완료 또는 실패 시각", nullable = true)
        LocalDateTime completedAt,

        @Schema(description = "임시파일 정리 예정 시각. 처리 중이거나 귀속 완료면 null", nullable = true)
        LocalDateTime expiresAt,

        @Schema(description = "정식 프로젝트로 귀속됐으면 프로젝트 ID", nullable = true)
        Long projectId,

        @Schema(description = "요청 당시 선택한 문서 목록과 처리 상태")
        List<DocumentResponse> documents,

        @Schema(description = "검토 결과의 근거 목록")
        List<CitationResponse> citations
) {

    public static BidReviewDetailResponse from(BidReviewDetailResult result) {
        return new BidReviewDetailResponse(
                result.reviewId(),
                result.noticeId(),
                result.prompt(),
                result.reviewStatus(),
                result.result(),
                result.errorMessage(),
                result.requestedAt(),
                result.completedAt(),
                result.expiresAt(),
                result.projectId(),
                result.documents().stream().map(DocumentResponse::from).toList(),
                result.citations().stream().map(CitationResponse::from).toList()
        );
    }

    public record DocumentResponse(
            @Schema(description = "문서 역할", example = "BID_ATTACHMENT")
            String documentRole,

            @Schema(description = "공고 첨부 ID. 문서 역할이 아니면 null", nullable = true)
            Long bidAttachmentId,

            @Schema(description = "기준자료 ID. 문서 역할이 아니면 null", nullable = true)
            Long referenceFileId,

            @Schema(description = "사내 문서함 참조 버전 ID. 문서 역할이 아니면 null", nullable = true)
            Long companyDocumentVersionId,

            @Schema(description = "검토 요청 당시 파일명 스냅샷")
            String fileName,

            @Schema(description = "문서 처리 상태", example = "READY")
            String processingStatus
    ) {

        public static DocumentResponse from(
                BidReviewDetailResult.DocumentResult result
        ) {
            return new DocumentResponse(
                    result.documentRole(),
                    result.bidAttachmentId(),
                    result.referenceFileId(),
                    result.companyDocumentVersionId(),
                    result.fileName(),
                    result.processingStatus()
            );
        }
    }

    public record CitationResponse(
            @Schema(description = "근거 순번", example = "1")
            Integer rankOrder,

            @Schema(description = "근거 문서의 역할", example = "INTERNAL_REFERENCE")
            String documentRole,

            @Schema(description = "공고 첨부 ID. 문서 역할이 아니면 null", nullable = true)
            Long bidAttachmentId,

            @Schema(description = "기준자료 ID. 문서 역할이 아니면 null", nullable = true)
            Long referenceFileId,

            @Schema(description = "사내 문서함 참조 버전 ID. 문서 역할이 아니면 null", nullable = true)
            Long companyDocumentVersionId,

            @Schema(description = "근거 문서 파일명")
            String fileName,

            @Schema(description = "페이지 번호. 없으면 null", nullable = true)
            Integer pageNumber,

            @Schema(description = "시트명. 없으면 null", nullable = true)
            String sheetName,

            @Schema(description = "발췌문")
            String excerpt
    ) {

        public static CitationResponse from(
                BidReviewDetailResult.CitationResult result
        ) {
            return new CitationResponse(
                    result.rankOrder(),
                    result.documentRole(),
                    result.bidAttachmentId(),
                    result.referenceFileId(),
                    result.companyDocumentVersionId(),
                    result.fileName(),
                    result.pageNumber(),
                    result.sheetName(),
                    result.excerpt()
            );
        }
    }
}