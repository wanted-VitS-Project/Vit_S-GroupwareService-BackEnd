package com.group3.vitamins.project.infrastructure.adapter;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;


@Component
@RequiredArgsConstructor
public class EmployeeLookupAdapter implements EmployeeLookupPort {

    private final EmployeeLookupQueryMapper employeeLookupQueryMapper;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    /** 타사 사번이면 null 을 돌려준다 — 호출부는 "없는 사원"과 같게 처리한다. */
    @Override
    public String findNameByUserId(String userId) {
        return employeeLookupQueryMapper
                .findNameByUserId(userId, currentCompanyIdProvider.currentCompanyId())
                .orElse(null);
    }

    /** 타사 사번은 결과 맵에서 빠진다. */
    @Override
    public Map<String, EmployeeRef> findRefsByUserIds(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return employeeLookupQueryMapper
                .findRefsByUserIds(userIds, currentCompanyIdProvider.currentCompanyId()).stream()
                .collect(Collectors.toMap(
                        EmployeeNameRow::userId,
                        row -> new EmployeeRef(row.name(), row.deleted())));
    }
}
