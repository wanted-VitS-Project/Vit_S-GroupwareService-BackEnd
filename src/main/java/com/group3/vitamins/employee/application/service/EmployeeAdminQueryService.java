package com.group3.vitamins.employee.application.service;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.employee.application.policy.EmployeeAdminPolicy;
import com.group3.vitamins.employee.application.port.EmployeeAdminQueryPort;
import com.group3.vitamins.employee.application.query.EmployeeListCriteria;
import com.group3.vitamins.employee.application.query.EmployeeListQuery;
import com.group3.vitamins.employee.application.result.EmployeeCertificateRow;
import com.group3.vitamins.employee.application.result.EmployeeDetailRow;
import com.group3.vitamins.employee.application.result.EmployeeEducationRow;
import com.group3.vitamins.employee.application.result.EmployeeGroupRow;
import com.group3.vitamins.employee.application.result.EmployeeListRow;
import com.group3.vitamins.employee.application.result.EmployeePage;
import com.group3.vitamins.employee.application.usecase.EmployeeAdminQueryUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 인사관리용 사원 조회 (`employee.md` §1·§2) — 목록·상세. 전부 ADMIN 전용이다.
 *
 * <p>이름 검색({@link EmployeeQueryService})과 별도 서비스로 둔다 — 검색은 로그인 사용자 누구나 쓰므로
 * 정책이 없고, 여기는 {@link EmployeeAdminPolicy} 로 ADMIN 을 강제한다. 두 관심사를 한 서비스에 섞으면
 * 정책 적용을 빠뜨리기 쉽다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeAdminQueryService implements EmployeeAdminQueryUseCase {

    private static final Set<String> ALLOWED_ROLES = Set.of("MASTER", "MEMBER");
    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "RESET_REQUIRED", "INACTIVE");

    /** 한 페이지 최대 크기. 30명 규모라 넉넉하며, ADMIN 이 거대한 LIMIT 조회로 부하를 주는 것을 막는다. */
    private static final int MAX_PAGE_SIZE = 200;

    private final EmployeeAdminQueryPort employeeAdminQueryPort;
    private final EmployeeAdminPolicy employeeAdminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public EmployeePage listEmployees(EmployeeListQuery query) {
        employeeAdminPolicy.assertAdmin(query.requesterRole());

        EmployeeListCriteria criteria = toCriteria(query);

        long total = employeeAdminQueryPort.count(criteria);
        List<EmployeeListRow> content = total == 0
                ? List.of()
                : employeeAdminQueryPort.findPage(criteria);

        return new EmployeePage(content, query.page(), query.size(), total);
    }

    @Override
    public EmployeeDetail getEmployee(String requesterRole, String userId) {
        employeeAdminPolicy.assertAdmin(requesterRole);

        EmployeeDetailRow employee = employeeAdminQueryPort
                .findDetail(userId, currentCompanyIdProvider.currentCompanyId())
                .orElseThrow(() -> new NotFoundException(EmployeeErrorCode.EMP_NOT_FOUND));

        // 시스템 계정(ADMIN 공용·배치)은 인사관리 대상이 아니다. 존재는 하므로 404 가 아니라 403 으로 막는다.
        if (employee.isSystem()) {
            throw new ForbiddenException(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED);
        }

        List<EmployeeGroupRow> groups = employeeAdminQueryPort.findGroups(userId);
        List<EmployeeEducationRow> educations = employeeAdminQueryPort.findEducations(userId);
        List<EmployeeCertificateRow> certificates = employeeAdminQueryPort.findCertificates(userId);
        return new EmployeeDetail(employee, groups, educations, certificates);
    }

    /**
     * 필터 값을 검증하고 화면용 {@code status} 를 (계정상태 · 비밀번호변경) 두 축으로 푼다.
     *
     * <p>허용되지 않는 role·status·페이징 값은 {@code EMP_INVALID_PARAMETER}(400)로 막는다 — 명세 계약이다.
     */
    private EmployeeListCriteria toCriteria(EmployeeListQuery query) {
        if (query.page() < 0 || query.size() <= 0 || query.size() > MAX_PAGE_SIZE) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_PARAMETER);
        }

        String role = normalize(query.role());
        if (role != null && !ALLOWED_ROLES.contains(role)) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_PARAMETER);
        }

        String status = normalize(query.status());
        if (status != null && !ALLOWED_STATUSES.contains(status)) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_PARAMETER);
        }

        // 화면 status → 계정상태 + 비밀번호변경 필요 여부.
        //   INACTIVE       : 계정 정지                     (비밀번호 상태 무관)
        //   RESET_REQUIRED : ACTIVE 이면서 비번 재설정 필요
        //   ACTIVE         : ACTIVE 이면서 비번 정상
        String accountStatus = null;
        Boolean mustChangePassword = null;
        if ("INACTIVE".equals(status)) {
            accountStatus = "INACTIVE";
        } else if ("RESET_REQUIRED".equals(status)) {
            accountStatus = "ACTIVE";
            mustChangePassword = true;
        } else if ("ACTIVE".equals(status)) {
            accountStatus = "ACTIVE";
            mustChangePassword = false;
        }

        boolean resignedOnly = Boolean.TRUE.equals(query.resigned());

        // offset = page * size. size 는 위에서 상한을 두지만 page 는 클 수 있어 int 곱셈이 넘칠 수 있다
        //   (예: page=1_500_000_000, size=2 → 음수 offset → 잘못된 조회/DB 오류). 넘치면 400 으로 막는다.
        int offset;
        try {
            offset = Math.multiplyExact(query.page(), query.size());
        } catch (ArithmeticException e) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_PARAMETER, e);
        }

        return new EmployeeListCriteria(
                normalize(query.keyword()),
                query.departmentId(),
                role,
                accountStatus,
                mustChangePassword,
                resignedOnly,
                offset,
                query.size(),
                currentCompanyIdProvider.currentCompanyId());
    }

    /** 앞뒤 공백 제거 후 빈 문자열은 null 로 눕힌다 (필터 미적용과 동일 취급). */
    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
