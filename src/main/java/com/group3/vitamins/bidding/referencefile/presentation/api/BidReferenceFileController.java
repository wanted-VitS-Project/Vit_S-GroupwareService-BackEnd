package com.group3.vitamins.bidding.referencefile.presentation.api;

import com.group3.vitamins.bidding.referencefile.application.command.CompleteReferenceFileUploadCommand;
import com.group3.vitamins.bidding.referencefile.application.command.DeleteReferenceFileCommand;
import com.group3.vitamins.bidding.referencefile.application.query.GetReferenceFileListQuery;
import com.group3.vitamins.bidding.referencefile.application.result.CompleteReferenceFileUploadResult;
import com.group3.vitamins.bidding.referencefile.application.result.ReferenceFileListResult;
import com.group3.vitamins.bidding.referencefile.application.result.StartReferenceFileUploadResult;
import com.group3.vitamins.bidding.referencefile.application.usecase.CompleteReferenceFileUploadUseCase;
import com.group3.vitamins.bidding.referencefile.application.usecase.DeleteReferenceFileUseCase;
import com.group3.vitamins.bidding.referencefile.application.usecase.GetReferenceFileListUseCase;
import com.group3.vitamins.bidding.referencefile.application.usecase.StartReferenceFileUploadUseCase;
import com.group3.vitamins.bidding.referencefile.presentation.api.request.StartReferenceFileUploadRequest;
import com.group3.vitamins.bidding.referencefile.presentation.api.response.CompleteReferenceFileUploadResponse;
import com.group3.vitamins.bidding.referencefile.presentation.api.response.ReferenceFileListResponse;
import com.group3.vitamins.bidding.referencefile.presentation.api.response.StartReferenceFileUploadResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(
        name = "Bidding - 입찰 기준자료",
        description = "입찰 문서 검토에 반복 사용하는 회사별 기준자료 파일함을 관리합니다."
)
@RestController
@RequestMapping("/api/v1/bidding/reference-files")
@RequiredArgsConstructor
public class BidReferenceFileController {

    private final StartReferenceFileUploadUseCase startUploadUseCase;
    private final CompleteReferenceFileUploadUseCase completeUploadUseCase;
    private final DeleteReferenceFileUseCase deleteUseCase;
    private final GetReferenceFileListUseCase getListUseCase;

    @Operation(
            summary = "입찰 기준자료 업로드 시작",
            description = "presigned 업로드 URL을 발급합니다. 클라이언트는 이 URL로 바이너리를 직접 PUT한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "업로드 시작 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "BIDDING_INVALID_REFERENCE_FILE_REQUEST"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            )
    })
    @PostMapping("/uploads")
    public ResponseEntity<ApiResponse<StartReferenceFileUploadResponse>> startUpload(
            @Valid @RequestBody StartReferenceFileUploadRequest request,
            Authentication authentication
    ) {
        StartReferenceFileUploadResult result = startUploadUseCase.start(
                request.toCommand(authentication.getName(), RequesterRole.from(authentication))
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED.value(),
                        "입찰 기준자료 업로드가 시작됐습니다.",
                        StartReferenceFileUploadResponse.from(result)
                ));
    }

    @Operation(
            summary = "입찰 기준자료 업로드 완료",
            description = "저장소 객체의 존재와 크기를 확인한 뒤 업로드를 완료 처리하고 인덱싱을 요청합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "업로드 완료 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "BIDDING_REFERENCE_FILE_NOT_FOUND"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "BIDDING_REFERENCE_FILE_OBJECT_NOT_FOUND 또는 BIDDING_REFERENCE_FILE_SIZE_MISMATCH"
            )
    })
    @PostMapping("/uploads/{referenceFileId}/complete")
    public ResponseEntity<ApiResponse<CompleteReferenceFileUploadResponse>> completeUpload(
            @Parameter(description = "기준자료 ID")
            @PathVariable Long referenceFileId,
            Authentication authentication
    ) {
        CompleteReferenceFileUploadResult result = completeUploadUseCase.complete(
                new CompleteReferenceFileUploadCommand(
                        referenceFileId, authentication.getName(), RequesterRole.from(authentication)
                )
        );

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(),
                "입찰 기준자료 업로드가 완료됐습니다.",
                CompleteReferenceFileUploadResponse.from(result)
        ));
    }

    @Operation(
            summary = "입찰 기준자료 파일함 조회",
            description = "현재 회사가 검토에 반복 사용할 기준자료를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ReferenceFileListResponse>> getList(
            Authentication authentication
    ) {
        ReferenceFileListResult result = getListUseCase.get(
                new GetReferenceFileListQuery(
                        authentication.getName(), RequesterRole.from(authentication)
                )
        );

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK.value(),
                "입찰 기준자료 파일함 조회 성공",
                ReferenceFileListResponse.from(result)
        ));
    }

    @Operation(
            summary = "입찰 기준자료 삭제",
            description = "현재 회사의 기준자료를 삭제합니다. 처리 중인 검토가 사용 중이면 거절합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "삭제 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "AUTH_UNAUTHENTICATED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "BIDDING_REFERENCE_FILE_NOT_FOUND"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "BIDDING_REFERENCE_FILE_IN_USE"
            )
    })
    @DeleteMapping("/{referenceFileId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @Parameter(description = "기준자료 ID")
            @PathVariable Long referenceFileId,
            Authentication authentication
    ) {
        deleteUseCase.delete(new DeleteReferenceFileCommand(
                referenceFileId, authentication.getName(), RequesterRole.from(authentication)
        ));

        return ResponseEntity.ok(ApiResponse.success("입찰 기준자료가 삭제됐습니다."));
    }
}