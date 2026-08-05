package com.group3.vitamins.file.application.usecase;

import com.group3.vitamins.file.application.result.BlockFileListResult;
import com.group3.vitamins.file.application.result.DownloadUrlResult;
import com.group3.vitamins.file.application.result.FilePreviewResult;
import com.group3.vitamins.file.application.result.FileVersionSingleResult;
import com.group3.vitamins.file.application.result.VersionHistoryResult;

/**
 * 파일 조회 인바운드 포트 (#134 조회 5종). 전부 스텝 접근 권한(VIEWER 이상)을 따른다.
 * 엔드포인트를 추가할 때마다 메서드를 더한다.
 */
public interface FileQueryUseCase {

    /** 다운로드 URL 발급(§9) — 완료된 버전의 presigned GET(5분)을 돌려준다. */
    DownloadUrlResult getDownloadUrl(Long fileVersionId, String requesterUserId, String role);

    /** 버전 단건 조회(§11) — 결재용. 문서가 휴지통이어도 반환한다. */
    FileVersionSingleResult getVersion(Long fileVersionId, String requesterUserId, String role);

    /** 버전 이력 조회(§8) — 완료 버전만 차수 내림차순. */
    VersionHistoryResult getVersionHistory(Long fileId, String requesterUserId, String role);

    /** 블록 파일 목록(§3) — deleted=true 면 휴지통. canEdit 포함. */
    BlockFileListResult getBlockFiles(Long blockId, boolean deleted, String requesterUserId, String role);

    /** 미리보기(§10) — PDF 앞 5페이지만 잘라 반환한다(PDF 만). */
    FilePreviewResult getPreview(Long fileVersionId, String requesterUserId, String role);
}
