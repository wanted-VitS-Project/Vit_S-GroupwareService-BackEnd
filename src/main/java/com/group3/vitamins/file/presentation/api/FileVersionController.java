package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.result.FilePreviewResult;
import com.group3.vitamins.file.application.usecase.FileQueryUseCase;
import com.group3.vitamins.file.presentation.api.response.FileDownloadResponse;
import com.group3.vitamins.file.presentation.api.response.FileVersionSingleResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "FileVersion - 파일 버전", description = "버전 단건·다운로드·미리보기 — 스텝 열람 권한을 따른다")
@RestController
@RequestMapping("/api/v1/file-versions")
@RequiredArgsConstructor
public class FileVersionController {

    private final FileQueryUseCase fileQueryUseCase;

    @Operation(summary = "다운로드 URL 발급",
            description = "완료된 버전의 presigned GET URL(5분)을 발급한다. 파일 바이너리를 서버가 반환하지 않고 "
                    + "클라이언트가 저장소에서 직접 받는다. 최신·과거 버전 모두 같은 API 다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "FILE_VERSION_NOT_FOUND — 버전 없음 또는 문서가 휴지통"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "FILE_UPLOAD_NOT_COMPLETED — 업로드 미완료 버전")
    })
    @GetMapping("/{fileVersionId}/download")
    public ApiResponse<FileDownloadResponse> getDownloadUrl(
            @PathVariable Long fileVersionId,
            Authentication authentication
    ) {
        FileDownloadResponse data = FileDownloadResponse.from(
                fileQueryUseCase.getDownloadUrl(
                        fileVersionId, authentication.getName(), RequesterRole.from(authentication)));

        return ApiResponse.success(FileResponseMessage.DOWNLOAD_URL_ISSUED, data);
    }

    @Operation(summary = "버전 단건 조회 (결재용)",
            description = "결재가 고정한 fileVersionId 로 그 버전을 연다. latest=false 면 결재 이후 새 버전이 올라온 것. "
                    + "다운로드·미리보기와 달리 문서가 휴지통에 있어도 반환한다(결재 이력 보존).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(휴지통이어도 반환)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_VERSION_NOT_FOUND")
    })
    @GetMapping("/{fileVersionId}")
    public ApiResponse<FileVersionSingleResponse> getVersion(
            @PathVariable Long fileVersionId,
            Authentication authentication
    ) {
        FileVersionSingleResponse data = FileVersionSingleResponse.from(
                fileQueryUseCase.getVersion(
                        fileVersionId, authentication.getName(), RequesterRole.from(authentication)));

        return ApiResponse.success(FileResponseMessage.VERSION_DETAIL, data);
    }

    @Operation(summary = "미리보기 조회",
            description = "PDF 앞 5페이지만 잘라낸 PDF 바이너리를 반환한다(JSON 아님). presigned 를 주면 전체에 접근되므로 "
                    + "서버가 잘라서 준다. PDF 가 아니면 409. X-Preview-Page-Count / X-Total-Page-Count 헤더로 문구를 만든다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "PDF 반환"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_VERSION_NOT_FOUND — 버전 없음/휴지통"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "FILE_PREVIEW_NOT_SUPPORTED(PDF 아님) · FILE_UPLOAD_NOT_COMPLETED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "FILE_PREVIEW_FAILED — PDF 처리 실패")
    })
    @GetMapping("/{fileVersionId}/preview")
    public ResponseEntity<byte[]> getPreview(
            @PathVariable Long fileVersionId,
            Authentication authentication
    ) {
        FilePreviewResult preview = fileQueryUseCase.getPreview(
                fileVersionId, authentication.getName(), RequesterRole.from(authentication));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"preview.pdf\"")
                .header("X-Preview-Page-Count", String.valueOf(preview.previewPageCount()))
                .header("X-Total-Page-Count", String.valueOf(preview.totalPageCount()))
                .body(preview.content());
    }
}

