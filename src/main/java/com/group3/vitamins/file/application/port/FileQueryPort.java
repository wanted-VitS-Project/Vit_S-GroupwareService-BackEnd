package com.group3.vitamins.file.application.port;

/**
 * 파일 화면용 조회 아웃바운드 포트 (MyBatis). 목록·버전이력 등은 각 엔드포인트 구현 시 확장한다.
 * 구현은 {@code infrastructure/adapter/FileQueryAdapter}.
 */
public interface FileQueryPort {

    /** 블록 안에 같은 표시명의 살아있는 문서가 있는지(§1 동명 확인). block_file ⋈ file, deleted_at IS NULL. */
    boolean existsActiveNameInBlock(Long blockId, String name);
}
