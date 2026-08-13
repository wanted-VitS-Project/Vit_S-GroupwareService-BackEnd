package com.group3.vitamins.companydocument.application.usecase;

import com.group3.vitamins.companydocument.application.command.CompleteCompanyDocumentUploadCommand;
import com.group3.vitamins.companydocument.application.command.StartCompanyDocumentUploadCommand;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentUploadStartResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionDetailResult;

/** 사내 문서 업로드 2단계 인바운드 포트 (§1 시작 · §2 완료 통보). */
public interface CompanyDocumentUploadUseCase {

    /** §1 업로드 시작 — 문서/버전을 UPLOADING 으로 만들고 presigned PUT URL(10분)을 발급한다. */
    CompanyDocumentUploadStartResult startUpload(StartCompanyDocumentUploadCommand command);

    /** §2 완료 통보 — 저장소 HEAD 검증 후 COMPLETED 로 확정하고 인덱싱 트리거를 발행한다. */
    CompanyDocumentVersionDetailResult completeUpload(CompleteCompanyDocumentUploadCommand command);
}
