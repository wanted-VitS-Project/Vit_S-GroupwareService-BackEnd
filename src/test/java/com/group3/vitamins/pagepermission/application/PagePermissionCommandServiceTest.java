package com.group3.vitamins.pagepermission.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.pagepermission.application.command.GrantPermissionsCommand;
import com.group3.vitamins.pagepermission.application.command.RevokePermissionCommand;
import com.group3.vitamins.pagepermission.application.policy.PagePermissionAdminPolicy;
import com.group3.vitamins.pagepermission.application.port.PagePermissionQueryPort;
import com.group3.vitamins.pagepermission.application.port.PagePermissionRepository;
import com.group3.vitamins.pagepermission.application.result.EmployeeRoleRow;
import com.group3.vitamins.pagepermission.application.result.GrantResult;
import com.group3.vitamins.pagepermission.application.result.RevokeResult;
import com.group3.vitamins.pagepermission.application.service.PagePermissionCommandService;
import com.group3.vitamins.pagepermission.domain.exception.PagePermissionErrorCode;
import com.group3.vitamins.pagepermission.domain.model.PageAccessLevel;
import com.group3.vitamins.pagepermission.domain.model.PageCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PagePermissionCommandService 부여·회수")
class PagePermissionCommandServiceTest {

    private PagePermissionRepository repository;
    private PagePermissionQueryPort queryPort;
    private PagePermissionCommandService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PagePermissionRepository.class);
        queryPort = Mockito.mock(PagePermissionQueryPort.class);
        CurrentCompanyIdProvider provider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(provider.currentCompanyId()).thenReturn(1L);
        service = new PagePermissionCommandService(repository, queryPort, new PagePermissionAdminPolicy(), provider);
    }

    private GrantPermissionsCommand grant(String role, String page, GrantPermissionsCommand.Item... items) {
        return new GrantPermissionsCommand(role, page, List.of(items));
    }

    private GrantPermissionsCommand.Item item(String userId, String permission) {
        return new GrantPermissionsCommand.Item(userId, permission);
    }

    private void stubMembers(String... userIds) {
        List<EmployeeRoleRow> rows = java.util.Arrays.stream(userIds)
                .map(u -> new EmployeeRoleRow(u, "MEMBER", false)).toList();
        when(queryPort.findEmployeeRoles(anyCollection(), eq(1L))).thenReturn(rows);
    }

    // ---- 부여 ----

    @Test
    @DisplayName("신규·변경·무변화를 분류해 집계한다")
    void classifiesGrantUpdateUnchanged() {
        stubMembers("EMP1", "EMP2", "EMP3");
        // EMP1 없음(신규), EMP2 이미 VIEWER→EDITOR 변경, EMP3 이미 EDITOR 동일(무변화)
        when(repository.findLevels(eq(PageCode.BIDDING), anyCollection())).thenReturn(
                Map.of("EMP2", PageAccessLevel.VIEWER, "EMP3", PageAccessLevel.EDITOR));

        GrantResult r = service.grant(grant("ADMIN", "BIDDING",
                item("EMP1", "EDITOR"), item("EMP2", "EDITOR"), item("EMP3", "EDITOR")));

        assertThat(r.requestedCount()).isEqualTo(3);
        assertThat(r.grantedCount()).isEqualTo(1);
        assertThat(r.updatedCount()).isEqualTo(1);
        assertThat(r.unchangedCount()).isEqualTo(1);
        verify(repository).grant(PageCode.BIDDING, "EMP1", PageAccessLevel.EDITOR);
        verify(repository).updateLevel(PageCode.BIDDING, "EMP2", PageAccessLevel.EDITOR);
    }

    @Test
    @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED")
    void rejectsNonAdmin() {
        assertThatThrownBy(() -> service.grant(grant("MASTER", "BIDDING", item("EMP1", "EDITOR"))))
                .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
    }

    @Test
    @DisplayName("부여 대상이 아닌 페이지(HOME)는 PAGE_NOT_FOUND")
    void rejectsNonGrantablePage() {
        assertThatThrownBy(() -> service.grant(grant("ADMIN", "HOME", item("EMP1", "EDITOR"))))
                .satisfies(hasCode(PagePermissionErrorCode.PAGE_NOT_FOUND));
    }

    @Test
    @DisplayName("사번 중복이면 PAGE_INVALID_REQUEST")
    void rejectsDuplicateUserId() {
        assertThatThrownBy(() -> service.grant(grant("ADMIN", "BIDDING",
                item("EMP1", "EDITOR"), item("EMP1", "VIEWER"))))
                .satisfies(hasCode(PagePermissionErrorCode.PAGE_INVALID_REQUEST));
    }

    @Test
    @DisplayName("허용되지 않는 등급이면 PAGE_INVALID_PERMISSION")
    void rejectsInvalidPermission() {
        assertThatThrownBy(() -> service.grant(grant("ADMIN", "BIDDING", item("EMP1", "NONE"))))
                .satisfies(hasCode(PagePermissionErrorCode.PAGE_INVALID_PERMISSION));
    }

    @Test
    @DisplayName("공백 사번이면 PAGE_INVALID_REQUEST — EMP_NOT_FOUND(404)로 새지 않는다")
    void rejectsBlankUserId() {
        assertThatThrownBy(() -> service.grant(grant("ADMIN", "BIDDING", item("  ", "EDITOR"))))
                .satisfies(hasCode(PagePermissionErrorCode.PAGE_INVALID_REQUEST));
    }

    @Test
    @DisplayName("null 항목이 섞이면 PAGE_INVALID_REQUEST — NPE(500)가 아니다")
    void rejectsNullItem() {
        GrantPermissionsCommand command = new GrantPermissionsCommand("ADMIN", "BIDDING",
                java.util.Arrays.asList(item("EMP1", "EDITOR"), null));
        assertThatThrownBy(() -> service.grant(command))
                .satisfies(hasCode(PagePermissionErrorCode.PAGE_INVALID_REQUEST));
    }

    @Test
    @DisplayName("시스템 계정이 섞이면 ACC_SYSTEM_ACCOUNT_NOT_ALLOWED")
    void rejectsSystemAccount() {
        when(queryPort.findEmployeeRoles(anyCollection(), eq(1L)))
                .thenReturn(List.of(new EmployeeRoleRow("ADMIN01", "ADMIN", true)));

        assertThatThrownBy(() -> service.grant(grant("ADMIN", "BIDDING", item("ADMIN01", "EDITOR"))))
                .satisfies(hasCode(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED));
        verify(repository, never()).grant(any(), any(), any());
    }

    @Test
    @DisplayName("없는/타사 사번이 섞이면 EMP_NOT_FOUND — 전체 거부")
    void rejectsMissingUser() {
        when(queryPort.findEmployeeRoles(anyCollection(), eq(1L)))
                .thenReturn(List.of(new EmployeeRoleRow("EMP1", "MEMBER", false))); // EMP2 빠짐

        assertThatThrownBy(() -> service.grant(grant("ADMIN", "BIDDING",
                item("EMP1", "EDITOR"), item("EMP2", "EDITOR"))))
                .satisfies(hasCode(EmployeeErrorCode.EMP_NOT_FOUND));
        verify(repository, never()).grant(any(), any(), any());
    }

    // ---- 회수 ----

    @Test
    @DisplayName("MEMBER 회수 — stillAccessible=false")
    void revokeMember() {
        when(queryPort.findEmployeeRoles(anyCollection(), eq(1L)))
                .thenReturn(List.of(new EmployeeRoleRow("EMP1", "MEMBER", false)));
        when(repository.revoke(PageCode.FINANCE, "EMP1")).thenReturn(1);

        RevokeResult r = service.revoke(new RevokePermissionCommand("ADMIN", "FINANCE", "EMP1"));

        assertThat(r.stillAccessible()).isFalse();
        assertThat(r.accessSource()).isNull();
    }

    @Test
    @DisplayName("MASTER 회수 — 전역 권한으로 열람 계속(stillAccessible=true·GLOBAL_ROLE)")
    void revokeMaster() {
        when(queryPort.findEmployeeRoles(anyCollection(), eq(1L)))
                .thenReturn(List.of(new EmployeeRoleRow("EMP2", "MASTER", false)));
        when(repository.revoke(PageCode.FINANCE, "EMP2")).thenReturn(1);

        RevokeResult r = service.revoke(new RevokePermissionCommand("ADMIN", "FINANCE", "EMP2"));

        assertThat(r.stillAccessible()).isTrue();
        assertThat(r.accessSource()).isEqualTo("GLOBAL_ROLE");
    }

    @Test
    @DisplayName("부여 기록이 없으면 PAGE_PERMISSION_NOT_FOUND")
    void revokeNoGrant() {
        when(queryPort.findEmployeeRoles(anyCollection(), eq(1L)))
                .thenReturn(List.of(new EmployeeRoleRow("EMP1", "MEMBER", false)));
        when(repository.revoke(PageCode.FINANCE, "EMP1")).thenReturn(0);

        assertThatThrownBy(() -> service.revoke(new RevokePermissionCommand("ADMIN", "FINANCE", "EMP1")))
                .satisfies(hasCode(PagePermissionErrorCode.PAGE_PERMISSION_NOT_FOUND));
    }

    @Test
    @DisplayName("타사·없는 사번 회수 — PAGE_PERMISSION_NOT_FOUND (회사에 없음)")
    void revokeOtherCompany() {
        when(queryPort.findEmployeeRoles(anyCollection(), eq(1L))).thenReturn(List.of());

        assertThatThrownBy(() -> service.revoke(new RevokePermissionCommand("ADMIN", "FINANCE", "EMPX")))
                .satisfies(hasCode(PagePermissionErrorCode.PAGE_PERMISSION_NOT_FOUND));
        verify(repository, never()).revoke(any(), any());
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return t -> assertThat(t).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(expected);
    }
}
