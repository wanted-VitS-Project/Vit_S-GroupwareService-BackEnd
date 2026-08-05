package com.group3.vitamins.file.application.port;

/**
 * 파일 화면용 조회 아웃바운드 포트 (MyBatis). 목록·버전이력 등은 각 엔드포인트 구현 시 확장한다.
 * 구현은 {@code infrastructure/adapter/FileQueryAdapter}.
 */
public interface FileQueryPort {

    /** 블록 안에 같은 표시명의 살아있는 문서가 있는지(§1 동명 확인). block_file ⋈ file, deleted_at IS NULL. */
    boolean existsActiveNameInBlock(Long blockId, String name);

    /** 문서가 연결된 블록 ID(권한 판정 경로 fileId→block→step). 파일 1 : 블록 1. 링크 없으면 empty. */
    java.util.Optional<Long> findBlockIdByFileId(Long fileId);

    /** 문서의 완료된 버전 목록(§8 이력) — 차수 내림차순. 실패·미완료 버전은 제외. */
    java.util.List<com.group3.vitamins.file.application.result.FileVersionProjection> findCompletedVersions(Long fileId);

    /** 블록의 파일 목록(§3) — 문서별 최신 완료 버전 + 버전 수. deleted=true 면 휴지통, false 면 재직 문서. 연결일 오름차순. */
    java.util.List<com.group3.vitamins.file.application.result.BlockFileProjection> findBlockFiles(Long blockId, boolean deleted);
}
