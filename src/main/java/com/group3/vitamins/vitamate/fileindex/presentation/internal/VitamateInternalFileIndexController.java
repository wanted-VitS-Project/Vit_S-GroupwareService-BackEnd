package com.group3.vitamins.vitamate.fileindex.presentation.internal;

import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexCallbackResult;
import com.group3.vitamins.vitamate.fileindex.application.usecase.HandleVitamateFileIndexCallbackUseCase;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.request.VitamateFileIndexCallbackRequest;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response.VitamateFileIndexCallbackResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Python worker가 호출하는 비타메이트 파일 인덱싱 내부 API 컨트롤러
@Hidden
@Tag(name = "Vitamate Internal", description = "비타메이트 Python worker 내부 API")
@RestController
@RequestMapping("/internal/v1/vitamate/file-indexes")
@RequiredArgsConstructor
public class VitamateInternalFileIndexController {

    private final HandleVitamateFileIndexCallbackUseCase callbackUseCase;

    // Python worker가 파일 버전 인덱싱 상태를 Spring Boot에 전달한다.
    @Operation(summary = "파일 인덱싱 상태 callback", description = "Python worker가 파일 버전 인덱싱 처리 상태를 전달한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "callback 수신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VITAMATE_INVALID_REQUEST — 잘못된 callback 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "VITAMATE_WORKER_UNAUTHORIZED — worker token 누락 또는 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON_FORBIDDEN — worker 전용 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VITAMATE_FILE_VERSION_NOT_FOUND — 파일 버전 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류")
    })
    @PostMapping("/{fileVersionId}/callback")
    public ResponseEntity<VitamateFileIndexCallbackResponse> handleCallback(
            @Parameter(description = "파일 버전 ID", example = "101")
            @PathVariable Long fileVersionId,
            @RequestBody VitamateFileIndexCallbackRequest request
    ) {
        VitamateFileIndexCallbackResult result = callbackUseCase.handle(request.toCommand(fileVersionId));
        return ResponseEntity.ok(VitamateFileIndexCallbackResponse.from(result));
    }
}