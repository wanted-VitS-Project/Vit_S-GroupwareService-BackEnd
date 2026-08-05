package com.group3.vitamins.employee.presentation.api;

import com.group3.vitamins.employee.application.query.EmployeeSearchQuery;
import com.group3.vitamins.employee.application.usecase.EmployeeQueryUseCase;
import com.group3.vitamins.employee.presentation.api.response.EmployeeSearchResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Employee - 사원", description = "사원 검색 (결재선 지정용) — 담당: 김동현")
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeQueryUseCase employeeQueryUseCase;

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
}
