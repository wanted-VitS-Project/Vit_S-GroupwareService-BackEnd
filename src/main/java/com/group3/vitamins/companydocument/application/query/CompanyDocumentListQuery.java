package com.group3.vitamins.companydocument.application.query;

/** 사내 문서 목록 조회(§3) 입력 — 컨트롤러 파라미터. 회사 스코프는 서비스가 현재 테넌트로 채운다. */
public record CompanyDocumentListQuery(
        String requesterUserId,
        String role,
        String category,
        String keyword,
        int page,
        int size
) {
}
