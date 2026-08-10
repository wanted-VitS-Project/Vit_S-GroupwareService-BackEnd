package com.group3.vitamins.file.domain.repository;

import com.group3.vitamins.file.domain.model.File;

import java.util.Optional;

/**
 * 파일(논리 문서) 영속성 아웃바운드 포트.
 *
 * <p>구현체는 {@code infrastructure/persistence/FileRepositoryAdapter} (JPA · saveAndFlush).
 * 블록 내 동명 검사·목록 같은 화면용 조회는 별도 MyBatis 조회 포트가 담당한다(§3 구현 시점).
 */
public interface FileRepository {

    /** 생성·수정을 저장한다. 제약 위반을 쓰기 시점에 동기 발생시켜야 하므로 어댑터는 saveAndFlush 로 구현한다. */
    File save(File file);

    /**
     * 낙관락 조건부 표시명 수정(§4). 기대 버전과 DB 버전이 같을 때만 이름을 바꾸고 version 을 +1 한다.
     * 바뀐 행 수를 돌려준다 — 0 이면 충돌(그 사이 남이 먼저 저장)이라 서비스가 409 로 변환한다.
     */
    int renameIfVersionMatches(Long fileId, String name, int expectedVersion);

    /**
     * 덮어쓰기(§5) — 버전 조건 없이 무조건 표시명을 바꾸고 version 을 +1 한다. 바뀐 행 수를 돌려준다
     * (0 = 그새 삭제됨). 갱신된 실제 version 은 호출자가 재조회해서 확인한다.
     */
    int forceRename(Long fileId, String name);

    /** 삭제 여부와 무관하게 문서를 찾는다(복구·상태 판정은 서비스가 deletedAt 으로 한다). */
    Optional<File> findById(Long fileId);

    /**
     * 문서 행을 물리 삭제한다(§7 영구삭제). {@code block_file} 은 {@code ON DELETE CASCADE} 로 함께 지워진다.
     * ⚠️ 버전 행({@code file_version})은 CASCADE 가 없으므로 이 호출 전에 먼저 지워야 한다.
     */
    void deleteById(Long fileId);
}
