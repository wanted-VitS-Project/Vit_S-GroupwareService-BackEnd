package com.group3.vitamins.image.presentation;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.image.application.query.GetImageTrashQuery;
import com.group3.vitamins.image.application.query.GetProjectImagesQuery;
import com.group3.vitamins.image.application.usecase.ImageQueryUseCase;
import com.group3.vitamins.image.application.usecase.ImageQueryUseCase.ImageTrashView;
import com.group3.vitamins.image.application.usecase.ImageQueryUseCase.ProjectImagesView;
import com.group3.vitamins.image.presentation.api.response.ImageTrashResponse;
import com.group3.vitamins.image.presentation.api.response.ProjectImagesResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 프로젝트 단위 이미지 API. 블록 하나(imgBlockId)가 아니라 프로젝트 전체(여러 스텝·블록에 걸침)를
 * 대상으로 하는 조회라 {@code ProjectAccessUseCase}로 프로젝트 접근 권한만 확인한다 — 스텝 하나를
 * 특정할 수 없어 스텝 단위 권한 검사가 불가능하기 때문.
 */
@Tag(name = "Image", description = "이미지 블록 API")
@RestController
@RequestMapping("/api/v1/projects/{projectId}/images")
@RequiredArgsConstructor
public class ImageProjectController {

    private final ImageQueryUseCase imageQueryUseCase;

    @Operation(summary = "프로젝트 이미지 모아보기", description = "프로젝트에 속한 활성 이미지를 생성일 최신순으로 페이지네이션 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로젝트 이미지 모아보기 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "IMG-012 — 페이지 조회 조건이 올바르지 않습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_ACCESS_DENIED — 프로젝트에 접근할 권한이 없습니다. / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트를 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectImagesResponse>> getProjectImages(
            @Parameter(description = "이미지를 모아볼 프로젝트 ID", example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "0-base 페이지 번호. 생략하면 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 개수(최대 100). 생략하면 20", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        ProjectImagesView view = imageQueryUseCase.getProjectImages(new GetProjectImagesQuery(
                authentication.getName(), projectId, RequesterRole.from(authentication), page, size));

        return ResponseEntity.ok(ApiResponse.success("프로젝트 이미지 모아보기 조회 성공", ProjectImagesResponse.from(view)));
    }

    @Operation(summary = "이미지 휴지통 조회", description = "프로젝트에 속한 삭제된 이미지를 삭제일 최신순으로 페이지네이션 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 휴지통 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "IMG-012 — 페이지 조회 조건이 올바르지 않습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "PROJECT_ACCESS_DENIED — 프로젝트에 접근할 권한이 없습니다. / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "PROJECT_NOT_FOUND — 프로젝트를 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<ImageTrashResponse>> getTrash(
            @Parameter(description = "삭제된 이미지를 조회할 프로젝트 ID", example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "0-base 페이지 번호. 생략하면 0", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 개수(최대 100). 생략하면 20", example = "20")
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        ImageTrashView view = imageQueryUseCase.getTrash(new GetImageTrashQuery(
                authentication.getName(), projectId, RequesterRole.from(authentication), page, size));

        return ResponseEntity.ok(ApiResponse.success("이미지 휴지통 조회 성공", ImageTrashResponse.from(view)));
    }
}
