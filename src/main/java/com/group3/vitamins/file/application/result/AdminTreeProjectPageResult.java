package com.group3.vitamins.file.application.result;

import java.util.List;

/** 전사 파일 트리(§14.1) 프로젝트 페이지 결과. CompanyFilePageResult 와 동일 규약. */
public record AdminTreeProjectPageResult(
        List<AdminTreeProjectProjection> content,
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
