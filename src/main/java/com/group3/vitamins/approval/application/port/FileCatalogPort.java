package com.group3.vitamins.approval.application.port;

import java.util.Optional;

/**
 * 파일 도메인(김동현님 소관)에 물어보는 아웃바운드 포트 — INV-04({@code file_version_id} 만 참조).
 * Block/Project 포트와 동일하게, 파일 도메인도 아직 구현이 없어 어댑터는 임시 스텁이다.
 * 실제 구현체가 나오면 이 인터페이스는 그대로 두고 어댑터만 교체한다.
 */
public interface FileCatalogPort {

    Optional<FileVersionSummary> findFileVersion(Long fileVersionId);
}
