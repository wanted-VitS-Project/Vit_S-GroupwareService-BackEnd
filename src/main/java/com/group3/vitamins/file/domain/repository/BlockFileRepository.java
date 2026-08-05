package com.group3.vitamins.file.domain.repository;

/**
 * {@code block_file} 링크 영속성 아웃바운드 포트 (파일 소유 테이블).
 *
 * <p>업로드로 새 문서가 생기면 그 문서를 블록에 연결한다(§1·§2). 새 버전만 올리는 경우엔 이미 링크가 있으므로
 * 호출하지 않는다. 링크 해제는 파일 영구삭제(§7)의 {@code ON DELETE CASCADE} 로 처리된다(별도 unlink 없음).
 */
public interface BlockFileRepository {

    /** 블록에 파일을 연결한다. */
    void link(Long blockId, Long fileId, String linkedBy);
}
