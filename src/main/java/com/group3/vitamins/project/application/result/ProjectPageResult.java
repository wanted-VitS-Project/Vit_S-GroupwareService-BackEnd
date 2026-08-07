package com.group3.vitamins.project.application.result;

import java.util.List;

public record ProjectPageResult(
        List<ProjectSummary> content,
        int page,
        int size,
        long totalElements
) {

    /** 전체 건수와 페이지 크기로 파생한다. size 가 0 이면 0 을 돌려준다. */
    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }
}