package com.group3.vitamins.approval.application.query;

import java.time.LocalDate;

/** 결재관리 목록조회(MGT-001~004). {@code scope} 해석(본인 강제 등)은 서비스가 담당한다. */
public record ListApprovalsQuery(
        String scope,
        String status,
        String drafterId,
        String approverId,
        LocalDate fromDate,
        LocalDate toDate,
        String keyword,
        Integer revisionNo,
        int page,
        int size,
        String requesterId
) {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final String DEFAULT_SCOPE = "drafted";

    /** {@code scope} 기본값(drafted)을 강제하고, 페이지·크기는 알림 목록조회와 동일한 정책(0/10, 상한 100)을 따른다. */
    public ListApprovalsQuery {
        scope = (scope == null || scope.isBlank()) ? DEFAULT_SCOPE : scope.trim().toLowerCase();
        status = (status == null || status.isBlank()) ? null : status.trim();
        keyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        if (size <= 0) {
            size = DEFAULT_SIZE;
        } else if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
        // offset(= page * size) 계산 시 int 오버플로 방지. size 를 먼저 확정한 뒤 그 값 기준으로 page 상한을 둔다.
        if (page < 0) {
            page = 0;
        } else if (page > Integer.MAX_VALUE / size) {
            page = Integer.MAX_VALUE / size;
        }
    }
}
