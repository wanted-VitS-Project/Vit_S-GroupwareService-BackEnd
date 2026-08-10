package com.group3.vitamins.bidding.collectionrun.application.service;

import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunTaskFailure;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunOutboxStorePort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunTaskPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollectionRunTaskFailureService {

    private final CollectionRunTaskPort taskPort;
    private final CollectionRunOutboxStorePort outboxStorePort;

    // Task 영구 실패와 DLQ 발행 대기를 하나의 DB 트랜잭션으로 기록합니다.
    @Transactional
    public boolean recordPermanentFailure(
            CollectionRunTaskFailure failure,
            String errorCode,
            String errorMessage,
            LocalDateTime failedAt
    ) {
        boolean failed = taskPort.fail(
                failure.taskId(),
                failure.attemptId(),
                errorCode,
                errorMessage,
                failedAt
        );
        if (!failed) {
            return false;
        }

        outboxStorePort.saveTaskFailurePending(
                UUID.randomUUID().toString(),
                failure,
                failedAt
        );
        return true;
    }
}
