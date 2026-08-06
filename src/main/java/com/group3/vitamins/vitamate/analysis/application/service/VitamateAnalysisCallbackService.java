package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.command.HandleVitamateAnalysisCallbackCommand;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisCallbackResult;
import com.group3.vitamins.vitamate.analysis.application.support.VitamateAnalysisStateManager;
import com.group3.vitamins.vitamate.analysis.application.usecase.HandleVitamateAnalysisCallbackUseCase;
import com.group3.vitamins.vitamate.analysis.domain.exception.VitamateErrorCode;
import com.group3.vitamins.vitamate.analysis.domain.model.AnalysisStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VitamateAnalysisCallbackService implements HandleVitamateAnalysisCallbackUseCase {

    private static final String INVALID_CITATION_TARGET_MESSAGE = "분석 근거가 선택 문서 범위를 벗어났습니다.";
    private static final String IGNORED_REASON = "attempt_mismatch_or_already_finished";

    private final VitamateAnalysisStateManager stateManager;
    private final VitamateAnalysisStorePort analysisStore;

    @Override
    @Transactional
    public VitamateAnalysisCallbackResult handle(HandleVitamateAnalysisCallbackCommand command) {
        validateCommand(command);

        if ("FAILED".equals(command.analysisStatus())) {
            boolean failed = stateManager.failProcessing(
                    command.analysisId(), command.attemptId(), command.errorMessage()
            );
            return result(command.analysisId(), failed, "FAILED");
        }

        List<VitamateAnalysisStorePort.NewCitation> citations = toNewCitations(command);

        if (!analysisStore.existsAllCitationTargets(command.analysisId(), citations)) {
            boolean failed = stateManager.failProcessing(
                    command.analysisId(), command.attemptId(), INVALID_CITATION_TARGET_MESSAGE
            );
            return result(command.analysisId(), failed, "FAILED");
        }

        boolean completed = stateManager.completeProcessing(
                command.analysisId(), command.attemptId(), command.result()
        );

        if (!completed) {
            return ignored(command.analysisId(), IGNORED_REASON);
        }

        analysisStore.saveAnalysisCitations(command.analysisId(), citations);
        return new VitamateAnalysisCallbackResult(true, command.analysisId(), "COMPLETED", null);
    }

    // callback 필수값과 상태별 null 규칙을 검증한다.
    private void validateCommand(HandleVitamateAnalysisCallbackCommand command) {
        if (command == null
                || command.analysisId() == null
                || command.analysisId() <= 0
                || command.attemptId() == null
                || command.attemptId().isBlank()
                || command.analysisStatus() == null) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (!AnalysisStatus.COMPLETED.name().equals(command.analysisStatus())
                && !AnalysisStatus.FAILED.name().equals(command.analysisStatus())) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        if (AnalysisStatus.COMPLETED.name().equals(command.analysisStatus())) {
            validateCompletedCallback(command);
            return;
        }

        validateFailedCallback(command);
    }

    // COMPLETED callback에서 필요한 결과와 citation 값을 검증한다.
    private void validateCompletedCallback(HandleVitamateAnalysisCallbackCommand command) {
        if (command.result() == null || command.result().isBlank() || command.citations() == null) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
        if (command.errorMessage() != null && !command.errorMessage().isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }

        Set<Integer> rankOrders = new HashSet<>();
        command.citations().forEach(this::validateCitation);
        command.citations().forEach(citation -> {
            if (!rankOrders.add(citation.rankOrder())) {
                throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
            }
        });
    }

    // FAILED callback에서 필요한 실패 사유와 상태별 null 규칙을 검증한다.
    private void validateFailedCallback(HandleVitamateAnalysisCallbackCommand command) {
        if (command.errorMessage() == null || command.errorMessage().isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
        if (command.result() != null && !command.result().isBlank()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
        if (command.citations() != null && !command.citations().isEmpty()) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // citation이 선택 문서와 청크 검증에 필요한 최소 식별자를 갖는지 확인한다.
    private void validateCitation(HandleVitamateAnalysisCallbackCommand.Citation citation) {
        if (citation == null
                || citation.documentChunkId() == null
                || citation.documentChunkId() <= 0
                || citation.fileVersionId() == null
                || citation.fileVersionId() <= 0
                || citation.rankOrder() == null
                || citation.rankOrder() <= 0
                || (citation.distanceScore() != null && citation.distanceScore().signum() < 0)) {
            throw new ValidationException(VitamateErrorCode.VITAMATE_INVALID_REQUEST);
        }
    }

    // command citation을 저장 포트에서 사용하는 값으로 변환한다.
    private List<VitamateAnalysisStorePort.NewCitation> toNewCitations(HandleVitamateAnalysisCallbackCommand command) {
        return command.citations().stream()
                .map(citation -> new VitamateAnalysisStorePort.NewCitation(
                        citation.documentChunkId(),
                        citation.fileVersionId(),
                        citation.rankOrder(),
                        citation.distanceScore(),
                        citation.excerpt()
                ))
                .toList();
    }

    // 상태 전이 성공 여부를 callback 결과로 변환한다.
    private VitamateAnalysisCallbackResult result(Long analysisId, boolean accepted, String analysisStatus) {
        if (!accepted) {
            return ignored(analysisId, IGNORED_REASON);
        }

        return new VitamateAnalysisCallbackResult(true, analysisId, analysisStatus, null);
    }

    // 늦게 도착한 worker 응답처럼 반영하지 않은 callback 결과를 만든다.
    private VitamateAnalysisCallbackResult ignored(Long analysisId, String reason) {
        String currentStatus = analysisStore.findAnalysisStatus(analysisId).orElse(null);
        return new VitamateAnalysisCallbackResult(false, analysisId, currentStatus, reason);
    }
}
