package com.group3.vitamins.file.application.usecase;

import com.group3.vitamins.file.application.command.AttachStagedFileCommand;
import com.group3.vitamins.file.application.result.AttachStagedFileResult;

/**
 * 입찰 검토 파일 귀속 인바운드 포트 (FILE-V1 §2-G).
 *
 * <p>입찰 도메인이 프로젝트 생성 확정 시 in-process 로 호출한다(REST 내부 엔드포인트 아님).
 * 파일 도메인은 임시 객체를 정식 키로 복사·검증하고 정식 등록만 한다 — 임시 객체 삭제는 입찰 도메인 소관(PROMOTE-008).
 */
public interface AttachStagedFileUseCase {

    AttachStagedFileResult attach(AttachStagedFileCommand command);
}
