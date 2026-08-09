package com.group3.vitamins.file.presentation.api;

import com.group3.vitamins.file.application.command.RestoreFileCommand;
import com.group3.vitamins.file.application.command.TrashFileCommand;
import com.group3.vitamins.file.application.usecase.FileCommandUseCase;
import com.group3.vitamins.file.application.usecase.FileQueryUseCase;
import com.group3.vitamins.file.presentation.api.request.FilePermanentDeleteRequest;
import com.group3.vitamins.file.presentation.api.request.FileRenameRequest;
import com.group3.vitamins.file.presentation.api.response.FilePermanentDeleteResponse;
import com.group3.vitamins.file.presentation.api.response.FileRenameResponse;
import com.group3.vitamins.file.presentation.api.response.FileRestoreResponse;
import com.group3.vitamins.file.presentation.api.response.FileTrashResponse;
import com.group3.vitamins.file.presentation.api.response.VersionHistoryResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "File - 문서", description = "문서 단위 조회·수정 — 스텝 권한을 따른다")
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileQueryUseCase fileQueryUseCase;
    private final FileCommandUseCase fileCommandUseCase;

    @Operation(summary = "버전 이력 조회",
            description = "문서의 완료된 버전들을 차수 내림차순으로 돌려준다. 업로드에 실패했거나 미완료인 버전은 제외된다. "
                    + "업로더 정보는 각 버전 시점의 스냅샷이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_ACCESS_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND — 문서 없음 또는 휴지통")
    })
    @GetMapping("/{fileId}/versions")
    public ApiResponse<VersionHistoryResponse> getVersionHistory(
            @PathVariable Long fileId,
            Authentication authentication
    ) {
        VersionHistoryResponse data = VersionHistoryResponse.from(
                fileQueryUseCase.getVersionHistory(
                        fileId, authentication.getName(), RequesterRole.from(authentication)));

        return ApiResponse.success(FileResponseMessage.VERSION_HISTORY, data);
    }

    @Operation(summary = "문서명 수정",
            description = "문서의 표시명만 바꾼다. 각 버전에 저장된 원본 파일명은 그대로다. 스텝 EDITOR 권한이 필요하다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE_INVALID_REQUEST — 이름이 비었거나 255자 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_EDIT_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND — 문서 없음 또는 이미 휴지통")
    })
    @PatchMapping("/{fileId}")
    public ApiResponse<FileRenameResponse> rename(
            @PathVariable Long fileId,
            @RequestBody FileRenameRequest request,
            Authentication authentication
    ) {
        FileRenameResponse data = FileRenameResponse.from(
                fileCommandUseCase.rename(
                        request.toCommand(fileId, authentication.getName(), RequesterRole.from(authentication))));

        return ApiResponse.success(FileResponseMessage.FILE_RENAMED, data);
    }

    @Operation(summary = "휴지통으로 이동",
            description = "문서를 휴지통으로 옮긴다(소프트 삭제). 저장소 객체는 유지되며 복구할 수 있다. "
                    + "진행 중인 결재의 대상 문서는 삭제할 수 없다. 스텝 EDITOR 권한이 필요하다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이동 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE_ALREADY_DELETED — 이미 휴지통"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_EDIT_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND — 문서 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "FILE_APPROVAL_IN_PROGRESS — 진행 중 결재의 대상")
    })
    @DeleteMapping("/{fileId}")
    public ApiResponse<FileTrashResponse> moveToTrash(
            @PathVariable Long fileId,
            Authentication authentication
    ) {
        FileTrashResponse data = FileTrashResponse.from(
                fileCommandUseCase.moveToTrash(
                        new TrashFileCommand(
                                fileId, authentication.getName(), RequesterRole.from(authentication))));

        return ApiResponse.success(FileResponseMessage.FILE_TRASHED, data);
    }

    @Operation(summary = "휴지통에서 복구",
            description = "휴지통에 있는 문서를 복구한다. 원래 블록으로 돌아가며, 원래 블록이 삭제된 경우에도 복구되고 "
                    + "이때는 blockId=null·blockDeleted=true 로 프로젝트 문서함에 복구된다. 스텝 EDITOR 권한이 필요하다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "복구 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE_NOT_DELETED — 휴지통에 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_EDIT_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND — 문서 없음")
    })
    @PostMapping("/{fileId}/restore")
    public ApiResponse<FileRestoreResponse> restore(
            @PathVariable Long fileId,
            Authentication authentication
    ) {
        FileRestoreResponse data = FileRestoreResponse.from(
                fileCommandUseCase.restore(
                        new RestoreFileCommand(
                                fileId, authentication.getName(), RequesterRole.from(authentication))));

        return ApiResponse.success(FileResponseMessage.FILE_RESTORED, data);
    }

    @Operation(summary = "영구 삭제",
            description = "휴지통에 있는 문서를 되돌릴 수 없이 지운다. DB(문서·전 버전)를 지운 뒤 저장소(S3) 객체를 "
                    + "커밋 후 best-effort 로 제거한다. storageDeletedCount 는 삭제를 요청한 객체 수이며(실제 삭제 완료 수가 "
                    + "아니다 — 저장소 삭제는 커밋 후 비동기이고 실패 키는 후속 정리 대상이다). 확인 문자로 정확히 \"영구 삭제\" 를 "
                    + "보내야 하며, 완료된 결재까지 포함해 이 문서의 버전을 참조하는 결재가 있으면 삭제할 수 없다. 스텝 EDITOR 권한이 필요하다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "영구 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "FILE_NOT_DELETED — 휴지통에 없음 / FILE_CONFIRM_TEXT_MISMATCH — 확인 문자 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "FILE_EDIT_PERMISSION_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FILE_NOT_FOUND — 문서 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "FILE_APPROVAL_REFERENCED — 결재가 이 문서의 버전을 참조")
    })
    @PostMapping("/{fileId}/permanent-deletion")
    public ApiResponse<FilePermanentDeleteResponse> permanentDelete(
            @PathVariable Long fileId,
            @RequestBody FilePermanentDeleteRequest request,
            Authentication authentication
    ) {
        FilePermanentDeleteResponse data = FilePermanentDeleteResponse.from(
                fileCommandUseCase.permanentDelete(
                        request.toCommand(fileId, authentication.getName(), RequesterRole.from(authentication))));

        return ApiResponse.success(FileResponseMessage.FILE_PERMANENTLY_DELETED, data);
    }
}
