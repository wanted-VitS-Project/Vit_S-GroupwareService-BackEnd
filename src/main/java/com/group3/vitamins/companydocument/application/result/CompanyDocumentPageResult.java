package com.group3.vitamins.companydocument.application.result;

import java.util.List;

/** 사내 문서 목록(§3) 페이지 결과. FILE-Q 전사 파일(CompanyFilePageResult)과 동일 규약. */
public record CompanyDocumentPageResult(
        List<CompanyDocumentListItemResult> content,
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
