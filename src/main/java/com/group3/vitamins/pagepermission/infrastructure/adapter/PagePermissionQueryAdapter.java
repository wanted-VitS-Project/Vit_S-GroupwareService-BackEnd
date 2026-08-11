package com.group3.vitamins.pagepermission.infrastructure.adapter;

import com.group3.vitamins.pagepermission.application.port.PagePermissionQueryPort;
import com.group3.vitamins.pagepermission.application.result.EmployeeRoleRow;
import com.group3.vitamins.pagepermission.application.result.PageAccessMemberRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * {@link PagePermissionQueryPort} 의 MyBatis 어댑터. 실제 SQL 은 {@link PagePermissionQueryMapper} 와 그 XML 이 갖는다.
 */
@Component
@RequiredArgsConstructor
public class PagePermissionQueryAdapter implements PagePermissionQueryPort {

    private final PagePermissionQueryMapper mapper;

    @Override
    public List<PageAccessMemberRow> findGrantedMembers(String pageCode, Long companyId) {
        return mapper.findGrantedMembers(pageCode, companyId);
    }

    @Override
    public List<PageAccessMemberRow> findMasterMembers(Long companyId) {
        return mapper.findMasterMembers(companyId);
    }

    @Override
    public long countGrants(String pageCode, Long companyId) {
        return mapper.countGrants(pageCode, companyId);
    }

    @Override
    public long countMasters(Long companyId) {
        return mapper.countMasters(companyId);
    }

    @Override
    public LocalDate findLastGrantedDate(String pageCode, Long companyId) {
        return mapper.findLastGrantedDate(pageCode, companyId);
    }

    @Override
    public List<EmployeeRoleRow> findEmployeeRoles(Collection<String> userIds, Long companyId) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return mapper.findEmployeeRoles(userIds, companyId);
    }
}
