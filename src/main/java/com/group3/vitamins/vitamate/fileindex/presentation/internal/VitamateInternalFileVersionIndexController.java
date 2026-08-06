package com.group3.vitamins.vitamate.fileindex.presentation.internal;

import com.group3.vitamins.vitamate.fileindex.application.command.SaveVitamateDocumentChunksCommand;
import com.group3.vitamins.vitamate.fileindex.application.query.GetVitamateFileIndexSourceQuery;
import com.group3.vitamins.vitamate.fileindex.application.result.SaveVitamateDocumentChunksResult;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;
import com.group3.vitamins.vitamate.fileindex.application.usecase.GetVitamateFileIndexSourceUseCase;
import com.group3.vitamins.vitamate.fileindex.application.usecase.SaveVitamateDocumentChunksUseCase;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.request.SaveVitamateDocumentChunksRequest;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response.SaveVitamateDocumentChunksResponse;
import com.group3.vitamins.vitamate.fileindex.presentation.internal.dto.response.VitamateFileIndexSourceResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Python worker가 파일 인덱싱 소스 조회와 청크 저장에 사용하는 내부 API 컨트롤러
@Hidden
@RestController
@RequestMapping("/internal/v1/vitamate/file-versions")
@RequiredArgsConstructor
public class VitamateInternalFileVersionIndexController {

    private final GetVitamateFileIndexSourceUseCase getIndexSourceUseCase;
    private final SaveVitamateDocumentChunksUseCase saveDocumentChunksUseCase;

    // Python worker가 파일 다운로드 URL과 메타데이터를 조회합니다.
    @GetMapping("/{fileVersionId}/index-source")
    public ResponseEntity<VitamateFileIndexSourceResponse> getIndexSource(
            @PathVariable Long fileVersionId
    ) {
        VitamateFileIndexSourceResult result = getIndexSourceUseCase.handle(
                new GetVitamateFileIndexSourceQuery(fileVersionId)
        );

        return ResponseEntity.ok(VitamateFileIndexSourceResponse.from(result));
    }

    // Python worker가 추출한 청크를 document_chunk에 저장합니다.
    @PostMapping("/{fileVersionId}/chunks")
    public ResponseEntity<SaveVitamateDocumentChunksResponse> saveChunks(
            @PathVariable Long fileVersionId,
            @RequestBody(required = false) SaveVitamateDocumentChunksRequest request
    ) {
        SaveVitamateDocumentChunksCommand command = request == null
                ? new SaveVitamateDocumentChunksCommand(fileVersionId, null)
                : request.toCommand(fileVersionId);

        SaveVitamateDocumentChunksResult result = saveDocumentChunksUseCase.handle(command);

        return ResponseEntity.ok(SaveVitamateDocumentChunksResponse.from(result));
    }
}