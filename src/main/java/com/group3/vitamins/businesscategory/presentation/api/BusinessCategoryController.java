package com.group3.vitamins.businesscategory.presentation.api;

import com.group3.vitamins.businesscategory.application.query.BusinessCategoryListQuery;
import com.group3.vitamins.businesscategory.application.usecase.BusinessCategoryQueryUseCase;
import com.group3.vitamins.businesscategory.presentation.api.response.BusinessCategoryListResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
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

    /** 세션 권한(ROLE_ADMIN 형태)에서 전역 role 문자열을 꺼낸다. */
    private String currentRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith(ROLE_PREFIX))
                .findFirst()
                .map(authority -> authority.substring(ROLE_PREFIX.length()))
                .orElse("");
    }
}