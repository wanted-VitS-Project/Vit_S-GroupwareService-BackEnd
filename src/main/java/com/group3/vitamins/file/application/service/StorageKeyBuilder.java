package com.group3.vitamins.file.application.service;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 파일 저장소 키 생성기 — 멀티테넌시 키 규약의 단일 출처.
 *
 * <p>키: {@code companies/{companyId}/projects/{projectId}/files/{fileId}/versions/{versionNo}/{uuid}[.ext]}.
 * 최상위 {@code companies/{companyId}/} 로 회사별 S3 접두사를 분리해 IAM·수명주기·감사·삭제를 회사 단위로 건다.
 * 경로에 fileVersionId 대신 versionNo 를 쓴다 — fileVersionId 는 INSERT 전에 알 수 없고, uuid 가 유일성을 보장한다.
 *
 * <p>업로드({@code FileUploadService})와 입찰 검토 파일 귀속({@code AttachStagedFileService})이 공유한다.
 */
@Component
public class StorageKeyBuilder {

    public String build(long companyId, long projectId, long fileId, int versionNo, String extension) {
        String uuid = UUID.randomUUID().toString();
        String suffix = (extension == null || extension.isEmpty()) ? "" : "." + extension;
        return "companies/%d/projects/%d/files/%d/versions/%d/%s%s"
                .formatted(companyId, projectId, fileId, versionNo, uuid, suffix);
    }
}
