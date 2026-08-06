package com.group3.vitamins.file.application.usecase;

import com.group3.vitamins.file.application.command.CompleteFileUploadCommand;
import com.group3.vitamins.file.application.command.StartFileUploadCommand;
import com.group3.vitamins.file.application.result.FileUploadStartResult;
import com.group3.vitamins.file.application.result.FileVersionDetailResult;

/**
 * 파일 업로드 인바운드 포트. §1 업로드 시작(presigned 발급)과 §2 완료 통보를 담당한다.
 */
public interface FileUploadUseCase {

    /** 업로드 시작 — 문서/버전 레코드를 UPLOADING 으로 만들고 presigned PUT URL 을 발급한다. */
    FileUploadStartResult startUpload(StartFileUploadCommand command);

    /** 완료 통보 — 저장소 HEAD 검증·PDF 페이지 수 추출 후 버전을 COMPLETED 로 확정한다. */
    FileVersionDetailResult completeUpload(CompleteFileUploadCommand command);
}
