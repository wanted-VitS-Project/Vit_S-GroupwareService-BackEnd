package com.group3.vitamins.pagepermission.application.usecase;

import com.group3.vitamins.pagepermission.application.result.MyPageResult;
import com.group3.vitamins.pagepermission.application.result.PageAccessListResult;
import com.group3.vitamins.pagepermission.application.result.PageListItemResult;

import java.util.List;

/** 페이지 권한 조회 인바운드 포트 (§1 내 페이지 · §2 페이지 목록 · §3 접근 가능자). */
public interface PagePermissionQueryUseCase {

    /** §1 내 페이지 목록 — 전체 사용자. 노출되는 페이지를 permission·source 와 함께 카탈로그 순서로. */
    List<MyPageResult> getMyPages(String userId, String role);

    /** §2 페이지 목록 — ADMIN. 부여 가능한 페이지(BIDDING·FINANCE)와 접근 인원 집계. */
    List<PageListItemResult> listPages(String requesterRole);

    /** §3 페이지 접근 가능자 목록 — ADMIN. 명시 부여자 + 전역 권한(MASTER). */
    PageAccessListResult getPageAccess(String requesterRole, String pageCode);
}
