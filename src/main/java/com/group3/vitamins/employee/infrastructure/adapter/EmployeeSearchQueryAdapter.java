package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.port.EmployeeSearchQueryPort;
import com.group3.vitamins.employee.application.result.EmployeeSearchRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link EmployeeSearchQueryPort} 의 MyBatis 어댑터. 실제 SQL 은
 * {@link EmployeeSearchQueryMapper} 와 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class EmployeeSearchQueryAdapter implements EmployeeSearchQueryPort {

    private final EmployeeSearchQueryMapper employeeSearchQueryMapper;

    @Override
    public List<EmployeeSearchRow> searchByName(String name) {
        return employeeSearchQueryMapper.searchByName(name);
    }
}
