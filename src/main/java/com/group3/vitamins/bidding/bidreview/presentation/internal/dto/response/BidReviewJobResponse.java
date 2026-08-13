package com.group3.vitamins.bidding.bidreview.presentation.internal.dto.response;

import com.group3.vitamins.bidding.bidreview.application.result.BidReviewJobResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record BidReviewJobResponse(

        @Schema(description = "검토 ID")
        Long reviewId,

        @Schema(description = "회사 ID")
        Long companyId,

        @Schema(description = "현재 작업 시도 ID")
        String attemptId,

        @Schema(description = "사용자가 입력한 검토 프롬프트")
        String prompt,

        @Schema(description = "입찰 공고 ID")
        Long noticeId,

        @Schema(description = "입찰 공고명")
        String noticeName,

        @Schema(description = "선택한 공고 첨부 목록")
        List<AttachmentJobResponse> attachments,

        @Schema(description = "선택한 사내 기준자료 목록")
        List<ReferenceFileJobResponse> referenceFiles,

        @Schema(description = "선택한 사내 문서함 참조 목록")
        List<CompanyDocumentJobResponse> companyDocuments
) {

    public static BidReviewJobResponse from(BidReviewJobResult result) {
        return new BidReviewJobResponse(
                result.reviewId(),
                result.companyId(),
                result.attemptId(),
                result.prompt(),
                result.noticeId(),
                result.noticeName(),
                result.attachments().stream()
                        .map(AttachmentJobResponse::from)
                        .toList(),
                result.referenceFiles().stream()
                        .map(ReferenceFileJobResponse::from)
                        .toList(),
                result.companyDocuments().stream()
                        .map(CompanyDocumentJobResponse::from)
                        .toList()
        );
    }

    public record AttachmentJobResponse(
            @Schema(description = "검토 요청에서 사용한 첨부 ID")
            Long attachmentId,

            @Schema(description = "원본 파일명")
            String fileName,

            @Schema(description = "내부 원본 다운로드 URL. 프론트용 API에서는 제공하지 않는다")
            String sourceUrl
    ) {

        static AttachmentJobResponse from(BidReviewJobResult.AttachmentJob job) {
            return new AttachmentJobResponse(
                    job.attachmentId(),
                    job.fileName(),
                    job.sourceUrl()
            );
        }
    }

    public record ReferenceFileJobResponse(
            @Schema(description = "검토 요청에서 사용한 기준자료 ID")
            Long referenceFileId,

            @Schema(description = "원본 파일명")
            String fileName,

            @Schema(description = "단명 내부 다운로드 URL. 프론트용 API에서는 제공하지 않는다")
            String downloadUrl
    ) {

        static ReferenceFileJobResponse from(BidReviewJobResult.ReferenceFileJob job) {
            return new ReferenceFileJobResponse(
                    job.referenceFileId(),
                    job.fileName(),
                    job.downloadUrl()
            );
        }
    }

    public record CompanyDocumentJobResponse(
            @Schema(description = "검토 요청에서 사용한 사내 문서함 참조 버전 ID")
            Long companyDocumentVersionId,

            @Schema(description = "원본 파일명")
            String fileName,

            @Schema(description = "단명 내부 다운로드 URL. 프론트용 API에서는 제공하지 않는다")
            String downloadUrl
    ) {

        static CompanyDocumentJobResponse from(BidReviewJobResult.CompanyDocumentJob job) {
            return new CompanyDocumentJobResponse(
                    job.companyDocumentVersionId(),
                    job.fileName(),
                    job.downloadUrl()
            );
        }
    }
}
