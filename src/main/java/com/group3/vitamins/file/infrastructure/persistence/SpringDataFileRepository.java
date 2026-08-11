package com.group3.vitamins.file.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

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
     * 덮어쓰기(§5)용 — 문서 행을 비관 잠금(SELECT … FOR UPDATE)하고 읽는다. 삭제/부재면 empty.
     * 잠금을 잡은 뒤 그 version 으로 조건부 UPDATE 를 돌리면, 잠금이 커밋까지 유지되므로 version 이
     * 변하지 않아 항상 성공하고 결과 version(=현재+1)이 확정된다. 재조회로 응답 version 을 얻으면
     * 그새 커밋된 다른 수정 값과 섞일 수 있어(비원자적) 이 방식을 쓴다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT f
              FROM FileJpaEntity f
             WHERE f.fileId = :fileId
               AND f.deletedAt IS NULL
            """)
    Optional<FileJpaEntity> findForUpdate(@Param("fileId") Long fileId);
}
