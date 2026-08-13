package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.port.EmployeeAdminQueryPort;
import com.group3.vitamins.employee.application.query.EmployeeListCriteria;
import com.group3.vitamins.employee.application.result.EmployeeCertificateRow;
import com.group3.vitamins.employee.application.result.EmployeeDetailRow;
import com.group3.vitamins.employee.application.result.EmployeeEducationRow;
import com.group3.vitamins.employee.application.result.EmployeeGroupRow;
import com.group3.vitamins.employee.application.result.EmployeeListRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * {@link EmployeeAdminQueryPort} 의 MyBatis 어댑터. 실제 SQL 은 {@link EmployeeAdminQueryMapper} 와
 * 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class EmployeeAdminQueryAdapter implements EmployeeAdminQueryPort {

    private final EmployeeAdminQueryMapper employeeAdminQueryMapper;

    @Override
    public List<EmployeeListRow> findPage(EmployeeListCriteria criteria) {
        return employeeAdminQueryMapper.findPage(criteria);
    }

    @Override
    public long count(EmployeeListCriteria criteria) {
        return employeeAdminQueryMapper.count(criteria);
    }

    @Override
    public Optional<EmployeeDetailRow> findDetail(String userId, Long companyId) {
        return employeeAdminQueryMapper.findDetail(userId, companyId);
    }

    @Override
    public List<EmployeeGroupRow> findGroups(String userId) {
        return employeeAdminQueryMapper.findGroups(userId);
    }

    @Override
    public List<EmployeeEducationRow> findEducations(String userId) {
        return employeeAdminQueryMapper.findEducations(userId);
    }

    @Override
    public List<EmployeeCertificateRow> findCertificates(String userId) {
        return employeeAdminQueryMapper.findCertificates(userId);
    }
}
