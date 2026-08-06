package com.group3.vitamins.approval.presentation.api.response;

import com.group3.vitamins.approval.application.result.ApprovalDetailResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ApprovalDetailResponse(
        @Schema(description = "현재 회차 구분 번호", example = "55")
        Long revisionId,

        @Schema(description = "현재 회차 번호", example = "1")
        int revisionNo,

        @Schema(description = "결재 제목", example = "출장비 정산 결재")
        String title,

        @Schema(description = "결재 내용", example = "3월 출장비 정산 요청드립니다.")
        String content,

        @Schema(description = "기안자 구분 번호(사번)", example = "EMP2024001")
        String drafterId,

        @Schema(description = "기안자 이름(라이브 조회)", example = "이강욱")
        String drafterName,

        @Schema(description = "기안자 부서(라이브 조회)", example = "기술본부 / 개발팀")
        String drafterDepartment,

        @Schema(description = "기안자 직책(라이브 조회)", example = "과장")
        String drafterPosition,

        @Schema(description = "회차 상태", example = "IN_PROGRESS")
        String status,

        @Schema(description = "결재 문서 목록")
        List<ApprovalRevisionDetailResponse.DocumentItem> documents,

        @Schema(description = "결재선 목록")
        List<ApprovalRevisionDetailResponse.LineItem> lines,

        @Schema(description = "원본 블록 이동 정보")
        BlockOrigin blockOrigin
) {

    public record BlockOrigin(
            @Schema(description = "원본 블록 구분 번호", example = "101")
            Long blockId,

            @Schema(description = "원본 스텝 구분 번호", example = "30")
            Long stepId,

            @Schema(description = "원본 프로젝트 구분 번호", example = "5")
            Long projectId
    ) {
    }

    public static ApprovalDetailResponse from(ApprovalDetailResult detail) {
        List<ApprovalRevisionDetailResponse.DocumentItem> documents = detail.documents().stream()
                .map(d -> new ApprovalRevisionDetailResponse.DocumentItem(
                        d.documentId(), d.fileVersionId(), d.fileName(), d.fileSize(), d.uploadedAt()))
                .toList();
        List<ApprovalRevisionDetailResponse.LineItem> lines = detail.lines().stream()
                .map(l -> new ApprovalRevisionDetailResponse.LineItem(l.lineId(), l.approverId(), l.approverName(),
                        l.approverPosition(), l.approverDepartment(), l.order(), l.status(), l.opinion(), l.processedAt()))
                .toList();
        BlockOrigin blockOrigin = new BlockOrigin(
                detail.blockOrigin().blockId(), detail.blockOrigin().stepId(), detail.blockOrigin().projectId());

        return new ApprovalDetailResponse(detail.revisionId(), detail.revisionNo(), detail.title(), detail.content(),
                detail.drafterId(), detail.drafterName(), detail.drafterDepartment(), detail.drafterPosition(),
                detail.status(), documents, lines, blockOrigin);
    }
}
