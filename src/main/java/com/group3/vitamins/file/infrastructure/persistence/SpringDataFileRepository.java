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
}
