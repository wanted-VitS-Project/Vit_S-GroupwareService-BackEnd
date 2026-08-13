package com.group3.vitamins.employee.presentation.api.request;

import com.group3.vitamins.employee.application.command.CertificateItem;
import com.group3.vitamins.employee.application.command.EducationItem;
import com.group3.vitamins.employee.application.command.RegisterEmployeeCommand;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 사원 등록 요청 (`employee.md` §3). 값 검증은 서비스에서 도메인 에러코드로 한다
 * (`@NotBlank` 등 프레임워크 검증은 명세 코드가 아니라 COMMON_* 을 내보내므로 쓰지 않는다 — 아키텍처 §4).
 *
 * <p>학력·자격증은 선택 배열이다. 등록 시엔 "전체 교체" 개념이 없어 배열이 있으면 그대로 저장, 없으면(생략·null) 없음이다.
 */
public record EmployeeRegisterRequest(
        @Schema(description = "사번(로그인 아이디)", example = "EMP021")
        String userId,
        @Schema(description = "이름", example = "홍길동")
        String name,
        @Schema(description = "부서 ID", example = "2")
        Long departmentId,
        @Schema(description = "입사일 yyyy-MM-dd", example = "2026-08-05")
        String hiredAt,
        @Schema(description = "전역 권한 (MASTER·MEMBER, ADMIN 불가)", example = "MEMBER")
        String role,
        @Schema(description = "직급 ID (선택)", example = "10")
        Long jobPositionId,
        @Schema(description = "초기 비밀번호를 보낼 이메일 (선택)", example = "hong@vitamins.com")
        String email,
        @Schema(description = "연락처 (선택)", example = "010-1234-5678")
        String phone,
        @Schema(description = "학력 (선택). 전공은 마스터에 있어야 한다")
        List<EducationRequest> educations,
        @Schema(description = "자격증 (선택). 자격증은 마스터에 있어야 한다")
        List<CertificateRequest> certificates
) {

    public RegisterEmployeeCommand toCommand(String actorRole) {
        return new RegisterEmployeeCommand(
                actorRole, userId, name, departmentId, hiredAt, role, jobPositionId, email, phone,
                toEducationItems(educations), toCertificateItems(certificates));
    }

    private static List<EducationItem> toEducationItems(List<EducationRequest> educations) {
        if (educations == null) {
            return List.of();
        }
        return educations.stream()
                .map(e -> new EducationItem(e.majorId(), e.degree(), e.school()))
                .toList();
    }

    private static List<CertificateItem> toCertificateItems(List<CertificateRequest> certificates) {
        if (certificates == null) {
            return List.of();
        }
        return certificates.stream()
                .map(c -> new CertificateItem(c.certificateId(), c.acquiredDate()))
                .toList();
    }

    /** 학력 한 건 (`employee.md` §3 {@code educations[]}). */
    public record EducationRequest(
            @Schema(description = "전공 마스터 ID", example = "3")
            Long majorId,
            @Schema(description = "학위 (BACHELOR·MASTER·DOCTOR)", example = "BACHELOR")
            String degree,
            @Schema(description = "학교 (선택)", example = "한국대학교")
            String school
    ) {
    }

    /** 자격증 한 건 (`employee.md` §3 {@code certificates[]}). */
    public record CertificateRequest(
            @Schema(description = "자격증 마스터 ID", example = "7")
            Long certificateId,
            @Schema(description = "취득일 yyyy-MM-dd (선택)", example = "2023-05-20")
            String acquiredDate
    ) {
    }
}
