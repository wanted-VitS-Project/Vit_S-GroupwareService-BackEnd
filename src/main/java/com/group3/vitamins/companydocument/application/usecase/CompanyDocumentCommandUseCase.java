package com.group3.vitamins.companydocument.application.usecase;

import com.group3.vitamins.companydocument.application.command.DeleteCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.command.RestoreCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.command.UpdateCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentDeleteResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentRestoreResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentUpdateResult;

/** 사내 문서 수정·삭제·복구 인바운드 포트 (§4·§5·§6). 모두 ADMIN 전용. */
public interface CompanyDocumentCommandUseCase {

    /** §4 표시명·카테고리 수정. 보낸 것만 반영. */
    CompanyDocumentUpdateResult update(UpdateCompanyDocumentCommand command);

    /** §5 soft delete + 인덱스 제외 트리거. */
    CompanyDocumentDeleteResult delete(DeleteCompanyDocumentCommand command);

    /** §6 복구 + 인덱스 재등록 트리거. */
    CompanyDocumentRestoreResult restore(RestoreCompanyDocumentCommand command);
}
