package com.group3.vitamins.vitamate.fileindex.application.service;

import com.group3.vitamins.vitamate.fileindex.application.port.VitamateFileIndexStorePort;
import com.group3.vitamins.vitamate.fileindex.domain.model.FileIndexStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// 큐 발행 실패를 별도 트랜잭션(REQUIRES_NEW)으로 확정 저장한다.
// afterCommit 콜백 안에서 호출되므로 호출자 트랜잭션은 이미 끝난 상태라 REQUIRES_NEW가 아니어도
// 동작은 하지만, 이 기록만은 호출 경로(동기/afterCommit)와 무관하게 항상 독립적으로 커밋되도록
// 명시적으로 분리한다 — 나중에 publish() 호출 지점이 바뀌어도 이 보장이 깨지지 않게 하기 위함.
@Component
@RequiredArgsConstructor
public class VitamateFileIndexFailureRecorder {

    private final VitamateFileIndexStorePort fileIndexStorePort;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long fileVersionId, String errorMessage) {
        fileIndexStorePort.upsertStatus(
                fileVersionId,
                null,
                FileIndexStatus.FAILED,
                errorMessage,
                false,
                LocalDateTime.now()
        );
    }
}
