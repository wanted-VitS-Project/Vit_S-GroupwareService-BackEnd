package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.usecase.FileUploadUseCase;
import com.group3.vitamins.file.presentation.api.request.FileUploadCompleteRequest;
import com.group3.vitamins.file.presentation.api.request.FileUploadStartRequest;
import com.group3.vitamins.file.presentation.api.response.FileUploadStartResponse;
import com.group3.vitamins.file.presentation.api.response.FileVersionDetailResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "File - 파일", description = "파일 업로드 · 버전 · 조회 — 권한은 스텝 권한을 따른다")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadUseCase fileUploadUseCase;

    @Operation(summary = "파일 업로드 시작",
            description = "문서/버전 레코드를 UPLOADING 으로 만들고 presigned PUT URL(10분)을 발급한다. "
                    + "fileId 를 주면 그 문서의 새 버전, 없으면 새 문서(v1)다. "
                    + "파일 자체는 응답의 uploadUrl 로 클라이언트가 저장소에 직접 PUT 한 뒤 완료 통보를 호출한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "발급 성공 — 버전이 UPLOADING 으로 생성됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "FILE_INVALID_REQUEST · FILE_SIZE_EXCEEDED · FILE_EXTENSION_BLOCKED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "FILE_EDIT_PERMISSION_REQUIRED — 스텝 편집 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "FILE_BLOCK_NOT_FOUND(블록 없음/삭제) · FILE_NOT_FOUND(fileId 문서 없음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "FILE_NAME_DUPLICATED — 동명 문서 존재(allowDuplicateName=true 로 재요청)")
    })
    @PostMapping("/uploads")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FileUploadStartResponse> startUpload(
            @RequestBody FileUploadStartRequest request,
            Authentication authentication
    ) {
        FileUploadStartResponse data = FileUploadStartResponse.from(
                fileUploadUseCase.startUpload(
                        request.toCommand(authentication.getName(), RequesterRole.from(authentication))));

        return ApiResponse.created(FileResponseMessage.UPLOAD_STARTED, data);
    }

    @Operation(summary = "파일 업로드 완료 통보",
            description = "클라이언트가 저장소에 PUT 을 마친 뒤 호출한다. 서버가 저장소에 HEAD 로 존재·크기를 검증하고 "
                    + "PDF 면 총 페이지 수를 추출한 뒤 버전을 COMPLETED 로 확정한다. 업로더 정보가 이 시점 스냅샷으로 남는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "FILE_ALREADY_COMPLETED — 이미 완료된 버전"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_EDIT_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "FILE_VERSION_NOT_FOUND · FILE_BLOCK_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "FILE_OBJECT_NOT_FOUND(저장소에 객체 없음→FAILED) · FILE_SIZE_MISMATCH")
    })
    @PostMapping("/uploads/{fileVersionId}/complete")
    public ApiResponse<FileVersionDetailResponse> completeUpload(
            @PathVariable Long fileVersionId,
            @RequestBody(required = false) FileUploadCompleteRequest request,
            Authentication authentication
    ) {
        FileUploadCompleteRequest body = request == null ? new FileUploadCompleteRequest(null) : request;
        FileVersionDetailResponse data = FileVersionDetailResponse.from(
                fileUploadUseCase.completeUpload(
                        body.toCommand(fileVersionId, authentication.getName(),
                                RequesterRole.from(authentication))));

        return ApiResponse.success(FileResponseMessage.UPLOAD_COMPLETED, data);
    }
}
