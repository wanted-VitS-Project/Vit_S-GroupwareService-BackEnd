package com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.vitamate.filecleanup.application.model.VitamateCleanupJob;
import com.group3.vitamins.vitamate.filecleanup.application.port.VitamateCleanupJobStorePort;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity.VitamateCleanupJobEntity;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.entity.VitamateCleanupOutboxEntity;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.repository.VitamateCleanupJobJpaRepository;
import com.group3.vitamins.vitamate.filecleanup.infrastructure.persistence.repository.VitamateCleanupOutboxJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// 파일 버전 목록을 확보해 cleanup job과 outbox를 같은 트랜잭션에 저장합니다.
@Component
@RequiredArgsConstructor
public class JpaVitamateCleanupJobStoreAdapter implements VitamateCleanupJobStorePort {

    private static final String EVENT_TYPE = "CHROMA_VECTOR_DELETE_REQUESTED";

    private final VitamateCleanupJobJpaRepository cleanupJobRepository;
    private final VitamateCleanupOutboxJpaRepository cleanupOutboxRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @Override
    public void createCleanupJob(Long fileId) {
        List<Long> fileVersionIds = findFileVersionIds(fileId);
        if (fileVersionIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String cleanupKey = UUID.randomUUID().toString();
        JsonNode fileVersionIdsJson = toJson(fileVersionIds);

        VitamateCleanupJobEntity cleanupJob = cleanupJobRepository.saveAndFlush(
                VitamateCleanupJobEntity.waiting(cleanupKey, fileId, fileVersionIdsJson, now)
        );

        JsonNode payload = toJson(new CleanupRequestedPayload(
                cleanupJob.getCleanupJobId(),
                cleanupKey,
                fileVersionIds
        ));

        cleanupOutboxRepository.save(VitamateCleanupOutboxEntity.pending(
                UUID.randomUUID().toString(),
                cleanupJob,
                EVENT_TYPE,
                payload,
                now
        ));
        }
        // 현재 시도의 cleanup 작업을 처리 중 상태로 변경합니다.
        @Override
        public boolean markProcessing(
                Long cleanupJobId,
                String attemptId,
                LocalDateTime processingStartedAt
    ) {
            int updatedCount = cleanupJobRepository.markProcessing(
                    cleanupJobId,
                    attemptId,
                    VitamateCleanupJob.Status.PUBLISHED,
                    VitamateCleanupJob.Status.PROCESSING,
                    processingStartedAt
            );

            return updatedCount == 1;
        }

        // 현재 시도의 cleanup 작업을 완료 상태로 변경합니다.
        @Override
        public boolean markCompleted(
                Long cleanupJobId,
                String attemptId,
        int deletedVectorCount,
        LocalDateTime completedAt
        ) {
            int updatedCount = cleanupJobRepository.markCompleted(
                    cleanupJobId,
                    attemptId,
                    deletedVectorCount,
                    activeStatuses(),
                    VitamateCleanupJob.Status.COMPLETED,
                    completedAt
            );

            return updatedCount == 1;
        }// 재시도 상태 변경과 다음 Redis 발행용 Outbox 생성을 함께 처리합니다.
    @Override
    @Transactional
    public boolean scheduleRetry(
            Long cleanupJobId,
            String attemptId,
            String errorCode,
            String errorMessage,
            int maxAttempts,
            LocalDateTime nextRetryAt,
            LocalDateTime updatedAt
    ) {
        int updatedCount = cleanupJobRepository.markRetryWaiting(
                cleanupJobId,
                attemptId,
                errorCode,
                errorMessage,
                maxAttempts,
                nextRetryAt,
                updatedAt,
                activeStatuses(),
                VitamateCleanupJob.Status.RETRY_WAIT
        );

        if (updatedCount != 1) {
            return false;
        }

        VitamateCleanupJobEntity cleanupJob = cleanupJobRepository
                .findById(cleanupJobId)
                .orElseThrow(() -> new IllegalStateException(
                        "재시도할 cleanup job을 찾을 수 없습니다."
                ));

        List<Long> fileVersionIds = new java.util.ArrayList<>();
        cleanupJob.getFileVersionIds().forEach(node ->
                fileVersionIds.add(node.asLong())
        );

        JsonNode payload = toJson(new CleanupRequestedPayload(
                cleanupJobId,
                cleanupJob.getCleanupKey(),
                fileVersionIds
        ));

        cleanupOutboxRepository.save(
                VitamateCleanupOutboxEntity.pending(
                        UUID.randomUUID().toString(),
                        cleanupJob,
                        EVENT_TYPE,
                        payload,
                        nextRetryAt
                )
        );

        return true;
    }

        // 재시도할 수 없는 실패를 최종 실패 상태로 변경합니다.
        @Override
        public boolean markDeadLetter(
                Long cleanupJobId,
                String attemptId,
                String errorCode,
                String errorMessage,
                LocalDateTime completedAt
    ) {
            int updatedCount = cleanupJobRepository.markDeadLetter(
                    cleanupJobId,
                    attemptId,
                    errorCode,
                    errorMessage,
                    activeStatuses(),
                    VitamateCleanupJob.Status.DEAD_LETTER,
                    completedAt
            );

            return updatedCount == 1;
        }

        // callback이 거부됐을 때 응답할 현재 cleanup 상태를 조회합니다.
        @Override
        public Optional<VitamateCleanupJob.Status> findStatus(Long cleanupJobId) {
            return cleanupJobRepository.findCleanupStatusById(cleanupJobId);
        }

        // callback 처리 시 재시도 간격과 최대 처리 횟수를 판단할 값을 조회합니다.
        @Override
        public Optional<Integer> findAttemptCount(Long cleanupJobId) {
            return cleanupJobRepository.findAttemptCountById(cleanupJobId);
        }

        // worker callback을 받을 수 있는 진행 상태 목록을 반환합니다.
        private List<VitamateCleanupJob.Status> activeStatuses() {
            return List.of(
                    VitamateCleanupJob.Status.PUBLISHED,
                    VitamateCleanupJob.Status.PROCESSING
            );

    }

    // 파일이 삭제되기 전에 모든 버전 ID를 오름차순으로 조회합니다.
    private List<Long> findFileVersionIds(Long fileId) {
        List<?> rows = entityManager.createNativeQuery("""
                        SELECT file_version_id
                        FROM file_version
                        WHERE file_id = :fileId
                        ORDER BY file_version_id
                        """)
                .setParameter("fileId", fileId)
                .getResultList();

        return rows.stream()
                .map(value -> ((Number) value).longValue())
                .toList();
    }


    // 구조화된 값을 MySQL JSON 컬럼에 저장할 문자열로 변환합니다.
    private JsonNode toJson(Object value) {
        return objectMapper.valueToTree(value);
    }

    private record CleanupRequestedPayload(
            Long cleanupJobId,
            String cleanupKey,
            List<Long> fileVersionIds
    ) {
    }
}
