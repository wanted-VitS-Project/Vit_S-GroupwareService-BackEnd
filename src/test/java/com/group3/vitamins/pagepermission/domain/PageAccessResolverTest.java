package com.group3.vitamins.pagepermission.domain;

import com.group3.vitamins.pagepermission.domain.model.PageAccessLevel;
import com.group3.vitamins.pagepermission.domain.model.PageAccessResolver;
import com.group3.vitamins.pagepermission.domain.model.PageAccessSource;
import com.group3.vitamins.pagepermission.domain.model.PageCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PageAccessResolver — my/pages 권한 매트릭스(§1)")
class PageAccessResolverTest {

    private Map<PageCode, PageAccessResolver.Entry> resolve(String role, Map<PageCode, PageAccessLevel> granted) {
        return PageAccessResolver.resolveMyPages(role, granted).stream()
                .collect(Collectors.toMap(PageAccessResolver.Entry::pageCode, e -> e));
    }

    @Test
    @DisplayName("ADMIN — 프로젝트 화면 2개만 미반환, 나머지 9개 반환(관리자·템플릿은 ADMIN_ONLY)")
    void admin() {
        Map<PageCode, PageAccessResolver.Entry> m = resolve("ADMIN", Map.of());

        assertThat(m.keySet()).containsExactlyInAnyOrder(
                PageCode.HOME, PageCode.NOTIFICATION, PageCode.APPROVAL, PageCode.BIDDING, PageCode.FINANCE,
                PageCode.COMPANY_STATUS, PageCode.TEMPLATE, PageCode.ADMIN_CONSOLE, PageCode.SETTINGS);
        // 부여 없이도 BIDDING·FINANCE 는 전역 권한으로 EDITOR
        assertThat(m.get(PageCode.BIDDING).permission()).isEqualTo(PageAccessLevel.EDITOR);
        assertThat(m.get(PageCode.BIDDING).source()).isEqualTo(PageAccessSource.GLOBAL_ROLE);
        assertThat(m.get(PageCode.TEMPLATE).source()).isEqualTo(PageAccessSource.ADMIN_ONLY);
        assertThat(m.get(PageCode.ADMIN_CONSOLE).source()).isEqualTo(PageAccessSource.ADMIN_ONLY);
        assertThat(m).doesNotContainKeys(PageCode.PROJECT_CREATE, PageCode.MY_PROJECT);
    }

    @Test
    @DisplayName("MASTER — 관리자·템플릿(ADMIN_ONLY)만 미반환. 전사현황·BIDDING·FINANCE 는 GLOBAL_ROLE")
    void master() {
        Map<PageCode, PageAccessResolver.Entry> m = resolve("MASTER", Map.of());

        assertThat(m).doesNotContainKeys(PageCode.TEMPLATE, PageCode.ADMIN_CONSOLE);
        assertThat(m.get(PageCode.COMPANY_STATUS).source()).isEqualTo(PageAccessSource.GLOBAL_ROLE);
        assertThat(m.get(PageCode.PROJECT_CREATE).source()).isEqualTo(PageAccessSource.DEFAULT);
        assertThat(m.get(PageCode.FINANCE).permission()).isEqualTo(PageAccessLevel.EDITOR);
        assertThat(m.get(PageCode.FINANCE).source()).isEqualTo(PageAccessSource.GLOBAL_ROLE);
    }

    @Test
    @DisplayName("MEMBER · 부여 없음 — 8개(전사현황·템플릿·관리자 미반환). BIDDING·FINANCE 는 NONE·DEFAULT")
    void memberNoGrant() {
        Map<PageCode, PageAccessResolver.Entry> m = resolve("MEMBER", Map.of());

        assertThat(m.keySet()).containsExactlyInAnyOrder(
                PageCode.HOME, PageCode.NOTIFICATION, PageCode.APPROVAL,
                PageCode.PROJECT_CREATE, PageCode.MY_PROJECT, PageCode.SETTINGS,
                PageCode.BIDDING, PageCode.FINANCE);
        assertThat(m.get(PageCode.BIDDING).permission()).isEqualTo(PageAccessLevel.NONE);
        assertThat(m.get(PageCode.BIDDING).source()).isEqualTo(PageAccessSource.DEFAULT);
        assertThat(m.get(PageCode.FINANCE).permission()).isEqualTo(PageAccessLevel.NONE);
    }

    @Test
    @DisplayName("MEMBER · BIDDING VIEWER 부여 — 그 페이지만 VIEWER·GRANTED, FINANCE 는 여전히 NONE")
    void memberGranted() {
        Map<PageCode, PageAccessResolver.Entry> m =
                resolve("MEMBER", Map.of(PageCode.BIDDING, PageAccessLevel.VIEWER));

        assertThat(m.get(PageCode.BIDDING).permission()).isEqualTo(PageAccessLevel.VIEWER);
        assertThat(m.get(PageCode.BIDDING).source()).isEqualTo(PageAccessSource.GRANTED);
        assertThat(m.get(PageCode.FINANCE).permission()).isEqualTo(PageAccessLevel.NONE);
    }

    @Test
    @DisplayName("grantedByCode 가 null 이어도 NPE 없이 처리한다(빈 맵 취급)")
    void nullGrantedMap() {
        List<PageAccessResolver.Entry> entries = PageAccessResolver.resolveMyPages("MEMBER", null);
        assertThat(entries).isNotEmpty(); // 500 대신 정상 반환
    }

    @Test
    @DisplayName("카탈로그 순서(상단바·사이드바)로 반환한다")
    void keepsCatalogOrder() {
        List<PageAccessResolver.Entry> entries = PageAccessResolver.resolveMyPages("MEMBER", Map.of());
        assertThat(entries).extracting(e -> e.pageCode().name())
                .startsWith("HOME", "NOTIFICATION", "APPROVAL", "BIDDING");
    }
}
