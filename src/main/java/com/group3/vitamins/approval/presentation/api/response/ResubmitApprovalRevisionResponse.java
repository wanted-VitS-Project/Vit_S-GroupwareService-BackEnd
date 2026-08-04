package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalResubmissionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ResubmitApprovalRevisionResponse(
        @Schema(description = "새(또는 기존) DRAFT 회차 구분 번호", example = "2")
        Long revisionId,

        @Schema(description = "상신 회차 번호", example = "2")
        int revisionNo,

        @Schema(description = "DRAFT", example = "DRAFT")
        String status,

        @Schema(description = "복사 원본 회차 번호", example = "1")
        int copiedFromRevisionNo,

        @Schema(description = "이전 회차에서 복사된 제목", example = "8월 정산 결재")
        String title,

        @Schema(description = "이전 회차에서 복사된 내용", example = "8월 정산 내역입니다.")
        String content,

        @Schema(description = "이전 회차에서 복사된 문서 목록")
        List<DocumentItem> documents,

        @Schema(description = "반려자부터 재구성된 결재선 목록")
        List<LineItem> lines
) {

    public record DocumentItem(
            @Schema(description = "결재 문서 구분 번호", example = "1")
            Long documentId,

            @Schema(description = "파일 버전 구분 번호", example = "10")
            Long fileVersionId
    ) {
    }

    public record LineItem(
            @Schema(description = "결재선 구분 번호", example = "3")
            Long lineId,

            @Schema(description = "결재자 구분 번호(사번)", example = "EMP2024002")
            String approverId,

            @Schema(description = "결재자 이름(라이브 조회)", example = "김철수")
            String approverName,

            @Schema(description = "결재자 직책(라이브 조회)", example = "부장")
            String approverPosition,

            @Schema(description = "결재자 부서(라이브 조회)", example = "기술본부 / 개발팀")
            String approverDepartment,

            @Schema(description = "결재 순서(반려자부터 1로 재부여)", example = "1")
            int order
    ) {
    }

    public static ResubmitApprovalRevisionResponse from(ApprovalResubmissionResult result) {
        List<DocumentItem> documents = result.documents().stream()
                .map(d -> new DocumentItem(d.getDocumentId(), d.getFileVersionId()))
                .toList();
        List<LineItem> lines = result.lines().stream()
                .map(l -> new LineItem(l.lineId(), l.approverId(), l.approverName(),
                        l.approverPosition(), l.approverDepartment(), l.order()))
                .toList();

        return new ResubmitApprovalRevisionResponse(
                result.revision().getRevisionId(), result.revision().getRevisionNo(),
                result.revision().getStatus().name(), result.copiedFromRevisionNo(),
                result.revision().getTitle(), result.revision().getContent(),
                documents, lines);
    }
}
