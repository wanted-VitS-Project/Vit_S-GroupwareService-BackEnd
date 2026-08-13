package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentReferenceView;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 참조 선택용 사내 문서 응답 (입찰 검토 비교자료 선택). 참조는 버전 고정({@code companyDocumentVersionId})으로 준다.
 */
public record CompanyDocumentReferenceResponse(
        @Schema(description = "문서 번호", example = "12")
        Long companyDocumentId,
        @Schema(description = "참조로 고정할 버전 번호", example = "34")
        Long companyDocumentVersionId,
        @Schema(description = "분류", example = "PERFORMANCE")
        String category,
        @Schema(description = "원본 파일명", example = "2025_실적요약.pdf")
        String originalFileName,
        @Schema(description = "버전 차수", example = "3")
        int versionNo,
        @Schema(description = "AI 인덱스 준비 상태 (현재 null · §6-2 이후 채움)", example = "READY")
        String indexStatus
) {

    public static CompanyDocumentReferenceResponse from(CompanyDocumentReferenceView v) {
        return new CompanyDocumentReferenceResponse(
                v.companyDocumentId(), v.companyDocumentVersionId(), v.category(),
                v.originalFileName(), v.versionNo(), v.indexStatus());
    }
}
