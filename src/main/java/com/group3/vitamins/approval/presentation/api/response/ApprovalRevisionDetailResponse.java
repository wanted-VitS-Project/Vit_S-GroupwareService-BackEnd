package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalRevisionDetail;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record ApprovalRevisionDetailResponse(
        @Schema(description = "상신 회차 구분 번호", example = "1")
        Long revisionId,

        @Schema(description = "상신 회차 번호", example = "1")
        int revisionNo,

        @Schema(description = "결재 제목", example = "8월 정산 결재")
        String title,

        @Schema(description = "결재 내용", example = "8월 정산 내역입니다.")
        String content,

        @Schema(description = "기안자 구분 번호(사번)", example = "EMP2024001")
        String drafterId,

        @Schema(description = "기안자 이름(라이브 조회)", example = "홍길동")
        String drafterName,

        @Schema(description = "기안자 부서(라이브 조회)", example = "기술본부 / 개발팀")
        String drafterDepartment,

        @Schema(description = "기안자 직책(라이브 조회)", example = "과장")
        String drafterPosition,

        @Schema(description = "회차 상태", example = "IN_PROGRESS")
        String status,

        @Schema(description = "상신 일시", example = "2026-08-04T13:00:00")
        LocalDateTime submittedAt,

        @Schema(description = "종료 일시(완료·반려)", example = "null")
        LocalDateTime finishedAt,

        @Schema(description = "결재 문서 목록")
        List<DocumentItem> documents,

        @Schema(description = "결재선 목록")
        List<LineItem> lines
) {

    public record DocumentItem(
            @Schema(description = "결재 문서 구분 번호", example = "1")
            Long documentId,

            @Schema(description = "파일 버전 구분 번호", example = "10")
            Long fileVersionId,

            @Schema(description = "파일명(라이브 조회)", example = "제안서_v1.pdf")
            String fileName,

            @Schema(description = "파일 크기(byte, 라이브 조회)", example = "4404019")
            Long fileSize,

            @Schema(description = "업로드 완료 일시(라이브 조회)", example = "2026-08-04T12:00:00")
            LocalDateTime uploadedAt
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

            @Schema(description = "결재 순서", example = "1")
            int order,

            @Schema(description = "결재선 상태", example = "ACTIVE")
            String status,

            @Schema(description = "결재 의견", example = "확인했습니다.")
            String opinion,

            @Schema(description = "결재 처리 일시", example = "null")
            LocalDateTime processedAt
    ) {
    }

    public static ApprovalRevisionDetailResponse from(ApprovalRevisionDetail detail) {
        List<DocumentItem> documents = detail.documents().stream()
                .map(d -> new DocumentItem(d.documentId(), d.fileVersionId(), d.fileName(), d.fileSize(), d.uploadedAt()))
                .toList();
        List<LineItem> lines = detail.lines().stream()
                .map(l -> new LineItem(l.lineId(), l.approverId(), l.approverName(), l.approverPosition(),
                        l.approverDepartment(), l.order(), l.status(), l.opinion(), l.processedAt()))
                .toList();

        return new ApprovalRevisionDetailResponse(
                detail.revisionId(), detail.revisionNo(), detail.title(), detail.content(),
                detail.drafterId(), detail.drafterName(), detail.drafterDepartment(), detail.drafterPosition(),
                detail.status(), detail.submittedAt(), detail.finishedAt(), documents, lines);
    }
}
