package com.group3.vitamins.project.block.infrastructure.persistence;

import com.group3.vitamins.project.block.domain.model.BlockType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataBlockRepository extends JpaRepository<BlockJpaEntity, Long> {

    Optional<BlockJpaEntity> findByBlockIdAndDeletedAtIsNull(Long blockId);

    List<BlockJpaEntity> findByStepIdAndDeletedAtIsNullOrderByRowIndexAscSortOrderAsc(Long stepId);

    List<BlockJpaEntity> findByBlockIdInAndDeletedAtIsNull(Collection<Long> blockIds);

    List<BlockJpaEntity> findByStepIdInAndDeletedAtIsNullOrderByStepIdAscRowIndexAscSortOrderAsc(
            Collection<Long> stepIds);

    /**
     * 프로젝트의 살아있는 블록 수 (복제 상한 판정 · PRJ-018).
     * {@code block.project_id} 가 없어 {@code step} 을 조인한다 — {@code idx_step_project} + {@code idx_block_step} 커버.
     */
    @Query("select count(b) from BlockJpaEntity b, StepJpaEntity s "
            + "where b.stepId = s.stepId and s.projectId = :projectId "
            + "and b.deletedAt is null and s.deletedAt is null")
    int countByProjectId(@Param("projectId") Long projectId);

    boolean existsByStepIdAndTypeAndDeletedAtIsNull(Long stepId, BlockType type);

    Optional<BlockJpaEntity> findByTypeAndTypeIdAndDeletedAtIsNull(BlockType type, Long typeId);

    /** 블록이 없으면 null 을 돌려준다 (JPQL MAX 의 동작). */
    @Query("select max(b.rowIndex) from BlockJpaEntity b "
            + "where b.stepId = :stepId and b.deletedAt is null")
    Integer findMaxRowIndex(@Param("stepId") Long stepId);

    /** 그 행에 블록이 없으면 null 을 돌려준다. */
    @Query("select max(b.sortOrder) from BlockJpaEntity b "
            + "where b.stepId = :stepId and b.rowIndex = :rowIndex and b.deletedAt is null")
    Integer findMaxSortOrder(@Param("stepId") Long stepId, @Param("rowIndex") int rowIndex);

    /**
     * 기대 버전이 일치할 때만 제목·담당자를 덮어쓴다. 0 이면 충돌이다.
     *
     * <p>⚠️ {@code clearAutomatically}·{@code flushAutomatically} 를 빼면 <b>조용히 깨진다.</b>
     * 같은 트랜잭션에서 조회한 엔티티가 영속성 컨텍스트에 남아 UPDATE 후에도 낡은 값을 읽는다
     * (`.ai/docs/global/CONCURRENCY.md` §6-2).
     *
     * <p>⚠️ 부분 수정(titleProvided·ownerProvided)이라도 <b>두 필드를 모두 SET 한다</b> —
     * 도메인이 "안 건드린 필드는 원래 값" 까지 반영해 둔 최종값을 넘기기 때문이다.
     * 요청에 실린 값을 그대로 넘기면 생략한 필드가 null 로 지워진다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BlockJpaEntity b set b.title = :title, b.owner = :owner, "
            + "b.updatedAt = :updatedAt, b.version = b.version + 1 "
            + "where b.blockId = :blockId and b.version = :expectedVersion "
            + "and b.deletedAt is null")
    int updateIfVersionMatches(@Param("blockId") Long blockId,
                               @Param("title") String title,
                               @Param("owner") String owner,
                               @Param("updatedAt") LocalDateTime updatedAt,
                               @Param("expectedVersion") int expectedVersion);

    /** 기대 버전이 일치할 때만 배치를 옮긴다. 0 이면 충돌이다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BlockJpaEntity b set b.rowIndex = :rowIndex, b.sortOrder = :sortOrder, "
            + "b.colSpan = :colSpan, b.updatedAt = :updatedAt, b.version = b.version + 1 "
            + "where b.blockId = :blockId and b.version = :expectedVersion "
            + "and b.deletedAt is null")
    int relocateIfVersionMatches(@Param("blockId") Long blockId,
                                 @Param("rowIndex") int rowIndex,
                                 @Param("sortOrder") int sortOrder,
                                 @Param("colSpan") int colSpan,
                                 @Param("updatedAt") LocalDateTime updatedAt,
                                 @Param("expectedVersion") int expectedVersion);

    /** 기대 버전이 일치할 때만 다른 스텝으로 옮긴다. 0 이면 충돌이다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update BlockJpaEntity b set b.stepId = :stepId, b.rowIndex = :rowIndex, "
            + "b.sortOrder = :sortOrder, b.updatedAt = :updatedAt, b.version = b.version + 1 "
            + "where b.blockId = :blockId and b.version = :expectedVersion "
            + "and b.deletedAt is null")
    int moveToStepIfVersionMatches(@Param("blockId") Long blockId,
                                   @Param("stepId") Long stepId,
                                   @Param("rowIndex") int rowIndex,
                                   @Param("sortOrder") int sortOrder,
                                   @Param("updatedAt") LocalDateTime updatedAt,
                                   @Param("expectedVersion") int expectedVersion);
}