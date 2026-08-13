package com.group3.vitamins.employee.presentation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.employee.application.command.CertificateItem;
import com.group3.vitamins.employee.application.command.EducationItem;
import com.group3.vitamins.employee.application.command.UpdateEmployeeCommand;
import com.group3.vitamins.employee.application.query.EmployeeListQuery;
import com.group3.vitamins.employee.application.query.EmployeeSearchQuery;
import com.group3.vitamins.employee.application.usecase.EmployeeAdminQueryUseCase;
import com.group3.vitamins.employee.application.usecase.EmployeeCommandUseCase;
import com.group3.vitamins.employee.application.usecase.EmployeeQueryUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.presentation.api.request.EmployeeRegisterRequest;
import com.group3.vitamins.employee.presentation.api.request.EmployeeResignRequest;
import com.group3.vitamins.employee.presentation.api.response.EmployeeDetailResponse;
import com.group3.vitamins.employee.presentation.api.response.EmployeePageResponse;
import com.group3.vitamins.employee.presentation.api.response.EmployeeRegisterResponse;
import com.group3.vitamins.employee.presentation.api.response.EmployeeResignResponse;
import com.group3.vitamins.employee.presentation.api.response.EmployeeSearchResponse;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Employee - 사원", description = "사원 목록·상세·검색·등록·수정·퇴사 — 담당: 김동현")
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private static final String ROLE_PREFIX = "ROLE_";

    private final EmployeeQueryUseCase employeeQueryUseCase;
    private final EmployeeAdminQueryUseCase employeeAdminQueryUseCase;
    private final EmployeeCommandUseCase employeeCommandUseCase;

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

    @Operation(summary = "사원 등록 (ADMIN)",
            description = "사원을 등록하면 계정이 함께 발급된다(로그인 아이디 = 사번). 초기 비밀번호는 이메일로 발송되며, "
                    + "이메일이 없어도 등록은 되지만 비밀번호를 전달할 수 없어 로그인하지 못한다. ADMIN 권한은 부여할 수 없다. "
                    + "메일 발송이 실패해도 등록은 성공(201)이며 emailSent=false 로 내려간다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EMP_INVALID_REQUEST(필수값 누락/형식 오류) · EMP_ADMIN_ROLE_NOT_ALLOWED(role=ADMIN)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED — ADMIN 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "EMP_DEPARTMENT_NOT_FOUND · EMP_JOB_POSITION_NOT_FOUND — 부서/직급 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "EMP_USER_ID_DUPLICATED — 이미 등록된 사번")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<EmployeeRegisterResponse> registerEmployee(
            @RequestBody EmployeeRegisterRequest request,
            Authentication authentication) {

        EmployeeRegisterResponse data = EmployeeRegisterResponse.from(
                employeeCommandUseCase.register(request.toCommand(currentRole(authentication))));

        return ApiResponse.created(EmployeeResponseMessage.REGISTERED, data);
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
                EmployeeDetailResponse.from(detail.employee(), detail.groups(),
                        detail.educations(), detail.certificates()));
    }

    @Operation(summary = "사원 정보 수정 (ADMIN)",
            description = "전달한 필드만 수정한다. 사번·전역 권한은 이 API 로 바꾸지 않는다. jobPositionId 에 null 을 "
                    + "보내면 직급이 미지정으로 바뀐다. 응답은 사원 상세와 같은 구조다. 시스템 계정은 수정할 수 없다.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(schema = @Schema(implementation = com.group3.vitamins.employee.presentation.api.request.EmployeeUpdateRequest.class)))
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EMP_INVALID_REQUEST — 형식 오류 또는 수정할 필드 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED / ACC_SYSTEM_ACCOUNT_NOT_ALLOWED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "EMP_NOT_FOUND · EMP_DEPARTMENT_NOT_FOUND · EMP_JOB_POSITION_NOT_FOUND")
    })
    @PatchMapping("/{userId}")
    public ApiResponse<EmployeeDetailResponse> updateEmployee(
            @Parameter(description = "수정할 사번")
            @PathVariable String userId,
            @RequestBody JsonNode requestBody,
            Authentication authentication) {

        String role = currentRole(authentication);
        employeeCommandUseCase.updateEmployee(toUpdateCommand(userId, requestBody, role));

        // 응답은 상세 구조 — 수정 커밋 후 조회 유스케이스로 다시 읽어 목록·상세와 같은 규칙으로 조립한다.
        EmployeeAdminQueryUseCase.EmployeeDetail detail =
                employeeAdminQueryUseCase.getEmployee(role, userId);
        return ApiResponse.success(EmployeeResponseMessage.UPDATED,
                EmployeeDetailResponse.from(detail.employee(), detail.groups(),
                        detail.educations(), detail.certificates()));
    }

    @Operation(summary = "퇴사 처리 (ADMIN)",
            description = "사원 정보는 지우지 않고 퇴사일을 기록하며 계정을 함께 INACTIVE 로 바꾼다(별도 상태변경 API 불필요). "
                    + "이미 퇴사한 사원은 400. 시스템 계정은 대상이 될 수 없다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "퇴사 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EMP_INVALID_REQUEST(형식 오류) · EMP_ALREADY_RESIGNED(이미 퇴사)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "ACC_ADMIN_REQUIRED / ACC_SYSTEM_ACCOUNT_NOT_ALLOWED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "EMP_NOT_FOUND — 사원 없음")
    })
    @PatchMapping("/{userId}/resignation")
    public ApiResponse<EmployeeResignResponse> resignEmployee(
            @Parameter(description = "퇴사할 사번")
            @PathVariable String userId,
            @RequestBody EmployeeResignRequest request,
            Authentication authentication) {

        EmployeeResignResponse data = EmployeeResignResponse.from(
                employeeCommandUseCase.resignEmployee(request.toCommand(currentRole(authentication), userId)));

        return ApiResponse.success(EmployeeResponseMessage.RESIGNED, data);
    }

    /**
     * raw JSON 에서 필드 존재 여부(생략 vs 값 전달)와 타입을 직접 판별해 커맨드로 옮긴다 (job-position 수정 선례).
     *
     * <p>{@code asText()}·{@code asLong()} 는 타입을 강제 변환하므로 명세와 다른 타입이 조용히 통과한다 —
     * 문자열 필드에 숫자/불리언, 숫자 필드에 문자열이 오면 {@code EMP_INVALID_REQUEST}(400)로 막는다.
     * {@code null} 값은 "전달됨"으로 취급한다 — jobPositionId 는 null 로 직급을 지울 수 있어야 하기 때문이다.
     */
    private UpdateEmployeeCommand toUpdateCommand(String userId, JsonNode body, String role) {
        // 학력·자격증은 전체 교체다. 미전송·명시적 null 은 "유지"(provided=false)이고, 배열([] 포함)만 교체 대상이다.
        List<EducationItem> educations = educationItems(body);
        List<CertificateItem> certificates = certificateItems(body);
        return new UpdateEmployeeCommand(
                role, userId,
                body.has("name"), textOrNull(body, "name"),
                body.has("phone"), textOrNull(body, "phone"),
                body.has("email"), textOrNull(body, "email"),
                body.has("departmentId"), longOrNull(body, "departmentId"),
                body.has("jobPositionId"), longOrNull(body, "jobPositionId"),
                body.has("hiredAt"), textOrNull(body, "hiredAt"),
                educations != null, educations,
                certificates != null, certificates);
    }

    /** 학력 배열 파싱. 미전송·null 이면 {@code null}(유지), 배열이면 항목 목록([] 은 빈 목록=삭제). 그 외 타입은 400. */
    private List<EducationItem> educationItems(JsonNode body) {
        JsonNode arr = arrayOrNull(body, "educations");
        if (arr == null) {
            return null;
        }
        List<EducationItem> items = new java.util.ArrayList<>();
        for (JsonNode node : arr) {
            requireObject(node);
            items.add(new EducationItem(longOrNull(node, "majorId"), textOrNull(node, "degree"), textOrNull(node, "school")));
        }
        return items;
    }

    /** 자격증 배열 파싱. 규칙은 학력과 같다. */
    private List<CertificateItem> certificateItems(JsonNode body) {
        JsonNode arr = arrayOrNull(body, "certificates");
        if (arr == null) {
            return null;
        }
        List<CertificateItem> items = new java.util.ArrayList<>();
        for (JsonNode node : arr) {
            requireObject(node);
            items.add(new CertificateItem(longOrNull(node, "certificateId"), textOrNull(node, "acquiredDate")));
        }
        return items;
    }

    /** 필드가 배열이면 그 노드를, 미전송·명시적 null 이면 null 을 반환. 배열도 null 도 아니면 400. */
    private JsonNode arrayOrNull(JsonNode body, String field) {
        if (!body.has(field)) {
            return null;
        }
        JsonNode node = body.get(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isArray()) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return node;
    }

    private void requireObject(JsonNode node) {
        if (!node.isObject()) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
    }

    /** 전달됐다면 문자열이거나 null 이어야 한다. 그 외 타입이면 400. */
    private String textOrNull(JsonNode body, String field) {
        if (!body.has(field)) {
            return null;
        }
        JsonNode node = body.get(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return node.asText();
    }

    /** 전달됐다면 long 범위 정수이거나 null 이어야 한다. 그 외 타입·범위 초과면 400. */
    private Long longOrNull(JsonNode body, String field) {
        if (!body.has(field)) {
            return null;
        }
        JsonNode node = body.get(field);
        if (node.isNull()) {
            return null;
        }
        // isIntegralNumber() 만 보면 long 범위를 넘는 BigInteger 도 통과하고 asLong() 이 값을 축소한다.
        // canConvertToLong() 으로 범위를 함께 검사한다.
        if (!node.isIntegralNumber() || !node.canConvertToLong()) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_REQUEST);
        }
        return node.asLong();
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
