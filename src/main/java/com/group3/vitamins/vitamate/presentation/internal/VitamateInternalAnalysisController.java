package com.group3.vitamins.vitamate.presentation.internal;

import com.group3.vitamins.vitamate.application.query.GetVitamateAnalysisJobQuery;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisCallbackResult;
import com.group3.vitamins.vitamate.application.result.VitamateAnalysisJobDetailResult;
import com.group3.vitamins.vitamate.application.usecase.GetVitamateAnalysisJobUseCase;
import com.group3.vitamins.vitamate.application.usecase.HandleVitamateAnalysisCallbackUseCase;
import com.group3.vitamins.vitamate.presentation.internal.dto.request.VitamateAnalysisCallbackRequest;
import com.group3.vitamins.vitamate.presentation.internal.dto.response.VitamateAnalysisCallbackResponse;
import com.group3.vitamins.vitamate.presentation.internal.dto.response.VitamateAnalysisJobResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Python worker가 호출하는 비타메이트 내부 API 컨트롤러
@Hidden
@RestController
@RequestMapping("/internal/v1/vitamate/analyses")
@RequiredArgsConstructor
public class VitamateInternalAnalysisController {

    private final GetVitamateAnalysisJobUseCase getAnalysisJobUseCase;
    private final HandleVitamateAnalysisCallbackUseCase callbackUseCase;

    // 분석 ID와 attemptId로 현재 worker가 처리할 작업 입력을 조회한다.
    @GetMapping("/{analysisId}/jobs/{attemptId}")
    public ResponseEntity<VitamateAnalysisJobResponse> getAnalysisJob(
            @PathVariable Long analysisId,
            @PathVariable String attemptId
    ) {
        VitamateAnalysisJobDetailResult result = getAnalysisJobUseCase.handle(
                new GetVitamateAnalysisJobQuery(analysisId, attemptId)
        );

        return ResponseEntity.ok(VitamateAnalysisJobResponse.from(result));
    }

    // Python worker가 처리한 분석 결과를 받아 상태와 citation을 저장한다.
    @PostMapping("/{analysisId}/callback")
    public ResponseEntity<VitamateAnalysisCallbackResponse> handleCallback(
            @PathVariable Long analysisId,
            @RequestBody VitamateAnalysisCallbackRequest request
    ) {
        VitamateAnalysisCallbackResult result = callbackUseCase.handle(request.toCommand(analysisId));
        return ResponseEntity.ok(VitamateAnalysisCallbackResponse.from(result));
    }
}
