package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository;

import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.VitamateAnalysisEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import com.group3.vitamins.vitamate.analysis.domain.model.AnalysisStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

// 비타메이트 분석 요청 엔티티를 저장하고 멱등성 키로 조회하는 JPA Repository
public interface VitamateAnalysisJpaRepository extends JpaRepository<VitamateAnalysisEntity, Long> {

    // 비타메이트 블록, 요청자, 멱등성 키 조합으로 기존 분석 요청을 찾는다.
    Optional<VitamateAnalysisEntity> findByVitamateBlockIdAndRequestedByAndIdempotencyKey(
            Long vitamateBlockId,
            String requestedBy,
            String idempotencyKey
    );

    // PENDING 분석 요청을 워커가 처리 중인 상태로 선점한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update VitamateAnalysisEntity analysis
        set analysis.analysisStatus = :processingStatus,
            analysis.processingAttemptId = :attemptId,
            analysis.processingStartedAt = :startedAt,
            analysis.leaseExpiresAt = :leaseExpiresAt,
            analysis.updatedAt = :startedAt
        where analysis.id = :analysisId
          and analysis.analysisStatus = :pendingStatus
          and analysis.deletedAt is null
        """)
    int markProcessing(
            @Param("analysisId") Long analysisId,
            @Param("pendingStatus") AnalysisStatus pendingStatus,
            @Param("processingStatus") AnalysisStatus processingStatus,
            @Param("attemptId") String attemptId,
            @Param("startedAt") LocalDateTime startedAt,
            @Param("leaseExpiresAt") LocalDateTime leaseExpiresAt
    );

    // 현재 워커 시도와 lease가 유효할 때만 분석 결과를 COMPLETED로 저장한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update VitamateAnalysisEntity analysis
        set analysis.analysisStatus = :completedStatus,
            analysis.result = :result,
            analysis.completedAt = :completedAt,
            analysis.leaseExpiresAt = null,
            analysis.updatedAt = :completedAt
        where analysis.id = :analysisId
          and analysis.analysisStatus = :processingStatus
          and analysis.processingAttemptId = :attemptId
          and analysis.leaseExpiresAt > :completedAt
          and analysis.deletedAt is null
        """)
    int markCompleted(
            @Param("analysisId") Long analysisId,
            @Param("processingStatus") AnalysisStatus processingStatus,
            @Param("completedStatus") AnalysisStatus completedStatus,
            @Param("attemptId") String attemptId,
            @Param("result") String result,
            @Param("completedAt") LocalDateTime completedAt
    );

    // 현재 워커 시도와 lease가 유효할 때만 PROCESSING 분석을 FAILED로 저장한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update VitamateAnalysisEntity analysis
        set analysis.analysisStatus = :failedStatus,
            analysis.errorMessage = :errorMessage,
            analysis.completedAt = :failedAt,
            analysis.leaseExpiresAt = null,
            analysis.updatedAt = :failedAt
        where analysis.id = :analysisId
          and analysis.analysisStatus = :processingStatus
          and analysis.processingAttemptId = :attemptId
          and analysis.leaseExpiresAt > :failedAt
          and analysis.deletedAt is null
        """)
    int markFailedFromProcessing(
            @Param("analysisId") Long analysisId,
            @Param("processingStatus") AnalysisStatus processingStatus,
            @Param("failedStatus") AnalysisStatus failedStatus,
            @Param("attemptId") String attemptId,
            @Param("errorMessage") String errorMessage,
            @Param("failedAt") LocalDateTime failedAt
    );

    // 아직 처리 시작 전인 PENDING 분석을 FAILED로 마감한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update VitamateAnalysisEntity analysis
        set analysis.analysisStatus = :failedStatus,
            analysis.errorMessage = :errorMessage,
            analysis.completedAt = :failedAt,
            analysis.updatedAt = :failedAt
        where analysis.id = :analysisId
          and analysis.analysisStatus = :pendingStatus
          and analysis.deletedAt is null
        """)
    int markFailedFromPending(
            @Param("analysisId") Long analysisId,
            @Param("pendingStatus") AnalysisStatus pendingStatus,
            @Param("failedStatus") AnalysisStatus failedStatus,
            @Param("errorMessage") String errorMessage,
            @Param("failedAt") LocalDateTime failedAt
    );
}
