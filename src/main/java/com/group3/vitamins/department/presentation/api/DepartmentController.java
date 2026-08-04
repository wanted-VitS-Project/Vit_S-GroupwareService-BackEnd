package com.group3.vitamins.department.presentation.api;

import com.group3.vitamins.department.application.DepartmentQueryService;
import com.group3.vitamins.department.presentation.api.dto.response.DepartmentListResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부서 관리 API — `.ai/api/department.md`.
 *
 * <p>목록 조회는 <b>전체 사용자</b>(인증만)이고, 생성·수정·삭제는 <b>ADMIN 전용</b>이다.
 * 인증(세션)은 Security 필터가 보고, ADMIN 판정은 서비스가 도메인 코드({@code ACC_ADMIN_REQUIRED})와 함께 한다.
 */
@Tag(name = "Department - 부서 관리", description = "부서 트리 조회(전체 사용자) / 생성·수정·삭제(ADMIN 전용, 담당: 김동현)")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentQueryService departmentQueryService;

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
        return ApiResponse.success("부서 목록 조회 성공", departmentQueryService.getDepartmentTree());
    }
}
