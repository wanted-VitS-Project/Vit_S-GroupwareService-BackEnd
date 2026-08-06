package com.group3.vitamins.file.domain.repository;

import com.group3.vitamins.file.domain.model.FileVersion;

import java.util.Optional;

/**
 * 파일 버전 영속성 아웃바운드 포트.
 *
 * <p>구현체는 {@code infrastructure/persistence/FileVersionRepositoryAdapter} (JPA).
 * 버전 이력 목록(§8)·단건(§11) 같은 화면용 조회는 MyBatis 조회 포트가 담당한다(그 엔드포인트 구현 시점).
 */
public interface FileVersionRepository {

    /** 업로드 시작(UPLOADING)·완료 통보(COMPLETED/FAILED) 전이를 저장한다. */
    FileVersion save(FileVersion fileVersion);

    /** 업로드 완료 통보(§2)에서 대상 버전을 찾는다. */
    Optional<FileVersion> findById(Long fileVersionId);

    /** 새 버전 차수 계산용 — 해당 문서의 현재 최대 versionNo(없으면 0). */
    int findMaxVersionNo(Long fileId);
}
