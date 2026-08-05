package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.port.EmployeeReferenceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link EmployeeReferenceQueryPort} 의 MyBatis 어댑터. 실제 SQL 은 {@link EmployeeReferenceQueryMapper} 와
 * 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class EmployeeReferenceQueryAdapter implements EmployeeReferenceQueryPort {

    private final EmployeeReferenceQueryMapper employeeReferenceQueryMapper;

    @Override
    public boolean departmentExists(Long departmentId) {
        return employeeReferenceQueryMapper.departmentExists(departmentId);
    }

    @Override
    public boolean jobPositionExists(Long jobPositionId) {
        return employeeReferenceQueryMapper.jobPositionExists(jobPositionId);
    }
}
