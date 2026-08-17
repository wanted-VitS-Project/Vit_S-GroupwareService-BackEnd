package com.group3.vitamins.employee.application.service;

import com.group3.vitamins.employee.application.port.EmployeeSearchQueryPort;
import com.group3.vitamins.employee.application.query.EmployeeSearchQuery;
import com.group3.vitamins.employee.application.result.EmployeeSearchRow;
import com.group3.vitamins.employee.application.usecase.EmployeeQueryUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사원 조회 유스케이스 — 후보 검색(이름 또는 부서) (`.ai/api/employee.md` §9).
 *
 * <p>⛔ ADMIN 판정을 하지 않는다 — 이 검색은 결재선·참여자 지정용이라 <b>로그인 사용자 누구나</b> 쓴다.
 * 인증(세션 유무)은 Security 가 보장하고, 여기서는 검색 조건만 검증한다.
 */
@Service
@RequiredArgsConstructor
public class EmployeeQueryService implements EmployeeQueryUseCase {

    private final EmployeeSearchQueryPort employeeSearchQueryPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeSearchRow> search(EmployeeSearchQuery query) {
        // 이름·부서 둘 다 없으면 후보를 특정할 수 없다 → 400. query 자체 null 도 NPE(500) 대신 400 으로 막는다.
        if (query == null || (query.name() == null && query.departmentId() == null)) {
            throw new ValidationException(EmployeeErrorCode.EMP_INVALID_PARAMETER);
        }
        return employeeSearchQueryPort.search(query.name(), query.departmentId(),
                currentCompanyIdProvider.currentCompanyId());
    }
}
