package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사내 문서 버전 상세 — 완료 통보(§2) 응답. 업로더 스냅샷은 ADMIN 업로드 시 null 일 수 있다(§6-6).")
public record CompanyDocumentVersionDetailResponse(
        @Schema(description = "문서 번호", example = "12") Long companyDocumentId,
        @Schema(description = "버전 번호", example = "34") Long versionId,
        @Schema(description = "버전 차수", example = "1") int versionNo,
        @Schema(description = "문서 표시명", example = "2026년 재무제표") String name,
        @Schema(description = "카테고리 enum", example = "FINANCE") String category,
        @Schema(description = "원본 파일명", example = "2026_재무제표.pdf") String originalFileName,
        @Schema(description = "확장자(소문자, 점 제외)", example = "pdf") String extension,
        @Schema(description = "파일 크기(byte)", example = "1048576") long sizeBytes,
        @Schema(description = "PDF 총 페이지 수. PDF 가 아니거나 추출 실패면 null", example = "12", nullable = true) Integer pageCount,
        @Schema(description = "버전 코멘트", example = "1분기 갱신", nullable = true) String comment,
        @Schema(description = "업로더 이름. ADMIN 업로드면 null", example = "박지영", nullable = true) String uploaderName,
        @Schema(description = "업로더 부서", example = "경영지원팀", nullable = true) String uploaderDepartment,
        @Schema(description = "업로더 직위", example = "팀장", nullable = true) String uploaderPosition,
        @Schema(description = "업로드 완료 시각 yyyy-MM-dd HH:mm:ss", example = "2026-08-13 00:53:02") String completedAt
) {

    public static CompanyDocumentVersionDetailResponse from(CompanyDocumentVersionDetailResult r) {
        return new CompanyDocumentVersionDetailResponse(
                r.companyDocumentId(), r.versionId(), r.versionNo(), r.name(), r.category(),
                r.originalFileName(), r.extension(), r.sizeBytes(), r.pageCount(), r.comment(),
                r.uploaderName(), r.uploaderDepartment(), r.uploaderPosition(),
                CompanyDocumentDateTimeFormat.format(r.completedAt()));
    }
}
