package com.group3.vitamins.pagepermission.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.pagepermission.application.policy.PagePermissionAdminPolicy;
import com.group3.vitamins.pagepermission.application.port.PagePermissionQueryPort;
import com.group3.vitamins.pagepermission.application.port.PagePermissionRepository;
import com.group3.vitamins.pagepermission.application.result.MyPageResult;
import com.group3.vitamins.pagepermission.application.result.PageAccessListResult;
import com.group3.vitamins.pagepermission.application.result.PageAccessMemberResult;
import com.group3.vitamins.pagepermission.application.result.PageAccessMemberRow;
import com.group3.vitamins.pagepermission.application.result.PageListItemResult;
import com.group3.vitamins.pagepermission.application.service.PagePermissionQueryService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("PagePermissionQueryService 내 페이지·목록·접근자")
class PagePermissionQueryServiceTest {

    private PagePermissionRepository repository;
    private PagePermissionQueryPort queryPort;
    private PagePermissionQueryService service;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(PagePermissionRepository.class);
        queryPort = Mockito.mock(PagePermissionQueryPort.class);
        CurrentCompanyIdProvider provider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(provider.currentCompanyId()).thenReturn(1L);
        service = new PagePermissionQueryService(repository, queryPort, new PagePermissionAdminPolicy(), provider);
    }

    @Test
    @DisplayName("§1 MEMBER — 부여 없으면 BIDDING·FINANCE 가 NONE·DEFAULT 로 내려간다")
    void myPagesMemberNoGrant() {
        when(repository.findGrantedLevels("EMP1")).thenReturn(Map.of());

        List<MyPageResult> result = service.getMyPages("EMP1", "MEMBER");

        MyPageResult bidding = result.stream().filter(r -> r.pageCode().equals("BIDDING")).findFirst().orElseThrow();
        assertThat(bidding.permission()).isEqualTo("NONE");
        assertThat(bidding.source()).isEqualTo("DEFAULT");
        assertThat(result).extracting(MyPageResult::pageCode)
                .doesNotContain("COMPANY_STATUS", "TEMPLATE", "ADMIN_CONSOLE"); // MEMBER 미반환
    }

    @Test
    @DisplayName("§2 부여 가능한 2개만, accessCount = granted + globalRole")
    void listPagesCounts() {
        when(queryPort.countMasters(1L)).thenReturn(2L);
        // BIDDING=3 만 부여, FINANCE 는 맵에서 빠짐(→ 0). 날짜는 부여 기록 없어 빈 맵(→ null).
        when(queryPort.countGrantsByPageCodes(Mockito.anyCollection(), eq(1L)))
                .thenReturn(Map.of("BIDDING", 3L));
        when(queryPort.findLastGrantedDatesByPageCodes(Mockito.anyCollection(), eq(1L)))
                .thenReturn(Map.of());

        List<PageListItemResult> result = service.listPages("ADMIN");

        assertThat(result).extracting(PageListItemResult::pageCode).containsExactly("BIDDING", "FINANCE");
        PageListItemResult bidding = result.get(0);
        assertThat(bidding.grantedCount()).isEqualTo(3);
        assertThat(bidding.globalRoleCount()).isEqualTo(2);
        assertThat(bidding.accessCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("§2 ADMIN 아니면 ACC_ADMIN_REQUIRED")
    void listPagesRejectsNonAdmin() {
        assertThatThrownBy(() -> service.listPages("MEMBER"))
                .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
    }

    @Test
    @DisplayName("§3 명단 — 부여자(GRANTED·회수가능) 먼저, 그다음 MASTER(GLOBAL_ROLE·회수불가·EDITOR)")
    void pageAccessAssembly() {
        when(queryPort.findGrantedMembers("FINANCE", 1L)).thenReturn(List.of(
                new PageAccessMemberRow("EMP1", "김철수", "개발팀", "기술본부", "대리", "MEMBER", "VIEWER")));
        when(queryPort.findMasterMembers(1L)).thenReturn(List.of(
                new PageAccessMemberRow("EMP9", "박마스터", null, null, null, "MASTER", null)));

        PageAccessListResult r = service.getPageAccess("ADMIN", "FINANCE");

        assertThat(r.grantedCount()).isEqualTo(1);
        assertThat(r.globalRoleCount()).isEqualTo(1);
        PageAccessMemberResult granted = r.content().get(0);
        assertThat(granted.source()).isEqualTo("GRANTED");
        assertThat(granted.revocable()).isTrue();
        assertThat(granted.departmentPath()).isEqualTo("기술본부 / 개발팀");
        PageAccessMemberResult master = r.content().get(1);
        assertThat(master.source()).isEqualTo("GLOBAL_ROLE");
        assertThat(master.revocable()).isFalse();
        assertThat(master.permission()).isEqualTo(PageAccessLevel.EDITOR.name());
    }

    @Test
    @DisplayName("§3 부여 대상 아닌 페이지(SETTINGS)는 PAGE_NOT_FOUND")
    void pageAccessRejectsNonGrantable() {
        assertThatThrownBy(() -> service.getPageAccess("ADMIN", "SETTINGS"))
                .satisfies(hasCode(PagePermissionErrorCode.PAGE_NOT_FOUND));
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return t -> assertThat(t).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(expected);
    }
}
