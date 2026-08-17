package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.vitamate.fileindex.application.command.DispatchVitamateFileIndexCommand;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexJobPublisherPort;
import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.application.usecase.DispatchVitamateFileIndexUseCase;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

// 파일 업로드 완료 시 file_index를 PENDING으로 등록하고, 커밋 후에만 큐에 발행하는 서비스.
// 호출자(FileUploadService)의 트랜잭션에 그대로 참여한다 — file_version 완료와 file_index 생성이
// 항상 함께 성립하거나 함께 롤백돼야 하기 때문이다 (ARCHITECTURE.md §2-2 쓰기 방향 포트 원칙).
@Slf4j
@Service
@RequiredArgsConstructor
public class VitamateFileIndexDispatchService implements DispatchVitamateFileIndexUseCase {

    private static final int INITIAL_RETRY_COUNT = 0;

    private final VitamateFileIndexStorePort fileIndexStorePort;
    private final VitamateFileIndexJobPublisherPort jobPublisherPort;
    private final VitamateFileIndexFailureRecorder failureRecorder;

    @Override
    public void handle(DispatchVitamateFileIndexCommand command) {
        if (command == null || command.fileVersionId() == null) {
            throw new IllegalArgumentException("fileVersionId must not be null.");
        }

        Long fileVersionId = command.fileVersionId();
        LocalDateTime now = LocalDateTime.now();

        fileIndexStorePort.upsertStatus(fileVersionId, null, FileIndexStatus.PENDING, null, false, now);

        registerAfterCommitPublish(fileVersionId, now);
    }

    // 파일 완료 트랜잭션이 실제로 커밋된 뒤에만 큐에 발행한다 — 커밋 전에 발행하면
    // worker가 아직 존재하지 않는 file_index/file_version을 조회하게 된다.
    private void registerAfterCommitPublish(Long fileVersionId, LocalDateTime createdAt) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publish(fileVersionId, createdAt);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publish(fileVersionId, createdAt);
            }
        });
    }

    // 큐 발행에 실패하면 PENDING으로 방치하지 않고 FAILED로 확정한다 —
    // 오늘 겪은 "아무도 모르게 멈춰 있음" 문제를 이 지점에서 다시 만들지 않기 위함.
    private void publish(Long fileVersionId, LocalDateTime createdAt) {
        try {
            jobPublisherPort.publish(new VitamateFileIndexJobPublisherPort.FileIndexJob(
                    fileVersionId,
                    INITIAL_RETRY_COUNT,
                    createdAt
            ));
        } catch (RuntimeException e) {
            log.error("Failed to publish vitamate file index job. fileVersionId={}, reason=queue_publish_failed",
                    fileVersionId, e);
            // REQUIRES_NEW 별도 빈으로 분리 — 호출 경로(동기/afterCommit)와 무관하게 항상 독립 커밋된다.
            failureRecorder.markFailed(fileVersionId, "파일 인덱싱 작업 큐 발행에 실패했습니다.");
        }
    }
}