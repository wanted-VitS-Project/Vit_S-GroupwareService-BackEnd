package com.group3.vitamins.pagepermission.domain.model;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.pagepermission.domain.exception.PagePermissionErrorCode;

import java.util.Arrays;

/**
 * 페이지 고정 카탈로그 (`.ai/api/page-permission.md` — 11코드, 2026-08-10 확장).
 *
 * <p>페이지는 개발자가 코드로 제공하는 고정 목록이다(DB 테이블 없음). {@code page_permission} 행은
 * <b>부여 대상({@link Category#GRANTABLE}) 코드에만</b> 생긴다 — {@code BIDDING}·{@code FINANCE} 2개뿐.
 * 나머지 9개는 role 로 열려 행이 생기지 않는다.
 *
 * <p>{@link Category} 가 my/pages 반환 규칙을 결정한다 — 판정은 {@code PageAccessResolver} 가 한다.
 */
public enum PageCode {

    HOME("홈", Category.COMMON, null),
    NOTIFICATION("알림", Category.COMMON, null),
    APPROVAL("결재관리", Category.COMMON, null),
    BIDDING("공고 조회 · 입찰 관리", Category.GRANTABLE, "공고 조회와 입찰 관리 화면 접근 권한"),
    PROJECT_CREATE("프로젝트 생성", Category.PROJECT, null),
    MY_PROJECT("내 프로젝트", Category.PROJECT, null),
    FINANCE("재무 관리", Category.GRANTABLE, "재무 관리 화면 접근 권한"),
    COMPANY_STATUS("전사현황", Category.MASTER_GATED, null),
    TEMPLATE("템플릿 관리", Category.ADMIN_ONLY, null),
    ADMIN_CONSOLE("관리자", Category.ADMIN_ONLY, null),
    SETTINGS("설정", Category.COMMON, null);

    /** my/pages 반환 규칙을 결정하는 분류. */
    public enum Category {
        /** 전원에게 EDITOR·DEFAULT 로 노출 (HOME·NOTIFICATION·APPROVAL·SETTINGS). */
        COMMON,
        /** ADMIN 은 미반환, 나머지는 EDITOR·DEFAULT ("내 것이 생기는 화면" — PROJECT_CREATE·MY_PROJECT). */
        PROJECT,
        /** 전원 노출. ADMIN·MASTER 는 GLOBAL_ROLE, MEMBER 는 부여 시 GRANTED·미부여 시 NONE·DEFAULT (BIDDING·FINANCE). */
        GRANTABLE,
        /** ADMIN·MASTER 만 EDITOR·GLOBAL_ROLE, MEMBER 미반환 (COMPANY_STATUS — MASTER 전용 화면). */
        MASTER_GATED,
        /** ADMIN 만 EDITOR·ADMIN_ONLY, 나머지 미반환 (TEMPLATE·ADMIN_CONSOLE). */
        ADMIN_ONLY
    }

    private final String displayName;
    private final Category category;
    private final String description;

    PageCode(String displayName, Category category, String description) {
        this.displayName = displayName;
        this.category = category;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public Category category() {
        return category;
    }

    /** §2 페이지 목록의 설명. 부여 대상(GRANTABLE)만 값이 있고 나머지는 {@code null}. */
    public String description() {
        return description;
    }

    /** 부여 대상 페이지인가 (page_permission 행이 생기는 코드 = BIDDING·FINANCE). */
    public boolean isGrantable() {
        return category == Category.GRANTABLE;
    }

    /**
     * 코드 문자열 → {@link PageCode}. 카탈로그에 없으면 {@code PAGE_NOT_FOUND}(404).
     * my/pages 처럼 전 카탈로그를 다룰 때 쓴다.
     */
    public static PageCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(p -> p.name().equals(code))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(PagePermissionErrorCode.PAGE_NOT_FOUND));
    }

    /**
     * <b>부여 대상</b> 코드 문자열 → {@link PageCode}. 부여 화면(§3·§4·§5)은 부여 가능한 페이지만 다루므로,
     * 카탈로그에 없거나 부여 대상이 아닌 코드(예: HOME)는 모두 {@code PAGE_NOT_FOUND}(404)로 막는다.
     */
    public static PageCode fromGrantableCode(String code) {
        PageCode page = fromCode(code);
        if (!page.isGrantable()) {
            throw new NotFoundException(PagePermissionErrorCode.PAGE_NOT_FOUND);
        }
        return page;
    }
}
