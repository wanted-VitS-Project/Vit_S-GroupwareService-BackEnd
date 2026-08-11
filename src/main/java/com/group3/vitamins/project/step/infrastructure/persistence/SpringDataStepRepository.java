package com.group3.vitamins.project.step.infrastructure.persistence;

import com.group3.vitamins.project.step.domain.model.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataStepRepository extends JpaRepository<StepJpaEntity, Long> {

    /** 스텝이 없으면 null 을 돌려준다 (JPQL MAX 의 동작). */
    @Query("select max(s.sortOrder) from StepJpaEntity s "
            + "where s.projectId = :projectId and s.deletedAt is null")
    Integer findMaxSortOrder(@Param("projectId") Long projectId);

    Optional<StepJpaEntity> findByStepIdAndDeletedAtIsNull(Long stepId);

    List<StepJpaEntity> findByProjectIdAndDeletedAtIsNullOrderBySortOrderAsc(Long projectId);

    List<StepJpaEntity> findByProjectIdAndStatusAndDeletedAtIsNullOrderBySortOrderAsc(
            Long projectId, StepStatus status);

    List<StepJpaEntity> findByStepIdInAndProjectIdAndDeletedAtIsNull(
            Collection<Long> stepIds, Long projectId);

    List<StepJpaEntity> findByStageIdAndDeletedAtIsNull(Long stageId);

    /**
     * 기대 버전이 일치할 때만 이름·기간·책임자를 덮어쓴다. 0 이면 충돌이다.
     *
     * <p>⚠️ {@code clearAutomatically}·{@code flushAutomatically} 를 빼면 <b>조용히 깨진다.</b>
     * 같은 트랜잭션에서 조회한 엔티티가 영속성 컨텍스트에 남아 UPDATE 후에도 낡은 값을 읽는다
     * (`.ai/docs/global/CONCURRENCY.md` §6-2).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StepJpaEntity s set s.name = :name, s.startedOn = :startedOn, "
            + "s.endedOn = :endedOn, s.ownerUserId = :ownerUserId, s.updatedAt = :updatedAt, "
            + "s.version = s.version + 1 "
            + "where s.stepId = :stepId and s.version = :expectedVersion "
            + "and s.deletedAt is null")
    int updateIfVersionMatches(@Param("stepId") Long stepId,
                               @Param("name") String name,
                               @Param("startedOn") LocalDate startedOn,
                               @Param("endedOn") LocalDate endedOn,
                               @Param("ownerUserId") String ownerUserId,
                               @Param("updatedAt") LocalDateTime updatedAt,
                               @Param("expectedVersion") int expectedVersion);

    /**
     * 기대 버전이 일치할 때만 상태를 바꾼다. 0 이면 충돌이다.
     *
     * <p>⚠️ <b>완료 정보(completedAt·completedBy)까지 함께 SET 한다.</b> DONE 에서 벗어나면
     * 두 값을 지워야 하는데(도메인 {@code changeStatus} 규칙), 여기서 안 넘기면 진행 중인데
     * 완료자·완료시각이 남아 조회 화면이 어긋난다. 규칙이 도메인과 SQL 두 곳으로 갈라지지 않도록
     * <b>도메인이 계산한 결과값을 그대로 받아</b> 넘긴다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StepJpaEntity s set s.status = :status, s.completedAt = :completedAt, "
            + "s.completedBy = :completedBy, s.updatedAt = :updatedAt, "
            + "s.version = s.version + 1 "
            + "where s.stepId = :stepId and s.version = :expectedVersion "
            + "and s.deletedAt is null")
    int changeStatusIfVersionMatches(@Param("stepId") Long stepId,
                                     @Param("status") StepStatus status,
                                     @Param("completedAt") LocalDateTime completedAt,
                                     @Param("completedBy") String completedBy,
                                     @Param("updatedAt") LocalDateTime updatedAt,
                                     @Param("expectedVersion") int expectedVersion);

    /** 기대 버전이 일치할 때만 위치를 옮긴다. stageId 가 null 이면 미소속이다. 0 이면 충돌이다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StepJpaEntity s set s.stageId = :stageId, s.sortOrder = :sortOrder, "
            + "s.updatedAt = :updatedAt, s.version = s.version + 1 "
            + "where s.stepId = :stepId and s.version = :expectedVersion "
            + "and s.deletedAt is null")
    int moveIfVersionMatches(@Param("stepId") Long stepId,
                             @Param("stageId") Long stageId,
                             @Param("sortOrder") int sortOrder,
                             @Param("updatedAt") LocalDateTime updatedAt,
                             @Param("expectedVersion") int expectedVersion);
}