package com.group3.vitamins.file.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataFileRepository extends JpaRepository<FileJpaEntity, Long> {

    /**
     * 기대 버전과 DB 버전이 같을 때만 표시명을 바꾸고 version 을 올린다(낙관락 · CONCURRENCY.md §1-2).
     * 바뀐 행 수를 돌려준다 — 0 이면 그 사이 남이 먼저 저장한 것이라 서비스가 409 로 변환한다.
     *
     * <p>⚠️ {@code clearAutomatically}·{@code flushAutomatically} 를 빼면 같은 트랜잭션에서 조회한
     * 엔티티가 영속성 컨텍스트에 남아 UPDATE 후에도 낡은 값을 읽는다(§6-2).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FileJpaEntity f
               SET f.name = :name,
                   f.version = f.version + 1
             WHERE f.fileId = :fileId
               AND f.version = :expectedVersion
               AND f.deletedAt IS NULL
            """)
    int renameIfVersionMatches(
            @Param("fileId") Long fileId,
            @Param("name") String name,
            @Param("expectedVersion") int expectedVersion
    );

    /**
     * 버전 조건 없이 무조건 표시명을 바꾸고 version 을 올린다(덮어쓰기 · §5).
     * 조회~저장 사이에 남이 먼저 저장해도 충돌로 막지 않는다 — "덮어쓰기 = 무조건 저장" 계약을 지키려면
     * 기대 버전을 조건에 걸면 안 된다. 삭제된 문서는 제외하므로 0 이면 그새 삭제된 것이다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FileJpaEntity f
               SET f.name = :name,
                   f.version = f.version + 1
             WHERE f.fileId = :fileId
               AND f.deletedAt IS NULL
            """)
    int forceRename(
            @Param("fileId") Long fileId,
            @Param("name") String name
    );
}
