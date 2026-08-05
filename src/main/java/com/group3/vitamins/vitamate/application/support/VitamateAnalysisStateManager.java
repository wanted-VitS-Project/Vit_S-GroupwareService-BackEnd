package com.group3.vitamins.vitamate.application.support;

import com.group3.vitamins.vitamate.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.application.result.StartVitamateAnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

// 비타메이트 분석 상태 전이를 한 곳에서 관리하는 내부 컴포넌트
@Component
@RequiredArgsConstructor
public class VitamateAnalysisStateManager {

    private static final Duration PROCESSING_LEASE_DURATION = Duration.ofMinutes(10);

    private final VitamateAnalysisStorePort analysisStore;

    // PENDING 분석 요청을 PROCESSING 상태로 선점한다.
    // 요청 저장 커밋 이후에도 호출될 수 있어 항상 새 트랜잭션에서 상태를 바꾼다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<StartVitamateAnalysisResult> startProcessing(Long analysisId) {
        validateAnalysisId(analysisId);

        String attemptId = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime leaseExpiresAt = startedAt.plus(PROCESSING_LEASE_DURATION);

        boolean started = analysisStore.markProcessing(
                analysisId,
                attemptId,
                startedAt,
                leaseExpiresAt
        );

        if (!started) {
            return Optional.empty();
        }

        return Optional.of(new StartVitamateAnalysisResult(
                analysisId,
                attemptId,
                startedAt,
                leaseExpiresAt
        ));
    }

    // 현재 워커 시도가 유효할 때 분석 결과를 COMPLETED로 저장한다.
    @Transactional
    public boolean completeProcessing(Long analysisId, String attemptId, String result) {
        validateAnalysisId(analysisId);
        validateRequired(attemptId, "attemptId");
        validateRequired(result, "result");

        return analysisStore.markCompleted(
                analysisId,
                attemptId,
                result,
                LocalDateTime.now()
        );
    }

    // 현재 워커 시도가 유효할 때 PROCESSING 분석을 FAILED로 마감한다.
    // 큐 발행 실패처럼 기존 트랜잭션이 끝난 뒤 호출되는 경로를 고려해 새 트랜잭션을 사용한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean failProcessing(Long analysisId, String attemptId, String errorMessage) {
        validateAnalysisId(analysisId);
        validateRequired(attemptId, "attemptId");
        validateRequired(errorMessage, "errorMessage");

        return analysisStore.markFailedFromProcessing(
                analysisId,
                attemptId,
                errorMessage,
                LocalDateTime.now()
        );
    }

    // 아직 처리되지 않은 PENDING 분석을 FAILED로 마감한다.
    @Transactional
    public boolean failPending(Long analysisId, String errorMessage) {
        validateAnalysisId(analysisId);
        validateRequired(errorMessage, "errorMessage");

        return analysisStore.markFailedFromPending(
                analysisId,
                errorMessage,
                LocalDateTime.now()
        );
    }

    // 분석 ID가 비어 있거나 잘못된 값인지 확인한다.
    private void validateAnalysisId(Long analysisId) {
        if (analysisId == null || analysisId <= 0) {
            throw new IllegalArgumentException("analysisId must be positive.");
        }
    }

    // 필수 문자열 값이 비어 있는지 확인한다.
    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }
    }
}
