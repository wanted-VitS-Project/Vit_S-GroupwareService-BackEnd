package com.group3.vitamins.pagepermission.infrastructure.adapter;

import com.group3.vitamins.pagepermission.application.port.PagePermissionQueryPort;
import com.group3.vitamins.pagepermission.application.result.EmployeeRoleRow;
import com.group3.vitamins.pagepermission.application.result.PageAccessMemberRow;
import com.group3.vitamins.pagepermission.application.result.PageGrantCountRow;
import com.group3.vitamins.pagepermission.application.result.PageLastGrantedDateRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    public Map<String, Long> countGrantsByPageCodes(Collection<String> pageCodes, Long companyId) {
        if (pageCodes == null || pageCodes.isEmpty()) {
            return Map.of();
        }
        return mapper.countGrantsByPageCodes(pageCodes, companyId).stream()
                .collect(Collectors.toMap(PageGrantCountRow::pageCode, PageGrantCountRow::grantedCount));
    }

    @Override
    public long countMasters(Long companyId) {
        return mapper.countMasters(companyId);
    }

    @Override
    public Map<String, LocalDate> findLastGrantedDatesByPageCodes(Collection<String> pageCodes, Long companyId) {
        if (pageCodes == null || pageCodes.isEmpty()) {
            return Map.of();
        }
        // GROUP BY 결과라 각 행의 lastGrantedDate 는 non-null → toMap NPE 없음.
        return mapper.findLastGrantedDatesByPageCodes(pageCodes, companyId).stream()
                .collect(Collectors.toMap(PageLastGrantedDateRow::pageCode, PageLastGrantedDateRow::lastGrantedDate));
    }

    @Override
    public List<EmployeeRoleRow> findEmployeeRoles(Collection<String> userIds, Long companyId) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return mapper.findEmployeeRoles(userIds, companyId);
    }
}
