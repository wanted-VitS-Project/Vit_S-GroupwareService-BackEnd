package com.group3.vitamins.pagepermission.application.port;

import com.group3.vitamins.pagepermission.application.result.EmployeeRoleRow;
import com.group3.vitamins.pagepermission.application.result.PageAccessMemberRow;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 페이지 권한 화면용 조회 아웃바운드 포트 (MyBatis). {@code page_permission} 과 {@code employee}·{@code account} 를
 * 가로지르는 명단·집계·검증을 담당한다. <b>모든 조회는 회사 범위</b>(employee.company_id)로 격리한다 — 타사 사원이
 * 명단·집계·검증에 섞이지 않는다.
 */
public interface PagePermissionQueryPort {

    /** §3 명시 부여 명단 — page_permission ⋈ employee(비시스템·회사). permission = 부여 등급. */
    List<PageAccessMemberRow> findGrantedMembers(String pageCode, Long companyId);

    /**
     * §3 전역 권한 명단 = <b>MASTER 사원</b>(비시스템·회사). ADMIN 은 시스템 계정이라 사람 목록에서 제외한다(EMP-003).
     * permission 은 null(→ 서비스가 EDITOR), source=GLOBAL_ROLE, revocable=false.
     */
    List<PageAccessMemberRow> findMasterMembers(Long companyId);

    /**
     * §2 페이지별 부여 인원 수 — 여러 페이지를 한 번에 집계한다(page_code → 인원 수). 회사 MEMBER 기준.
     * 부여 0 인 페이지는 맵에서 빠진다(→ 호출부가 0). 빈 요청이면 빈 맵.
     */
    Map<String, Long> countGrantsByPageCodes(Collection<String> pageCodes, Long companyId);

    /** §2·§3 전역 권한 인원 = MASTER 사원 수(비시스템·회사). */
    long countMasters(Long companyId);

    /**
     * §2 페이지별 마지막 부여 수정일 — 여러 페이지를 한 번에 조회한다(page_code → yyyy-MM-dd). 회사 사원 기준.
     * 부여 기록 없는 페이지는 맵에서 빠진다(→ 호출부가 null). 빈 요청이면 빈 맵.
     */
    Map<String, LocalDate> findLastGrantedDatesByPageCodes(Collection<String> pageCodes, Long companyId);

    /** §4 부여 검증·§5 회수 후 판정 — 요청 사번의 (사번·role·isSystem). 없는/타사 사번은 결과에서 빠진다. */
    List<EmployeeRoleRow> findEmployeeRoles(Collection<String> userIds, Long companyId);
}
