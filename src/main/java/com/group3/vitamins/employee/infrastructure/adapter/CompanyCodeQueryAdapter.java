package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.port.CompanyCodeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link CompanyCodeQueryPort} 의 MyBatis 어댑터. 실제 SQL 은 {@link CompanyCodeQueryMapper} 와 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class CompanyCodeQueryAdapter implements CompanyCodeQueryPort {

    private final CompanyCodeQueryMapper companyCodeQueryMapper;

    @Override
    public String findCodeByCompanyId(Long companyId) {
        return companyCodeQueryMapper.findCodeByCompanyId(companyId);
    }
}
