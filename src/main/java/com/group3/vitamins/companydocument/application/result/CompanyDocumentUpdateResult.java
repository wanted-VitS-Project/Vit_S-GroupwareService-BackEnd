package com.group3.vitamins.companydocument.application.result;

/** 사내 문서 수정(§4) 결과. */
public record CompanyDocumentUpdateResult(
        Long companyDocumentId,
        String name,
        String category
) {
}
