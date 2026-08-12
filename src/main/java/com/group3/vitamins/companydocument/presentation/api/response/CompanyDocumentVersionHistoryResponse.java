package com.group3.vitamins.companydocument.presentation.api.response;

import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionHistoryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Schema(description = "사내 문서 버전 이력(§7). 완료 버전만, 차수 내림차순.")
public record CompanyDocumentVersionHistoryResponse(
        @Schema(description = "문서 번호") Long companyDocumentId,
        @Schema(description = "문서 표시명") String name,
        @Schema(description = "카테고리 enum", example = "FINANCE") String category,
        @Schema(description = "완료 버전 수") int versionCount,
        @Schema(description = "버전 목록") List<Item> content
) {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Schema(description = "버전 이력 항목")
    public record Item(
            @Schema(description = "버전 ID") Long versionId,
            @Schema(description = "버전 차수") int versionNo,
            @Schema(description = "최신 버전 여부") boolean latest,
            @Schema(description = "원본 파일명") String originalFileName,
            @Schema(description = "확장자") String extension,
            @Schema(description = "크기(바이트)") long sizeBytes,
            @Schema(description = "PDF 총 페이지 수. PDF 아니거나 추출 실패면 null", nullable = true) Integer pageCount,
            @Schema(description = "미리보기 가능 여부(PDF 만 true)") boolean previewable,
            @Schema(description = "버전 코멘트", nullable = true) String comment,
            @Schema(description = "업로더 이름(null 허용)", nullable = true) String uploaderName,
            @Schema(description = "업로더 부서(null 허용)", nullable = true) String uploaderDepartment,
            @Schema(description = "업로더 직급(null 허용)", nullable = true) String uploaderPosition,
            @Schema(description = "완료 시각 yyyy-MM-dd HH:mm:ss") String completedAt
    ) {
    }

    public static CompanyDocumentVersionHistoryResponse from(CompanyDocumentVersionHistoryResult r) {
        List<Item> items = r.content().stream()
                .map(i -> new Item(
                        i.versionId(), i.versionNo(), i.latest(), i.originalFileName(), i.extension(),
                        i.sizeBytes(), i.pageCount(), i.previewable(), i.comment(),
                        i.uploaderName(), i.uploaderDepartment(), i.uploaderPosition(),
                        i.completedAt() == null ? null : i.completedAt().format(FMT)))
                .toList();
        return new CompanyDocumentVersionHistoryResponse(
                r.companyDocumentId(), r.name(), r.category(), r.versionCount(), items);
    }
}
