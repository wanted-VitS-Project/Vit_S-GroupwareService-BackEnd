package com.group3.vitamins.project.stage.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataStageRepository extends JpaRepository<StageJpaEntity, Long> {

    /** 스테이지가 없으면 null 을 돌려준다 (JPQL MAX 의 동작). */
    @Query("select max(s.sortOrder) from StageJpaEntity s "
            + "where s.projectId = :projectId and s.deletedAt is null")
    Integer findMaxSortOrder(@Param("projectId") Long projectId);

    List<StageJpaEntity> findByProjectIdAndDeletedAtIsNullOrderBySortOrderAsc(Long projectId);

    Optional<StageJpaEntity> findByStageIdAndDeletedAtIsNull(Long stageId);

    List<StageJpaEntity> findByStageIdInAndProjectIdAndDeletedAtIsNull(
            Collection<Long> stageIds, Long projectId);

    boolean existsByStageIdAndProjectIdAndDeletedAtIsNull(Long stageId, Long projectId);

    /**
     * 기대 버전이 일치할 때만 이름을 바꾼다. 반환값이 영향 행 수이고, 0 이면 충돌이다.
     *
     * <p>⚠️ {@code clearAutomatically}·{@code flushAutomatically} 를 빼면 <b>조용히 깨진다.</b>
     * 같은 트랜잭션에서 조회한 엔티티가 영속성 컨텍스트에 남아 UPDATE 후에도 낡은 값을 읽는다
     * (`.ai/docs/global/CONCURRENCY.md` §6-2).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StageJpaEntity s set s.name = :name, s.version = s.version + 1 "
            + "where s.stageId = :stageId and s.version = :expectedVersion "
            + "and s.deletedAt is null")
    int renameIfVersionMatches(@Param("stageId") Long stageId,
                               @Param("name") String name,
                               @Param("expectedVersion") int expectedVersion);

    /** 기대 버전이 일치할 때만 정렬 순서를 바꾼다. 0 이면 충돌이다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StageJpaEntity s set s.sortOrder = :sortOrder, s.version = s.version + 1 "
            + "where s.stageId = :stageId and s.version = :expectedVersion "
            + "and s.deletedAt is null")
    int moveIfVersionMatches(@Param("stageId") Long stageId,
                             @Param("sortOrder") int sortOrder,
                             @Param("expectedVersion") int expectedVersion);

    /** 프로젝트의 미삭제 스테이지를 한 번에 논리 삭제한다. version 은 올리지 않는다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StageJpaEntity s set s.deletedAt = :deletedAt "
            + "where s.projectId = :projectId and s.deletedAt is null")
    int deleteByProjectId(@Param("projectId") Long projectId,
                          @Param("deletedAt") LocalDateTime deletedAt);
}