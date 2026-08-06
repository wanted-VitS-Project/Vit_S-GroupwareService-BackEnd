package com.group3.vitamins.approval.application.result;

import java.util.List;

/** 결재관리 목록조회 페이지 결과 (`NotificationPageResult`와 동일 구조). */
public record ApprovalListPageResult(List<ApprovalListItemResult> content, long totalElements, int totalPages) {
}
