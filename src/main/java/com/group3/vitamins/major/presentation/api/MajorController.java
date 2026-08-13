package com.group3.vitamins.major.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.major.application.query.MajorListQuery;
import com.group3.vitamins.major.application.usecase.MajorCommandUseCase;
import com.group3.vitamins.major.application.usecase.MajorQueryUseCase;
import com.group3.vitamins.major.presentation.api.request.MajorCreateRequest;
import com.group3.vitamins.major.presentation.api.request.MajorUpdateRequest;
import com.group3.vitamins.major.presentation.api.response.MajorDeleteResponse;
import com.group3.vitamins.major.presentation.api.response.MajorListResponse;
import com.group3.vitamins.major.presentation.api.response.MajorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Major - 전공 마스터", description = "전공 마스터 CRUD — 사원 학력의 전공 원본 (ADMIN 전용 · HR-V1 MAJ)")
@RestController
@RequestMapping("/api/v1/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorCommandUseCase majorCommandUseCase;
    private final MajorQueryUseCase majorQueryUseCase;

    @Operation(summary = "전공 마스터 목록",
            description = "회사의 전공 목록을 사용 사원 수와 함께 이름 오름차순으로 조회한다. ADMIN 전용. keyword 로 이름 검색.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED")
    })
    @GetMapping
    public ApiResponse<MajorListResponse> list(
            @RequestParam(required = false) String keyword,
            Authentication authentication
    ) {
        return ApiResponse.success(MajorResponseMessage.MAJOR_LIST, MajorListResponse.from(
                majorQueryUseCase.list(new MajorListQuery(keyword, RequesterRole.from(authentication)))));
    }

    @Operation(summary = "전공 생성", description = "새 전공을 만든다. 회사 내 이름 UNIQUE. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "MAJOR_INVALID_REQUEST"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "MAJOR_NAME_DUPLICATED")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MajorResponse> create(
            @RequestBody MajorCreateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.created(MajorResponseMessage.MAJOR_CREATED, MajorResponse.from(
                majorCommandUseCase.create(request.toCommand(RequesterRole.from(authentication)))));
    }

    @Operation(summary = "전공 수정", description = "전공명을 수정한다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "MAJOR_INVALID_REQUEST"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MAJOR_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "MAJOR_NAME_DUPLICATED")
    })
    @PatchMapping("/{majorId}")
    public ApiResponse<MajorResponse> update(
            @PathVariable Long majorId,
            @RequestBody MajorUpdateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(MajorResponseMessage.MAJOR_UPDATED, MajorResponse.from(
                majorCommandUseCase.update(request.toCommand(majorId, RequesterRole.from(authentication)))));
    }

    @Operation(summary = "전공 삭제",
            description = "전공을 삭제한다(hard delete). 참조하는 사원 학력이 있으면 MAJOR_IN_USE 로 막는다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MAJOR_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "MAJOR_IN_USE — 사용 중")
    })
    @DeleteMapping("/{majorId}")
    public ApiResponse<MajorDeleteResponse> delete(
            @PathVariable Long majorId,
            Authentication authentication
    ) {
        majorCommandUseCase.delete(new com.group3.vitamins.major.application.command.DeleteMajorCommand(
                majorId, RequesterRole.from(authentication)));
        return ApiResponse.success(MajorResponseMessage.MAJOR_DELETED, new MajorDeleteResponse(majorId));
    }
}
