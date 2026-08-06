package com.group3.vitamins.approval.application.result;

/** 결재관리 목록조회(MGT-004)의 결재선 미리보기 1건(아바타 표시용). */
public record ApprovalLinePreviewResult(String approverId, String approverName, int order, String status) {
}
