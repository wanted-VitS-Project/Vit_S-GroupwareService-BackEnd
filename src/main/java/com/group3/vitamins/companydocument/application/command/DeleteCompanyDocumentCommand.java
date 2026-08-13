package com.group3.vitamins.companydocument.application.command;

/** 사내 문서 삭제(§5, soft delete) 커맨드. */
public record DeleteCompanyDocumentCommand(
        Long companyDocumentId,
        String requesterUserId,
        String role
) {
}
