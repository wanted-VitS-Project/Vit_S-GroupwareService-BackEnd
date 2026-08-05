package com.group3.vitamins.vitamate.application.service;

import com.group3.vitamins.vitamate.application.command.DispatchVitamateAnalysisJobCommand;
import com.group3.vitamins.vitamate.application.port.VitamateAnalysisJobPublisherPort;
import com.group3.vitamins.vitamate.application.result.StartVitamateAnalysisResult;
import com.group3.vitamins.vitamate.application.support.VitamateAnalysisStateManager;
import com.group3.vitamins.vitamate.application.usecase.DispatchVitamateAnalysisJobUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

// 비타메이트 분석 요청을 비동기 작업 큐로 발행하는 서비스
@Slf4j
@Service
@RequiredArgsConstructor
public class VitamateAnalysisJobDispatchService implements DispatchVitamateAnalysisJobUseCase {

    private static final int INITIAL_RETRY_COUNT = 0;

    private final VitamateAnalysisStateManager stateManager;
    private final VitamateAnalysisJobPublisherPort jobPublisherPort;

    // PENDING 분석을 PROCESSING으로 선점하고 Redis Stream에 작업 메시지를 발행한다.
    @Override
    public void handle(DispatchVitamateAnalysisJobCommand command) {
        validateCommand(command);

        Optional<StartVitamateAnalysisResult> started = stateManager.startProcessing(command.analysisId());

        if (started.isEmpty()) {
            log.warn("Vitamate analysis dispatch skipped. analysisId={}, reason=not_pending_or_already_processing",
                    command.analysisId());
            return;
        }

        StartVitamateAnalysisResult result = started.get();

        try {
            LocalDateTime publishedAt = LocalDateTime.now();
            jobPublisherPort.publish(new VitamateAnalysisJobPublisherPort.AnalysisJob(
                    result.analysisId(),
                    result.attemptId(),
                    INITIAL_RETRY_COUNT,
                    publishedAt
            ));
        } catch (RuntimeException e) {
            log.error("Failed to publish vitamate analysis job. analysisId={}, attemptId={}, reason=queue_publish_failed",
                    result.analysisId(), result.attemptId(), e);

            stateManager.failProcessing(
                    result.analysisId(),
                    result.attemptId(),
                    "분석 작업 큐 발행에 실패했습니다."
            );
        }
    }

    // 큐 발행에 필요한 분석 ID가 유효한지 확인한다.
    private void validateCommand(DispatchVitamateAnalysisJobCommand command) {
        if (command == null || command.analysisId() == null || command.analysisId() <= 0) {
            throw new IllegalArgumentException("analysisId must be positive.");
        }
    }
}
