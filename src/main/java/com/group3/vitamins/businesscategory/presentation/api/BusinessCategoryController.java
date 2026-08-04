package com.group3.vitamins.businesscategory.presentation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.businesscategory.application.command.CreateBusinessCategoryCommand;
import com.group3.vitamins.businesscategory.application.command.UpdateBusinessCategoryCommand;
import com.group3.vitamins.businesscategory.application.query.BusinessCategoryListQuery;
import com.group3.vitamins.businesscategory.application.result.BusinessCategoryResult;
import com.group3.vitamins.businesscategory.application.usecase.BusinessCategoryCommandUseCase;
import com.group3.vitamins.businesscategory.application.usecase.BusinessCategoryQueryUseCase;
import com.group3.vitamins.businesscategory.presentation.api.request.BusinessCategoryCreateRequest;
import com.group3.vitamins.businesscategory.presentation.api.request.BusinessCategoryUpdateRequest;
import com.group3.vitamins.businesscategory.presentation.api.response.BusinessCategoryDetailResponse;
import com.group3.vitamins.businesscategory.presentation.api.response.BusinessCategoryListResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "BusinessCategory - 사업 카테고리",
        description = "사업 카테고리 조회 / 생성 / 수정 / 삭제 (담당: 동훈)")
@RestController
@RequestMapping("/api/v1/business-categories")
@RequiredArgsConstructor
public class BusinessCategoryController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final BusinessCategoryQueryUseCase businessCategoryQueryUseCase;
    private final BusinessCategoryCommandUseCase businessCategoryCommandUseCase;

    @Operation(summary = "사업 카테고리 목록 조회",
            description = "선택용 목록을 이름 오름차순으로 조회한다. 삭제분은 기본 제외이며 페이징·정렬 파라미터를 받지 않는다. "
                    + "각 항목의 deletable 은 연결된 프로젝트가 없으면 true 다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공 (0건이면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "BUSINESS_CATEGORY_ADMIN_ONLY — ADMIN 이 아닌데 includeDeleted=true 를 요청 (MASTER 포함)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<BusinessCategoryListResponse>> listCategories(
            @Parameter(description = "이름·업무코드 부분 일치 검색")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "삭제된 카테고리 포함. ADMIN 만 true 를 쓸 수 있다")
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,

            Authentication authentication
    ) {
        BusinessCategoryListResponse data = BusinessCategoryListResponse.from(
                businessCategoryQueryUseCase.listCategories(
                        new BusinessCategoryListQuery(keyword, includeDeleted, currentRole(authentication))));

        return ResponseEntity.ok(
                ApiResponse.success(BusinessCategoryResponseMessage.SUCCESS, data));
    }

    @Operation(summary = "사업 카테고리 생성",
            description = "이름·업무코드·설명으로 카테고리를 생성한다. name 은 필수·중복 불가, code 는 선택이며 입력한 경우에만 중복 검사한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "BUSINESS_CATEGORY_NAME_REQUIRED / _FIELD_TOO_LONG / _CODE_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "BUSINESS_CATEGORY_ADMIN_ONLY — ADMIN 이 아님 (MASTER 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "BUSINESS_CATEGORY_NAME_DUPLICATED / _CODE_DUPLICATED")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BusinessCategoryDetailResponse>> createCategory(
            @RequestBody BusinessCategoryCreateRequest request,
            Authentication authentication
    ) {
        BusinessCategoryResult result = businessCategoryCommandUseCase.createCategory(
                request.toCommand(currentRole(authentication)));

        return ResponseEntity.status(201).body(
                ApiResponse.created(BusinessCategoryResponseMessage.SUCCESS,
                        BusinessCategoryDetailResponse.from(result)));
    }

    @Operation(summary = "사업 카테고리 수정",
            description = "보낸 필드만 바꾼다. name·code·description 셋 다 없으면 400. code 에 null 을 보내면 업무코드를 지운다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(implementation = BusinessCategoryUpdateRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "BUSINESS_CATEGORY_NO_FIELD_TO_UPDATE / _FIELD_TOO_LONG / _NAME_REQUIRED / _CODE_INVALID"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "BUSINESS_CATEGORY_ADMIN_ONLY — ADMIN 이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "BUSINESS_CATEGORY_NOT_FOUND — 카테고리가 없거나 이미 삭제됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "BUSINESS_CATEGORY_NAME_DUPLICATED / _CODE_DUPLICATED")
    })
    @PatchMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<BusinessCategoryDetailResponse>> updateCategory(
            @Parameter(description = "수정할 카테고리 ID")
            @PathVariable Long categoryId,
            @RequestBody JsonNode requestBody,
            Authentication authentication
    ) {
        BusinessCategoryResult result = businessCategoryCommandUseCase.updateCategory(
                toUpdateCommand(categoryId, requestBody, currentRole(authentication)));

        return ResponseEntity.ok(
                ApiResponse.success(BusinessCategoryResponseMessage.SUCCESS,
                        BusinessCategoryDetailResponse.from(result)));
    }

    /** 세션 권한(ROLE_ADMIN 형태)에서 전역 role 문자열을 꺼낸다. */
    private String currentRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .findFirst()
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .orElse("");
    }

    /** raw JSON 에서 필드 존재 여부(생략 vs null)를 직접 확인해 커맨드로 옮긴다. */
    private UpdateBusinessCategoryCommand toUpdateCommand(Long categoryId, JsonNode body, String role) {
        boolean nameProvided = body.has("name");
        boolean codeProvided = body.has("code");
        boolean descriptionProvided = body.has("description");

        return new UpdateBusinessCategoryCommand(
                categoryId,
                nameProvided, textOrNull(body, "name"),
                codeProvided, textOrNull(body, "code"),
                descriptionProvided, textOrNull(body, "description"),
                role);
    }

    private String textOrNull(JsonNode body, String field) {
        JsonNode value = body.get(field);
        return (value == null || value.isNull()) ? null : value.asText();
    }
}