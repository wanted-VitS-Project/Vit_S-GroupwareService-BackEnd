package com.group3.vitamins.file.application.usecase;

import com.group3.vitamins.file.application.result.BlockFileListResult;
import com.group3.vitamins.file.application.result.DownloadUrlResult;
import com.group3.vitamins.file.application.result.FilePreviewResult;
import com.group3.vitamins.file.application.result.FileVersionSingleResult;
import com.group3.vitamins.file.application.result.ProjectFileVersionResult;
import com.group3.vitamins.file.application.result.VersionHistoryResult;

import java.util.List;

/**
 * 파일 조회 인바운드 포트 (#134 조회 5종 + #138 버전목록). §1~§5·§8~§11 은 스텝 접근 권한(VIEWER 이상)을,
 * 버전 목록(§11, #138)은 프로젝트 접근 권한(VIEWER 이상)을 따른다. 엔드포인트를 추가할 때마다 메서드를 더한다.
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

    /**
     * 프로젝트 파일 버전 목록(§11, #138) — 비타메이트 분석 선택용. 프로젝트 전체 문서의 완료 버전(과거 버전 포함,
     * 고아 파일 포함, 휴지통 제외)을 돌려준다. 프로젝트 접근 권한(VIEWER 이상)을 따른다.
     */
    List<ProjectFileVersionResult> getProjectFileVersions(Long projectId, String requesterUserId, String role);
}
