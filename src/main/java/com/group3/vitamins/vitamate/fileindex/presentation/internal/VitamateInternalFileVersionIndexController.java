package com.group3.vitamins.vitamate.fileindex.presentation.internal;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateChunkEmbeddingsCommand;
import com.group3.vitamins.vitamate.fileindex.application.query.GetVitamateFileIndexSourceQuery;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateChunkEmbeddingsResult;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;
import com.group3.vitamins.vitamate.fileindex.application.usecase.GetVitamateFileIndexSourceUseCase;
import com.group3.vitamins.vitamate.fileindex.application.usecase.SaveVitamateChunkEmbeddingsUseCase;
import com.group3.vitamins.vitamate.fileindex.application.usecase.SaveVitamateDocumentChunksUseCase;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.request.SaveVitamateChunkEmbeddingsRequest;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.request.SaveVitamateDocumentChunksRequest;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response.SaveVitamateChunkEmbeddingsResponse;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response.SaveVitamateDocumentChunksResponse;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response.VitamateFileIndexSourceResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Python worker가 파일 인덱싱 소스 조회와 청크 저장에 사용하는 내부 API 컨트롤러
@Hidden
@Tag(name = "Vitamate Internal", description = "비타메이트 Python worker 내부 API")
@RestController
@RequestMapping("/internal/v1/vitamate/file-versions")
@RequiredArgsConstructor
public class VitamateInternalFileVersionIndexController {

    private final GetVitamateFileIndexSourceUseCase getIndexSourceUseCase;
    private final SaveVitamateDocumentChunksUseCase saveDocumentChunksUseCase;
    private final SaveVitamateChunkEmbeddingsUseCase saveChunkEmbeddingsUseCase;

    // Python worker가 파일 다운로드 URL과 메타데이터를 조회합니다.
    @Operation(summary = "파일 인덱싱 소스 조회", description = "Python worker가 파일 텍스트 추출에 사용할 파일 버전 메타데이터와 다운로드 URL을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인덱싱 소스 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VITAMATE_INVALID_REQUEST — 잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "VITAMATE_WORKER_UNAUTHORIZED — worker token 누락 또는 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON_FORBIDDEN — worker 전용 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VITAMATE_FILE_VERSION_NOT_FOUND — 파일 버전 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류")
    })
    @GetMapping("/{fileVersionId}/index-source")
    public ResponseEntity<VitamateFileIndexSourceResponse> getIndexSource(
            @Parameter(description = "파일 버전 ID", example = "101")
            @PathVariable Long fileVersionId
    ) {
        VitamateFileIndexSourceResult result = getIndexSourceUseCase.handle(
                new GetVitamateFileIndexSourceQuery(fileVersionId)
        );

        return ResponseEntity.ok(VitamateFileIndexSourceResponse.from(result));
    }

    // Python worker가 추출한 청크를 document_chunk에 저장합니다.
    @Operation(summary = "문서 청크 저장", description = "Python worker가 추출한 문서 청크를 파일 버전 기준으로 저장한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "청크 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VITAMATE_INVALID_REQUEST — 요청 형식 또는 청크 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "VITAMATE_WORKER_UNAUTHORIZED — worker token 누락 또는 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON_FORBIDDEN — worker 전용 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VITAMATE_FILE_VERSION_NOT_FOUND — 파일 버전 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류")
    })
    @PostMapping("/{fileVersionId}/chunks")
    public ResponseEntity<SaveVitamateDocumentChunksResponse> saveChunks(
            @Parameter(description = "파일 버전 ID", example = "101")
            @PathVariable Long fileVersionId,
            @RequestBody(required = false) SaveVitamateDocumentChunksRequest request
    ) {
        SaveVitamateDocumentChunksCommand command = request == null
                ? new SaveVitamateDocumentChunksCommand(fileVersionId, null)
                : request.toCommand(fileVersionId);

        SaveVitamateDocumentChunksResult result = saveDocumentChunksUseCase.handle(command);

        return ResponseEntity.ok(SaveVitamateDocumentChunksResponse.from(result));
    }

    // Python worker가 ChromaDB 저장 결과를 document_chunk에 반영합니다.
    @Operation(summary = "문서 chunk 임베딩 결과 저장", description = "Python worker가 ChromaDB에 저장한 vector ID와 임베딩 모델명을 document_chunk에 반영한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "임베딩 결과 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VITAMATE_INVALID_REQUEST - 요청 형식 또는 chunk 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "VITAMATE_WORKER_UNAUTHORIZED - worker token 누락 또는 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "COMMON_FORBIDDEN - worker 전용 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "VITAMATE_FILE_VERSION_NOT_FOUND - 파일 버전 또는 chunk 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR - 서버 내부 오류")
    })
    @PostMapping("/{fileVersionId}/chunks/embeddings")
    public ResponseEntity<SaveVitamateChunkEmbeddingsResponse> saveChunkEmbeddings(
            @Parameter(description = "파일 버전 ID", example = "101")
            @PathVariable Long fileVersionId,
            @RequestBody(required = false) SaveVitamateChunkEmbeddingsRequest request
    ) {
        SaveVitamateChunkEmbeddingsCommand command = request == null
                ? new SaveVitamateChunkEmbeddingsCommand(fileVersionId, null, null, null)
                : request.toCommand(fileVersionId);

        SaveVitamateChunkEmbeddingsResult result = saveChunkEmbeddingsUseCase.handle(command);

        return ResponseEntity.ok(SaveVitamateChunkEmbeddingsResponse.from(result));
    }
}
