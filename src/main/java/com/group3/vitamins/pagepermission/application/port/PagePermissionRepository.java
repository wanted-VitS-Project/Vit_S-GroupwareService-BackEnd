package com.group3.vitamins.pagepermission.application.port;

import com.group3.vitamins.pagepermission.domain.model.PageAccessLevel;
import com.group3.vitamins.pagepermission.domain.model.PageCode;

import java.util.Collection;
import java.util.Map;

/**
 * 페이지 권한 쓰기·단건 조회 아웃바운드 포트 (JPA). {@code page_permission} 은 부여 대상(BIDDING·FINANCE) 행만 갖는다.
 * 명단·집계처럼 {@code employee} 를 가로지르는 조회는 {@link PagePermissionQueryPort}(MyBatis)가 맡는다.
 */
public interface PagePermissionRepository {

    /** 사용자에게 부여된 (페이지 → 등급). my/pages 판정용 — BIDDING·FINANCE 행만 존재한다. */
    Map<PageCode, PageAccessLevel> findGrantedLevels(String userId);

    /** 한 페이지에 대해 요청 사번들의 현재 부여 등급. 부여 없는 사번은 맵에서 빠진다(부여/변경/무변화 분류용). */
    Map<String, PageAccessLevel> findLevels(PageCode pageCode, Collection<String> userIds);

    /** 신규 부여(행 삽입). */
    void grant(PageCode pageCode, String userId, PageAccessLevel level);

    /** 등급 변경(기존 행 갱신). */
    void updateLevel(PageCode pageCode, String userId, PageAccessLevel level);

    /** 회수 — 삭제된 행 수(0 또는 1). 0 이면 부여 기록이 없던 것. */
    int revoke(PageCode pageCode, String userId);
}
