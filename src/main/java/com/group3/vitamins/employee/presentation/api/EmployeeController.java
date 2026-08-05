package com.group3.vitamins.employee.presentation.api;

import com.group3.vitamins.employee.application.query.EmployeeListQuery;
import com.group3.vitamins.employee.application.query.EmployeeSearchQuery;
import com.group3.vitamins.employee.application.usecase.EmployeeAdminQueryUseCase;
import com.group3.vitamins.employee.application.usecase.EmployeeQueryUseCase;
import com.group3.vitamins.employee.presentation.api.response.EmployeeDetailResponse;
import com.group3.vitamins.employee.presentation.api.response.EmployeePageResponse;
import com.group3.vitamins.employee.presentation.api.response.EmployeeSearchResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Employee - 사원", description = "사원 목록·상세·검색 — 담당: 김동현")
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final EmployeeQueryUseCase employeeQueryUseCase;
    private final EmployeeAdminQueryUseCase employeeAdminQueryUseCase;

    @Operation(summary = "사원 목록 조회 (ADMIN)",
            description = "인사관리용 사원 목록. 시스템 계정은 어떤 조건으로도 조회되지 않으며, 기본은 재직자만 내려간다. "
                    + "keyword 는 이름·사번 부분 검색, status 는 화면 배지 기준(ACTIVE·RESET_REQUIRED·INACTIVE)이다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공 (0건이면 빈 목록)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EMP_INVALID_PARAMETER — 허용되지 않는 필터 값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님 (MASTER 포함)")
    })
    @GetMapping
    public ApiResponse<EmployeePageResponse> listEmployees(
            @Parameter(description = "이름 또는 사번 부분 검색")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "부서 필터")
            @RequestParam(required = false) Long departmentId,
            @Parameter(description = "권한 필터 (MASTER · MEMBER)")
            @RequestParam(required = false) String role,
            @Parameter(description = "상태 필터 (ACTIVE · RESET_REQUIRED · INACTIVE)")
            @RequestParam(required = false) String status,
            @Parameter(description = "퇴사 여부. 미지정이면 재직자만")
            @RequestParam(required = false) Boolean resigned,
            @Parameter(description = "0-base 페이지 번호 (기본 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기 (기본 20)")
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        EmployeePageResponse data = EmployeePageResponse.from(
                employeeAdminQueryUseCase.listEmployees(new EmployeeListQuery(
                        currentRole(authentication),
                        keyword, departmentId, role, status, resigned, page, size)));

        return ApiResponse.success(EmployeeResponseMessage.LIST_SUCCESS, data);
    }

    @Operation(summary = "사원 이름 검색 (결재선 지정용)",
            description = "이름 부분 일치로 결재자 후보를 찾는다. 결재선 등록 화면의 자동완성이라 "
                    + "ADMIN 이 아닌 로그인 사용자 누구나 호출한다. 시스템 계정·퇴사자는 후보에 나오지 않으며, "
                    + "급여 등 민감 정보는 응답에 포함하지 않는다(userId·name·department·position 4개 필드만).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공 (결과 없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EMP_INVALID_PARAMETER — name 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @GetMapping("/search")
    public ApiResponse<List<EmployeeSearchResponse>> searchEmployees(
            @Parameter(description = "이름 부분 일치 검색어 (필수)")
            @RequestParam(required = false) String name) {

        // name 은 required=false 로 받고 서비스에서 검증한다 — @RequestParam 기본 검증에 맡기면
        // 명세 코드(EMP_INVALID_PARAMETER)가 아니라 전역 COMMON_* 이 새어 나간다 (`.ai/API.md` §0).
        List<EmployeeSearchResponse> data = employeeQueryUseCase.searchByName(new EmployeeSearchQuery(name))
                .stream()
                .map(EmployeeSearchResponse::from)
                .toList();

        return ApiResponse.success(EmployeeResponseMessage.SEARCH_SUCCESS, data);
    }

    @Operation(summary = "사원 상세 조회 (ADMIN)",
            description = "사번으로 사원 상세를 조회한다. 목록 필드에 더해 부서·직급 ID, 연락처, 입사일, 마지막 로그인, "
                    + "소속 그룹을 내려준다. 시스템 계정은 존재해도 403 으로 막는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED / ACC_SYSTEM_ACCOUNT_NOT_ALLOWED — 권한 없음 / 시스템 계정"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "EMP_NOT_FOUND — 사원 없음")
    })
    @GetMapping("/{userId}")
    public ApiResponse<EmployeeDetailResponse> getEmployee(
            @Parameter(description = "조회할 사번")
            @PathVariable String userId,
            Authentication authentication) {

        EmployeeAdminQueryUseCase.EmployeeDetail detail =
                employeeAdminQueryUseCase.getEmployee(currentRole(authentication), userId);

        return ApiResponse.success(EmployeeResponseMessage.DETAIL_SUCCESS,
                EmployeeDetailResponse.from(detail.employee(), detail.groups()));
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
