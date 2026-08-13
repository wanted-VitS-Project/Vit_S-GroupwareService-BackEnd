package com.group3.vitamins.file.application.result;

import java.util.List;

/**
 * 전사 파일 관리(FILE-Q-01) 페이지 결과. 프로젝트 목록 페이지네이션(ProjectPageResult)과 동일 규약.
 */
public record CompanyFilePageResult(
        List<FileViewResult> content,
        int page,
        int size,
        long totalElements
) {

    /** 전체 건수와 페이지 크기로 파생한다. size 가 0 이면 0. */
    public int totalPages() {
        if (size <= 0) {
            return 0;
        }
        return (int) ((totalElements + size - 1) / size);
    }
}
