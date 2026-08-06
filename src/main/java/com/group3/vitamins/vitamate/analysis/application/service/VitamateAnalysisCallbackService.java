package com.group3.vitamins.vitamate.analysis.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.vitamate.analysis.application.command.HandleVitamateAnalysisCallbackCommand;
import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.analysis.application.result.VitamateAnalysisCallbackResult;
import com.group3.vitamins.vitamate.analysis.application.support.VitamateAnalysisStateManager;
import com.group3.vitamins.vitamate.analysis.application.usecase.HandleVitamateAnalysisCallbackUseCase;
import com.group3.vitamins.vitamate.analysis.domain.model.AnalysisStatus;
import com.group3.vitamins.vitamate.domain.exception.VitamateErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VitamateAnalysisCallbackService implements HandleVitamateAnalysisCallbackUseCase {

    private static final String INVALID_CITATION_TARGET_MESSAGE = "Citation target is outside selected documents.";
    private static final String IGNORED_REASON = "attempt_mismatch_or_already_finished";

    private final VitamateAnalysisStateManager stateManager;
    private final VitamateAnalysisStorePort analysisStore;

    @Override
    @Transactional
    public VitamateAnalysisCallbackResult handle(HandleVitamateAnalysisCallbackCommand command) {
        validateCommand(command);

        if (AnalysisStatus.FAILED.name().equals(command.analysisStatus())) {
            boolean failed = stateManager.failProcessing(
                    command.analysisId(), command.attemptId(), command.errorMessage()
            );
            return result(command.analysisId(), failed, AnalysisStatus.FAILED.name());
        }

        List<VitamateAnalysisStorePort.NewCitation> citations = toNewCitations(command);

        if (!analysisStore.existsAllCitationTargets(command.analysisId(), citations)) {
            boolean failed = stateManager.failProcessing(
                    command.analysisId(), command.attemptId(), INVALID_CITATION_TARGET_MESSAGE
            );
            return result(command.analysisId(), failed, AnalysisStatus.FAILED.name());
        }

        boolean completed = stateManager.completeProcessing(
                command.analysisId(), command.attemptId(), command.result()
        );

        if (!completed) {
            return ignored(command.analysisId(), IGNORED_REASON);
        }

        analysisStore.saveAnalysisCitations(command.analysisId(), citations);
        return new VitamateAnalysisCallbackResult(true, command.analysisId(), AnalysisStatus.COMPLETED.name(), null);
    }

    // Validate callback identifiers, status, and status-specific null rules.
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

    // Validate the result and citation payload required for a completed callback.
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

    // Validate the error payload required for a failed callback.
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

    // Check the minimum citation identifiers needed to verify selected document boundaries.
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

    // Convert callback citation values into the store port input model.
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

    // Convert conditional state transition results into a callback response.
    private VitamateAnalysisCallbackResult result(Long analysisId, boolean accepted, String analysisStatus) {
        if (!accepted) {
            return ignored(analysisId, IGNORED_REASON);
        }

        return new VitamateAnalysisCallbackResult(true, analysisId, analysisStatus, null);
    }

    // Return the current state when a stale worker response is ignored.
    private VitamateAnalysisCallbackResult ignored(Long analysisId, String reason) {
        String currentStatus = analysisStore.findAnalysisStatus(analysisId).orElse(null);
        return new VitamateAnalysisCallbackResult(false, analysisId, currentStatus, reason);
    }
}
