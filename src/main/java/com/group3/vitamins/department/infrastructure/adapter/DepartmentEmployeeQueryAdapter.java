package com.group3.vitamins.department.infrastructure.adapter;

import com.group3.vitamins.department.application.port.DepartmentEmployeeQueryPort;
import com.group3.vitamins.department.application.result.DepartmentEmployeeCountRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link DepartmentEmployeeQueryPort} 의 MyBatis 어댑터. 실제 SQL 은
 * {@link DepartmentEmployeeQueryMapper} 와 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class DepartmentEmployeeQueryAdapter implements DepartmentEmployeeQueryPort {

    private final DepartmentEmployeeQueryMapper departmentEmployeeQueryMapper;

    @Override
    public List<DepartmentEmployeeCountRow> findAllWithDirectEmployeeCount(Long companyId) {
        return departmentEmployeeQueryMapper.findAllWithDirectEmployeeCount(companyId);
    }

    @Override
    public long countDirectEmployees(Long departmentId) {
        return departmentEmployeeQueryMapper.countDirectEmployees(departmentId);
    }
}
