package com.group3.vitamins.employee.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 사원 정보 수정 요청 — <b>Swagger 문서화 전용</b> 스키마 (`employee.md` §4).
 *
 * <p>실제 파싱은 컨트롤러가 {@code JsonNode} 로 한다 — "필드 생략 vs 명시적 null"(특히 jobPositionId 로 직급
 * 지우기)을 구별해야 해서 record 바인딩으로는 표현할 수 없다. 이 클래스는 요청 형태를 문서에 보여주기만 한다.
 */
public record EmployeeUpdateRequest(
        @Schema(description = "이름", example = "홍길동")
        String name,
        @Schema(description = "연락처", example = "010-1234-5678")
        String phone,
        @Schema(description = "이메일", example = "hong@vitamins.com")
        String email,
        @Schema(description = "부서 ID", example = "3")
        Long departmentId,
        @Schema(description = "직급 ID (null 을 보내면 직급 미지정으로 변경)", example = "10")
        Long jobPositionId,
        @Schema(description = "입사일 yyyy-MM-dd", example = "2024-03-02")
        String hiredAt,
        @Schema(description = "학력 — 전체 교체. 생략/null 이면 유지, [] 면 전부 삭제")
        java.util.List<EmployeeRegisterRequest.EducationRequest> educations,
        @Schema(description = "자격증 — 전체 교체. 생략/null 이면 유지, [] 면 전부 삭제")
        java.util.List<EmployeeRegisterRequest.CertificateRequest> certificates
) {
}
