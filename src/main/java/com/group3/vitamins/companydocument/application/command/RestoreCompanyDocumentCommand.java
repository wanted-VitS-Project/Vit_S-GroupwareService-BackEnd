package com.group3.vitamins.companydocument.application.command;

/** 사내 문서 복구(§6) 커맨드. */
public record RestoreCompanyDocumentCommand(
        Long companyDocumentId,
        String requesterUserId,
        String role
) {
}
