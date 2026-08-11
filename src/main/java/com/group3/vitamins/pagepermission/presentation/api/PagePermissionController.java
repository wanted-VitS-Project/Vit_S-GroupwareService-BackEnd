package com.group3.vitamins.pagepermission.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.pagepermission.application.result.RevokeResult;
import com.group3.vitamins.pagepermission.application.usecase.PagePermissionCommandUseCase;
import com.group3.vitamins.pagepermission.application.usecase.PagePermissionQueryUseCase;
import com.group3.vitamins.pagepermission.presentation.api.request.GrantPermissionsRequest;
import com.group3.vitamins.pagepermission.presentation.api.response.GrantPermissionsResponse;
import com.group3.vitamins.pagepermission.presentation.api.response.MyPageResponse;
import com.group3.vitamins.pagepermission.presentation.api.response.MyPagesResponse;
import com.group3.vitamins.pagepermission.presentation.api.response.PageAccessListResponse;
import com.group3.vitamins.pagepermission.presentation.api.response.PageListItemResponse;
import com.group3.vitamins.pagepermission.presentation.api.response.PageListResponse;
import com.group3.vitamins.pagepermission.presentation.api.response.RevokePermissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "PagePermission - 페이지 권한", description = "페이지 고정 카탈로그의 노출·접근 판정과 부여·회수")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PagePermissionController {

    private final PagePermissionQueryUseCase queryUseCase;
    private final PagePermissionCommandUseCase commandUseCase;

    @Operation(summary = "내 페이지 목록 조회 (§1)",
            description = "사이드바 버튼 노출의 유일한 근거. 노출되는 페이지를 permission·source 와 함께 카탈로그 순서로 준다. "
                    + "permission=NONE 은 '버튼은 그리되 접근은 막으라'(부여 전 MEMBER 의 BIDDING·FINANCE). 전체 사용자.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED")
    })
    @GetMapping("/my/pages")
    public ApiResponse<MyPagesResponse> getMyPages(Authentication authentication) {
        List<MyPageResponse> content = queryUseCase
                .getMyPages(authentication.getName(), RequesterRole.from(authentication))
                .stream().map(MyPageResponse::from).toList();
        return ApiResponse.success(PagePermissionResponseMessage.MY_PAGES, new MyPagesResponse(content));
    }

    @Operation(summary = "페이지 목록 조회 (§2)",
            description = "페이지 권한 화면의 목록. 부여 가능한 페이지(BIDDING·FINANCE)만 나오며 접근 인원 집계를 함께 준다. ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED")
    })
    @GetMapping("/pages")
    public ApiResponse<PageListResponse> listPages(Authentication authentication) {
        List<PageListItemResponse> content = queryUseCase.listPages(RequesterRole.from(authentication))
                .stream().map(PageListItemResponse::from).toList();
        return ApiResponse.success(PagePermissionResponseMessage.PAGE_LIST, new PageListResponse(content));
    }

    @Operation(summary = "페이지 접근 가능자 목록 (§3)",
            description = "명시 부여자(GRANTED·회수 가능) + 전역 권한 열람자(MASTER·GLOBAL_ROLE·회수 불가). GRANTED 먼저, 이름순. ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PAGE_NOT_FOUND")
    })
    @GetMapping("/pages/{pageCode}/permissions")
    public ApiResponse<PageAccessListResponse> getPageAccess(
            @PathVariable String pageCode, Authentication authentication) {
        PageAccessListResponse data = PageAccessListResponse.from(
                queryUseCase.getPageAccess(RequesterRole.from(authentication), pageCode));
        return ApiResponse.success(PagePermissionResponseMessage.PAGE_ACCESS, data);
    }

    @Operation(summary = "페이지 권한 부여 (§4)",
            description = "부여와 등급 변경이 같은 API(이미 있으면 갱신). 전체 교체가 아니라 요청 목록만 처리한다. "
                    + "그룹 일괄도 개인 단위로 저장. ADMIN 은 대상 불가(시스템 계정), 없는 사번이 섞이면 전체 거부. ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "PAGE_INVALID_REQUEST / PAGE_INVALID_PERMISSION"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED / ACC_SYSTEM_ACCOUNT_NOT_ALLOWED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PAGE_NOT_FOUND / EMP_NOT_FOUND")
    })
    @PostMapping("/pages/{pageCode}/permissions")
    public ApiResponse<GrantPermissionsResponse> grant(
            @PathVariable String pageCode,
            @RequestBody GrantPermissionsRequest request,
            Authentication authentication) {
        GrantPermissionsResponse data = GrantPermissionsResponse.from(
                commandUseCase.grant(request.toCommand(RequesterRole.from(authentication), pageCode)));
        return ApiResponse.success(PagePermissionResponseMessage.GRANTED, data);
    }

    @Operation(summary = "페이지 권한 회수 (§5)",
            description = "명시적 부여 기록만 회수한다. 대상이 ADMIN·MASTER 면 전역 권한으로 열람은 계속된다(stillAccessible=true). "
                    + "부여 기록이 없으면 PAGE_PERMISSION_NOT_FOUND. ADMIN.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회수 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "PAGE_NOT_FOUND / PAGE_PERMISSION_NOT_FOUND")
    })
    @DeleteMapping("/pages/{pageCode}/permissions/{userId}")
    public ApiResponse<RevokePermissionResponse> revoke(
            @PathVariable String pageCode,
            @PathVariable String userId,
            Authentication authentication) {
        RevokeResult result = commandUseCase.revoke(
                new com.group3.vitamins.pagepermission.application.command.RevokePermissionCommand(
                        RequesterRole.from(authentication), pageCode, userId));
        String message = result.stillAccessible()
                ? PagePermissionResponseMessage.REVOKED_STILL_ACCESSIBLE
                : PagePermissionResponseMessage.REVOKED;
        return ApiResponse.success(message, RevokePermissionResponse.from(result));
    }
}
