package com.group3.vitamins.file.application.usecase;

import com.group3.vitamins.file.application.command.StartFileUploadCommand;
import com.group3.vitamins.file.application.result.FileUploadStartResult;

/**
 * 파일 업로드 인바운드 포트. §1 업로드 시작(presigned 발급)과 §2 완료 통보를 담당한다.
 * (§2 완료 통보 메서드는 다음 단계에서 추가한다.)
 */
public interface FileUploadUseCase {

    /** 업로드 시작 — 문서/버전 레코드를 UPLOADING 으로 만들고 presigned PUT URL 을 발급한다. */
    FileUploadStartResult startUpload(StartFileUploadCommand command);
}
