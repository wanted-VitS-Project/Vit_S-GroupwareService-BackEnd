package com.group3.vitamins.pagepermission.application.service;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.pagepermission.application.command.GrantPermissionsCommand;
import com.group3.vitamins.pagepermission.application.command.RevokePermissionCommand;
import com.group3.vitamins.pagepermission.application.policy.PagePermissionAdminPolicy;
import com.group3.vitamins.pagepermission.application.port.PagePermissionQueryPort;
import com.group3.vitamins.pagepermission.application.port.PagePermissionRepository;
import com.group3.vitamins.pagepermission.application.result.EmployeeRoleRow;
import com.group3.vitamins.pagepermission.application.result.GrantResult;
import com.group3.vitamins.pagepermission.application.result.RevokeResult;
import com.group3.vitamins.pagepermission.application.usecase.PagePermissionCommandUseCase;
import com.group3.vitamins.pagepermission.domain.exception.PagePermissionErrorCode;
import com.group3.vitamins.pagepermission.domain.model.PageAccessLevel;
import com.group3.vitamins.pagepermission.domain.model.PageAccessResolver;
import com.group3.vitamins.pagepermission.domain.model.PageAccessSource;
import com.group3.vitamins.pagepermission.domain.model.PageCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 페이지 권한 변경 (§4 부여/등급변경 · §5 회수). 둘 다 ADMIN. 부여 대상 사번은 <b>현재 회사</b>에 실재하고
 * 시스템 계정이 아니어야 한다 — 타사 사원에게 부여·회수할 수 없다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PagePermissionCommandService implements PagePermissionCommandUseCase {

    private final PagePermissionRepository pagePermissionRepository;
    private final PagePermissionQueryPort pagePermissionQueryPort;
    private final PagePermissionAdminPolicy adminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public GrantResult grant(GrantPermissionsCommand command) {
        adminPolicy.assertAdmin(command.requesterRole());
        PageCode page = PageCode.fromGrantableCode(command.pageCode()); // 부여 대상 아니면 PAGE_NOT_FOUND

        List<GrantPermissionsCommand.Item> items = command.permissions();
        if (items == null || items.isEmpty()) {
            throw new ValidationException(PagePermissionErrorCode.PAGE_INVALID_REQUEST);
        }
        // 사번 중복 금지 — 한 요청에 같은 사번이 두 번 오면 어느 등급이 이기는지 모호하다.
        Set<String> userIds = new LinkedHashSet<>();
        for (GrantPermissionsCommand.Item item : items) {
            if (item.userId() == null || !userIds.add(item.userId())) {
                throw new ValidationException(PagePermissionErrorCode.PAGE_INVALID_REQUEST);
            }
            if (!PageAccessLevel.isGrantable(item.permission())) { // VIEWER·EDITOR 만
                throw new ValidationException(PagePermissionErrorCode.PAGE_INVALID_PERMISSION);
            }
        }

        Long companyId = currentCompanyIdProvider.currentCompanyId();
        List<EmployeeRoleRow> refs = pagePermissionQueryPort.findEmployeeRoles(userIds, companyId);
        Set<String> found = new LinkedHashSet<>();
        for (EmployeeRoleRow ref : refs) {
            found.add(ref.userId());
            if (ref.isSystem()) { // ADMIN 등 시스템 계정은 대상이 될 수 없다
                throw new ForbiddenException(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED);
            }
        }
        if (!found.containsAll(userIds)) { // 없는/타사 사번이 섞이면 전체 거부
            throw new NotFoundException(EmployeeErrorCode.EMP_NOT_FOUND);
        }

        // 현재 등급과 대조해 신규/변경/무변화로 분류. 부여와 등급 변경이 같은 API 이므로 upsert 로 처리한다.
        Map<String, PageAccessLevel> existing = pagePermissionRepository.findLevels(page, userIds);
        int granted = 0;
        int updated = 0;
        int unchanged = 0;
        for (GrantPermissionsCommand.Item item : items) {
            PageAccessLevel level = PageAccessLevel.valueOf(item.permission());
            PageAccessLevel current = existing.get(item.userId());
            if (current == null) {
                pagePermissionRepository.grant(page, item.userId(), level);
                granted++;
            } else if (current == level) {
                unchanged++;
            } else {
                pagePermissionRepository.updateLevel(page, item.userId(), level);
                updated++;
            }
        }
        log.info("페이지 권한 부여 - page={} requested={} granted={} updated={} unchanged={}",
                page.name(), userIds.size(), granted, updated, unchanged);
        return new GrantResult(page.name(), userIds.size(), granted, updated, unchanged);
    }

    @Override
    public RevokeResult revoke(RevokePermissionCommand command) {
        adminPolicy.assertAdmin(command.requesterRole());
        PageCode page = PageCode.fromGrantableCode(command.pageCode());
        Long companyId = currentCompanyIdProvider.currentCompanyId();

        // 대상이 현재 회사 사원인지 확인(타사 사번 회수 차단) + 회수 후 판정을 위한 role 확보.
        List<EmployeeRoleRow> refs = pagePermissionQueryPort.findEmployeeRoles(List.of(command.userId()), companyId);
        if (refs.isEmpty()) {
            throw new NotFoundException(PagePermissionErrorCode.PAGE_PERMISSION_NOT_FOUND);
        }

        // 명시적 부여 기록만 회수한다. 0건이면 부여받은 적 없이 전역 권한으로만 보고 있던 것 → 404.
        if (pagePermissionRepository.revoke(page, command.userId()) == 0) {
            throw new NotFoundException(PagePermissionErrorCode.PAGE_PERMISSION_NOT_FOUND);
        }

        // ADMIN·MASTER 는 회수해도 전역 권한으로 열람이 계속된다(부여 기록만 사라진다).
        String role = refs.get(0).role();
        boolean stillAccessible = PageAccessResolver.ADMIN.equals(role) || PageAccessResolver.MASTER.equals(role);
        String accessSource = stillAccessible ? PageAccessSource.GLOBAL_ROLE.name() : null;
        log.info("페이지 권한 회수 - page={} userId={} stillAccessible={}", page.name(), command.userId(), stillAccessible);
        return new RevokeResult(page.name(), command.userId(), stillAccessible, accessSource);
    }
}
