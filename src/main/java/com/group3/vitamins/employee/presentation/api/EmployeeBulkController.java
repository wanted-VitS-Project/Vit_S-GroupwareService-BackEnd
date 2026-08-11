package com.group3.vitamins.employee.presentation.api;

import com.group3.vitamins.employee.application.command.RegisterBulkCommand;
import com.group3.vitamins.employee.application.command.ValidateBulkCommand;
import com.group3.vitamins.employee.application.result.BulkRegisterResult;
import com.group3.vitamins.employee.application.result.BulkValidateResult;
import com.group3.vitamins.employee.application.usecase.EmployeeBulkUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.presentation.api.response.BulkRegisterResponse;
import com.group3.vitamins.employee.presentation.api.response.BulkValidateResponse;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Employee - 엑셀 일괄 등록", description = "템플릿 다운로드·검증·일괄 등록 (ADMIN 전용) — 담당: 김동현")
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeBulkController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024; // 5MB (서비스 검증과 동일)

    private final EmployeeBulkUseCase employeeBulkUseCase;

    @Operation(summary = "엑셀 템플릿 내려받기 (ADMIN)",
            description = "사원 일괄 등록용 빈 템플릿(.xlsx)을 내려받는다. 컬럼은 사번·이름·부서명·직급명·입사일·이메일·연락처·권한. "
                    + "권한 컬럼은 있지만 ADMIN 값은 검증에서 거부된다. 응답은 JSON 이 아니라 파일 바이너리다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = ".xlsx 바이너리"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED — ADMIN 아님")
    })
    @GetMapping("/bulk-template")
    public ResponseEntity<byte[]> downloadTemplate(Authentication authentication) {
        byte[] body = employeeBulkUseCase.getTemplate(RequesterRole.from(authentication));

        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"employee_bulk_template.xlsx\"")
                .body(body);
    }

    @Operation(summary = "엑셀 일괄 등록 검증 (ADMIN)",
            description = "업로드한 엑셀을 등록하지 않고 검증만 한다(화면 스텝퍼 ②). 행별 오류를 돌려주며 오류가 있어도 성공(200)이다. "
                    + "파일 없음·형식 아님·5MB 초과만 400 이고, 파일을 연 뒤의 오류는 모두 data.errors 로 간다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검증 완료(오류가 있어도 200)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EMP_FILE_REQUIRED · EMP_FILE_TYPE_INVALID · EMP_FILE_SIZE_EXCEEDED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED — ADMIN 아님")
    })
    @PostMapping(value = "/bulk/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BulkValidateResponse> validateBulk(
            // required=false 라야 파일 part 누락 시 Spring 이 컨트롤러 前에 튕기지 않고 서비스가 EMP_FILE_REQUIRED 로 판정한다.
            @RequestParam(value = "file", required = false) MultipartFile file,
            Authentication authentication) {

        BulkValidateResult result = employeeBulkUseCase.validate(toValidateCommand(file, authentication));

        String message = result.errorCount() == 0
                ? "검증 완료"
                : result.totalRows() + "건 중 " + result.errorCount() + "건 오류";
        return ApiResponse.success(message, BulkValidateResponse.from(result));
    }

    @Operation(summary = "엑셀 일괄 등록 (ADMIN)",
            description = "검증을 통과한 사원을 실제로 등록한다(화면 스텝퍼 ③). skipErrors=false(기본)면 오류 행이 있을 때 "
                    + "EMP_HAS_ERRORS(400)로 막고, true 면 오류 행을 빼고 유효 행만 등록한다(부분 등록). 행마다 독립 트랜잭션이라 "
                    + "일부가 실패해도 나머지는 등록된다. 초기 비밀번호는 이메일이 있는 사원에게만 발송된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EMP_FILE_REQUIRED · EMP_FILE_TYPE_INVALID · EMP_FILE_SIZE_EXCEEDED · EMP_HAS_ERRORS(skipErrors=false 인데 오류 행 있음)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED — ADMIN 아님")
    })
    @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BulkRegisterResponse> registerBulk(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "skipErrors", defaultValue = "false") boolean skipErrors,
            Authentication authentication) {

        BulkRegisterResult result = employeeBulkUseCase.register(
                toRegisterCommand(file, skipErrors, authentication));

        String message = result.failedCount() == 0
                ? result.registeredCount() + "건 등록 완료"
                : result.registeredCount() + "건 등록 완료 (실패 " + result.failedCount() + "건)";
        return ApiResponse.success(message, BulkRegisterResponse.from(result));
    }

    private ValidateBulkCommand toValidateCommand(MultipartFile file, Authentication authentication) {
        String role = RequesterRole.from(authentication);
        if (file == null || file.isEmpty()) {
            // 파일 없음은 서비스가 EMP_FILE_REQUIRED 로 판정한다(빈 커맨드).
            return new ValidateBulkCommand(role, null, null, 0);
        }
        return new ValidateBulkCommand(role, readBytesWithinLimit(file), file.getOriginalFilename(), file.getSize());
    }

    private RegisterBulkCommand toRegisterCommand(MultipartFile file, boolean skipErrors, Authentication authentication) {
        String role = RequesterRole.from(authentication);
        if (file == null || file.isEmpty()) {
            return new RegisterBulkCommand(role, null, null, 0, skipErrors);
        }
        return new RegisterBulkCommand(role, readBytesWithinLimit(file), file.getOriginalFilename(), file.getSize(), skipErrors);
    }

    /** 5MB 초과면 바이트를 읽기 전에 막는다(멀티파트 전역 상한 20MB 라 읽어오면 그만큼 메모리를 쓴다). */
    private byte[] readBytesWithinLimit(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException(EmployeeErrorCode.EMP_FILE_SIZE_EXCEEDED);
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new ValidationException(EmployeeErrorCode.EMP_FILE_REQUIRED, e);
        }
    }
}
