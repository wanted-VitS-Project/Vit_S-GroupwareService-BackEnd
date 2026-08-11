package com.group3.vitamins.pagepermission.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * my/pages(§1) 반환 규칙의 <b>단일 판정처</b> (`.ai/api/page-permission.md`). 프론트는 표시 규칙을 갖지 않고
 * 이 결과만 그린다 — 판정이 서버 한 곳에 있어야 게이트를 바꿔도 어긋나지 않는다.
 *
 * <p>role 과 "부여된 등급 맵"(부여 대상 코드 → VIEWER/EDITOR)을 받아 카탈로그 순서대로 노출 항목을 만든다.
 * 미반환(ADMIN 의 프로젝트 화면, MEMBER 의 전사현황·관리자 등)은 결과에서 빠진다.
 */
public final class PageAccessResolver {

    public static final String ADMIN = "ADMIN";
    public static final String MASTER = "MASTER";

    private PageAccessResolver() {
    }

    /** my/pages 한 항목 — 노출되는 페이지의 코드·등급·근거. */
    public record Entry(PageCode pageCode, PageAccessLevel permission, PageAccessSource source) {
    }

    /**
     * 노출 항목 목록을 카탈로그(enum 선언) 순서로 만든다.
     *
     * @param role       전역 권한 (ADMIN·MASTER·MEMBER)
     * @param grantedByCode 부여 대상 코드 → 부여 등급(VIEWER/EDITOR). MEMBER 만 의미 있고, 없으면 빈 맵.
     */
    public static List<Entry> resolveMyPages(String role, Map<PageCode, PageAccessLevel> grantedByCode) {
        Map<PageCode, PageAccessLevel> granted = grantedByCode == null ? Map.of() : grantedByCode;
        boolean isAdmin = ADMIN.equals(role);
        boolean isMaster = MASTER.equals(role);

        List<Entry> entries = new ArrayList<>();
        for (PageCode page : PageCode.values()) {
            Entry entry = switch (page.category()) {
                case COMMON -> new Entry(page, PageAccessLevel.EDITOR, PageAccessSource.DEFAULT);
                case PROJECT -> isAdmin ? null : new Entry(page, PageAccessLevel.EDITOR, PageAccessSource.DEFAULT);
                case MASTER_GATED -> (isAdmin || isMaster)
                        ? new Entry(page, PageAccessLevel.EDITOR, PageAccessSource.GLOBAL_ROLE) : null;
                case ADMIN_ONLY -> isAdmin
                        ? new Entry(page, PageAccessLevel.EDITOR, PageAccessSource.ADMIN_ONLY) : null;
                case GRANTABLE -> resolveGrantable(page, isAdmin, isMaster, granted.get(page));
            };
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * BIDDING·FINANCE 판정. ADMIN·MASTER 는 부여 없이도 GLOBAL_ROLE 로 EDITOR. MEMBER 는 부여받은 등급이면 GRANTED,
     * 부여 전이면 <b>노출되되 접근 불가</b>(NONE·DEFAULT) — 기능의 존재를 알리고 관리자에게 요청할 경로를 남긴다.
     */
    private static Entry resolveGrantable(PageCode page, boolean isAdmin, boolean isMaster, PageAccessLevel granted) {
        if (isAdmin || isMaster) {
            return new Entry(page, PageAccessLevel.EDITOR, PageAccessSource.GLOBAL_ROLE);
        }
        if (granted != null) {
            return new Entry(page, granted, PageAccessSource.GRANTED);
        }
        return new Entry(page, PageAccessLevel.NONE, PageAccessSource.DEFAULT);
    }
}
