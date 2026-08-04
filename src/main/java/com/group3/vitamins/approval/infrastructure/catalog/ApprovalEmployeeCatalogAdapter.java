package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.auth.infrastructure.persistence.AuthQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * {@code AuthQueryMapper}(계정+사원+부서+직급 조인)를 재사용하는 실제 구현체.
 * Block/Project 포트의 스텁과 다르게, Account/Auth 도메인이 이미 완성돼 있어 바로 연동한다.
 */
@Component
@RequiredArgsConstructor
public class ApprovalEmployeeCatalogAdapter implements EmployeeCatalogPort {

    private final AuthQueryMapper authQueryMapper;

    @Override
    public Optional<EmployeeSummary> findEmployee(String userId) {
        return authQueryMapper.findProfile(userId)
                .map(profile -> new EmployeeSummary(
                        profile.userId(), profile.name(), profile.jobPositionName(),
                        profile.departmentPath(), profile.role()));
    }
}
