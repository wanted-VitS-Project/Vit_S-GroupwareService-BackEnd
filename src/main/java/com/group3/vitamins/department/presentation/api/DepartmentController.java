package com.group3.vitamins.department.presentation.api;

import com.group3.vitamins.department.application.command.DeleteDepartmentCommand;
import com.group3.vitamins.department.application.usecase.DepartmentCommandUseCase;
import com.group3.vitamins.department.application.usecase.DepartmentQueryUseCase;
import com.group3.vitamins.department.presentation.api.request.CreateDepartmentRequest;
import com.group3.vitamins.department.presentation.api.request.UpdateDepartmentRequest;
import com.group3.vitamins.department.presentation.api.response.DepartmentCreateResponse;
import com.group3.vitamins.department.presentation.api.response.DepartmentListResponse;
import com.group3.vitamins.department.presentation.api.response.DepartmentUpdateResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부서 관리 API — `.ai/api/department.md`.
 *
 * <p>목록 조회는 <b>전체 사용자</b>(인증만)이고, 생성·수정·삭제는 <b>ADMIN 전용</b>이다.
 * 인증(세션)은 Security 필터가 보고, ADMIN 판정은 {@code DepartmentAdminPolicy} 가 도메인 코드
 * ({@code ACC_ADMIN_REQUIRED})와 함께 한다.
 */
@Tag(name = "Department - 부서 관리", description = "부서 트리 조회(전체 사용자) / 생성·수정·삭제(ADMIN 전용, 담당: 김동현)")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final DepartmentQueryUseCase departmentQueryUseCase;
    private final DepartmentCommandUseCase departmentCommandUseCase;

    @Operation(summary = "부서 목록 조회",
            description = "전체 부서를 최대 2단 트리로 반환한다. 페이징 없음, 정렬은 생성 순(departmentId 오름차순). "
                    + "각 부서에 직속 인원 수와 하위 포함 인원 수를 함께 담는다(시스템 계정·퇴사자 제외). "
                    + "사원 등록·필터·구성원 선택 화면에서 쓰이므로 전체 사용자가 호출한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @GetMapping
    public ApiResponse<DepartmentListResponse> getDepartments() {
        return ApiResponse.success(DepartmentResponseMessage.LIST_SUCCESS,
                DepartmentListResponse.from(departmentQueryUseCase.getDepartmentTree()));
    }

    @Operation(summary = "부서 생성",
            description = "최상위 부서 또는 하위 부서를 만든다. parentId 유무로 갈린다(하위 부서 추가는 parentId 고정). "
                    + "계층은 최대 2단이라 하위 부서를 parentId 로 지정하면 409. 부서명은 전체에서 중복될 수 없다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "DEPT_INVALID_REQUEST — 부서명이 비었거나 50자 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "DEPT_PARENT_NOT_FOUND — 상위 부서 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "DEPT_NAME_DUPLICATED — 이미 존재하는 부서명 · "
                            + "DEPT_MAX_DEPTH_EXCEEDED — 하위 부서를 상위로 지정(계층 최대 2단)")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<DepartmentCreateResponse> createDepartment(Authentication authentication,
                                                                  @RequestBody CreateDepartmentRequest request) {
        DepartmentCreateResponse result = DepartmentCreateResponse.from(
                departmentCommandUseCase.create(request.toCommand(roleOf(authentication))));
        return ApiResponse.created(DepartmentResponseMessage.CREATED, result);
    }

    @Operation(summary = "부서명 수정",
            description = "부서명을 수정한다. 상위 부서는 바꿀 수 없다(부서 이동 기능 없음). "
                    + "부서명은 전체에서 중복될 수 없으며, 소속 사원의 배정은 그대로다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "DEPT_INVALID_REQUEST — 부서명이 비었거나 50자 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "DEPT_NOT_FOUND — 부서 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "DEPT_NAME_DUPLICATED — 이미 존재하는 부서명")
    })
    @PatchMapping("/{departmentId}")
    public ApiResponse<DepartmentUpdateResponse> updateDepartment(Authentication authentication,
                                                                  @PathVariable Long departmentId,
                                                                  @RequestBody UpdateDepartmentRequest request) {
        DepartmentUpdateResponse result = DepartmentUpdateResponse.from(
                departmentCommandUseCase.rename(request.toCommand(roleOf(authentication), departmentId)));
        return ApiResponse.success(DepartmentResponseMessage.UPDATED, result);
    }

    @Operation(summary = "부서 삭제",
            description = "부서를 삭제한다. 직속 사원이 있거나 하위 부서가 있으면 409 로 차단하며 message 에 개수를 담는다. "
                    + "하위 부서를 함께 지우지 않고, 소프트 삭제가 아니라 행을 제거한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "DEPT_NOT_FOUND — 부서 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "DEPT_HAS_EMPLOYEES — 직속 사원 있음(message 에 인원 수) · "
                            + "DEPT_HAS_CHILDREN — 하위 부서 있음(message 에 하위 부서 수)")
    })
    @DeleteMapping("/{departmentId}")
    public ApiResponse<Void> deleteDepartment(Authentication authentication,
                                              @PathVariable Long departmentId) {
        departmentCommandUseCase.delete(new DeleteDepartmentCommand(roleOf(authentication), departmentId));
        return ApiResponse.success(DepartmentResponseMessage.DELETED);
    }

    /**
     * 현재 로그인 사용자의 전역 권한을 추출한다.
     *
     * <p>세션에는 {@code ROLE_ADMIN} 처럼 접두어가 붙은 권한이 실려 있다 (`AuthSessionManager`).
     * 접두어를 떼어 {@code ADMIN}·{@code MASTER}·{@code MEMBER} 원값으로 돌려준다.
     */
    private String roleOf(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith(ROLE_PREFIX))
                .map(auth -> auth.substring(ROLE_PREFIX.length()))
                .findFirst()
                .orElse(null);
    }
}
