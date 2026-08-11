package com.group3.vitamins.project.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.CloseReasonCode;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SpringDataProjectRepository extends JpaRepository<ProjectJpaEntity, Long> {

    /** 같은 공고로 이미 만들어진 프로젝트가 있는지 확인한다 (`UNIQUE(bid_notice_id)`). */
    /**
     * 논리 삭제분은 제외한다. 삭제 시 bid_notice_id 를 비우므로 삭제분이 걸릴 일은 없지만,
     * 과거 데이터가 남아 있을 수 있어 조회에서도 한 번 더 막는다.
     */
    Optional<ProjectJpaEntity> findByBidNoticeIdAndCompanyIdAndDeletedAtIsNull(
            Long bidNoticeId, Long companyId);

    /** 회사 범위 단건 조회. 논리 삭제분과 타사 프로젝트는 조회하지 않는다. */
    Optional<ProjectJpaEntity> findByProjectIdAndCompanyIdAndDeletedAtIsNull(
            Long projectId, Long companyId);

    /**
     * 기대 버전이 일치할 때만 수정 가능한 6필드를 덮어쓴다. 0 이면 충돌이다.
     *
     * <p>⚠️ {@code clearAutomatically}·{@code flushAutomatically} 를 빼면 <b>조용히 깨진다.</b>
     * 같은 트랜잭션에서 조회한 엔티티가 영속성 컨텍스트에 남아 UPDATE 후에도 낡은 값을 읽는다
     * (`.ai/docs/global/CONCURRENCY.md` §6-2).
     *
     * <p>⚠️ {@code companyId} 조건을 빼지 마라 — 회사 격리는 낙관락과 별개 규칙이라
     * version 이 우연히 맞으면 타사 프로젝트가 수정된다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProjectJpaEntity p set p.name = :name, p.description = :description, "
            + "p.clientName = :clientName, p.startedOn = :startedOn, p.endedOn = :endedOn, "
            + "p.contractAmount = :contractAmount, p.updatedAt = :updatedAt, "
            + "p.version = p.version + 1 "
            + "where p.projectId = :projectId and p.companyId = :companyId "
            + "and p.version = :expectedVersion and p.deletedAt is null")
    int updateIfVersionMatches(@Param("projectId") Long projectId,
                               @Param("companyId") Long companyId,
                               @Param("name") String name,
                               @Param("description") String description,
                               @Param("clientName") String clientName,
                               @Param("startedOn") LocalDate startedOn,
                               @Param("endedOn") LocalDate endedOn,
                               @Param("contractAmount") BigDecimal contractAmount,
                               @Param("updatedAt") LocalDateTime updatedAt,
                               @Param("expectedVersion") int expectedVersion);

    /**
     * 기대 버전이 일치할 때만 상태를 바꾼다. 0 이면 충돌이다.
     *
     * <p>⚠️ <b>종결 정보(closeReasonCode·closeReasonNote·closedAt)까지 함께 SET 한다.</b>
     * CLOSED 에서 벗어나면 셋을 지워야 하는데(도메인 {@code changeStatus} 규칙), 여기서 안 넘기면
     * 진행 중인데 종결 사유·일시가 남아 조회 화면이 어긋난다. 규칙이 도메인과 SQL 두 곳으로
     * 갈라지지 않도록 <b>도메인이 계산한 결과값을 그대로 받아</b> 넘긴다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ProjectJpaEntity p set p.status = :status, "
            + "p.closeReasonCode = :closeReasonCode, p.closeReasonNote = :closeReasonNote, "
            + "p.closedAt = :closedAt, p.updatedAt = :updatedAt, p.version = p.version + 1 "
            + "where p.projectId = :projectId and p.companyId = :companyId "
            + "and p.version = :expectedVersion and p.deletedAt is null")
    int changeStatusIfVersionMatches(@Param("projectId") Long projectId,
                                     @Param("companyId") Long companyId,
                                     @Param("status") ProjectStatus status,
                                     @Param("closeReasonCode") CloseReasonCode closeReasonCode,
                                     @Param("closeReasonNote") String closeReasonNote,
                                     @Param("closedAt") LocalDateTime closedAt,
                                     @Param("updatedAt") LocalDateTime updatedAt,
                                     @Param("expectedVersion") int expectedVersion);
}