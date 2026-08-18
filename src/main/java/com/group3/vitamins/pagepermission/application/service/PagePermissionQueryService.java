package com.group3.vitamins.pagepermission.application.service;

import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.pagepermission.application.policy.PagePermissionAdminPolicy;
import com.group3.vitamins.pagepermission.application.port.PagePermissionQueryPort;
import com.group3.vitamins.pagepermission.application.port.PagePermissionRepository;
import com.group3.vitamins.pagepermission.application.result.MyPageResult;
import com.group3.vitamins.pagepermission.application.result.PageAccessListResult;
import com.group3.vitamins.pagepermission.application.result.PageAccessMemberResult;
import com.group3.vitamins.pagepermission.application.result.PageAccessMemberRow;
import com.group3.vitamins.pagepermission.application.result.PageListItemResult;
import com.group3.vitamins.pagepermission.application.usecase.PagePermissionQueryUseCase;
import com.group3.vitamins.pagepermission.domain.model.PageAccessLevel;
import com.group3.vitamins.pagepermission.domain.model.PageAccessResolver;
import com.group3.vitamins.pagepermission.domain.model.PageAccessSource;
import com.group3.vitamins.pagepermission.domain.model.PageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 페이지 권한 조회 (§1 내 페이지 · §2 페이지 목록 · §3 접근 가능자). §1 은 전체 사용자, §2·§3 은 ADMIN.
 * §2·§3 의 명단·집계는 회사 범위로 격리한다({@link PagePermissionQueryPort}).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PagePermissionQueryService implements PagePermissionQueryUseCase {

    private final PagePermissionRepository pagePermissionRepository;
    private final PagePermissionQueryPort pagePermissionQueryPort;
    private final PagePermissionAdminPolicy adminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;

    @Override
    public List<MyPageResult> getMyPages(String userId, String role) {
        // 본인의 부여 등급(BIDDING·FINANCE)만 로드해 판정에 넣는다 — 판정은 도메인 리졸버 한 곳에서.
        Map<PageCode, PageAccessLevel> granted = pagePermissionRepository.findGrantedLevels(userId);
        return PageAccessResolver.resolveMyPages(role, granted).stream()
                .map(e -> new MyPageResult(
                        e.pageCode().name(), e.pageCode().displayName(),
                        e.permission().name(), e.source().name()))
                .toList();
    }

    @Override
    public List<PageListItemResult> listPages(String requesterRole) {
        adminPolicy.assertAdmin(requesterRole);
        Long companyId = currentCompanyIdProvider.currentCompanyId();

        // 부여 가능한 페이지(BIDDING·FINANCE)만 부여 화면 목록에 뜬다. role 로 여는 페이지는 부여 대상이 아니라 제외.
        List<String> grantableCodes = Arrays.stream(PageCode.values())
                .filter(PageCode::isGrantable)
                .map(PageCode::name)
                .toList();

        // 집계를 페이지마다 왕복(페이지당 2쿼리)하지 않고 한 번에 받는다 — page_code 별 GROUP BY 2쿼리로 고정.
        long globalRoleCount = pagePermissionQueryPort.countMasters(companyId);
        Map<String, Long> grantedCounts = pagePermissionQueryPort.countGrantsByPageCodes(grantableCodes, companyId);
        Map<String, LocalDate> lastGrantedDates =
                pagePermissionQueryPort.findLastGrantedDatesByPageCodes(grantableCodes, companyId);

        List<PageListItemResult> items = new ArrayList<>();
        for (PageCode page : PageCode.values()) {
            if (!page.isGrantable()) {
                continue;
            }
            long grantedCount = grantedCounts.getOrDefault(page.name(), 0L);
            LocalDate lastModifiedAt = lastGrantedDates.get(page.name());
            items.add(new PageListItemResult(
                    page.name(), page.displayName(), page.description(),
                    (int) (grantedCount + globalRoleCount), (int) grantedCount, (int) globalRoleCount,
                    lastModifiedAt));
        }
        return items;
    }

    @Override
    public PageAccessListResult getPageAccess(String requesterRole, String pageCode) {
        adminPolicy.assertAdmin(requesterRole);
        PageCode page = PageCode.fromGrantableCode(pageCode); // 부여 대상 아니면 PAGE_NOT_FOUND
        Long companyId = currentCompanyIdProvider.currentCompanyId();

        // 명시 부여자(MEMBER·GRANTED·회수 가능) 먼저, 그다음 전역 권한(MASTER·GLOBAL_ROLE·회수 불가). 각 이름 오름차순.
        List<PageAccessMemberResult> granted = pagePermissionQueryPort.findGrantedMembers(page.name(), companyId).stream()
                .map(r -> toMember(r, r.permission(), PageAccessSource.GRANTED, true))
                .toList();
        List<PageAccessMemberResult> masters = pagePermissionQueryPort.findMasterMembers(companyId).stream()
                .map(r -> toMember(r, PageAccessLevel.EDITOR.name(), PageAccessSource.GLOBAL_ROLE, false))
                .toList();

        List<PageAccessMemberResult> content = new ArrayList<>(granted.size() + masters.size());
        content.addAll(granted);
        content.addAll(masters);

        return new PageAccessListResult(
                page.name(), page.displayName(), content, granted.size(), masters.size());
    }

    private PageAccessMemberResult toMember(PageAccessMemberRow r, String permission,
                                            PageAccessSource source, boolean revocable) {
        return new PageAccessMemberResult(
                r.userId(), r.name(), departmentPath(r), r.jobPositionName(),
                r.role(), permission, source.name(), revocable);
    }

    /** "상위부서 / 부서" 조립. 부서 미배정이면 null, 최상위 부서면 부서명만. */
    private String departmentPath(PageAccessMemberRow r) {
        if (r.departmentName() == null) {
            return null;
        }
        return r.parentDepartmentName() == null
                ? r.departmentName()
                : r.parentDepartmentName() + " / " + r.departmentName();
    }
}
