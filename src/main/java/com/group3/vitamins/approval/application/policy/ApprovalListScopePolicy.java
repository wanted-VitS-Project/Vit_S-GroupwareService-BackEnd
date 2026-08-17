package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 결재관리 목록조회(MGT-001~004) — 요청 {@code scope} 를 요청자 role 로 해석하고 전체 조회 권한을 검증한다.
 *
 * <p>2026-08-17 계약 변경 — {@code ADMIN} 의 결재 <b>조회</b> 차단을 해제했다. 인사 담당이 회사의 결재
 * 현황을 봐야 한다는 요구가 실제로 있었고, 조회 권한은 쓰기 권한과 분리되어 있어 열어도 기안·상신·결재
 * 처리 경로는 그대로 막힌다({@code ApprovalBlockCatalogAdapter.isStepEditor} 는 손대지 않았다).
 */
@Component
@RequiredArgsConstructor
public class ApprovalListScopePolicy {

    /**
     * 회사 전체 결재를 조회할 수 있는 role.
     *
     * <p>⚠️ <b>조회 전용 목록이다.</b> 쓰기 판정(스텝 EDITOR 취급·결재자 지정 시 project member 검증 면제)
     * 에는 {@code MASTER} 만 들어간다 — 여기에 role 을 추가해도 쓰기는 열리지 않는다. 두 목록을 하나로
     * 합치지 마라.
     */
    private static final Set<String> FULL_ACCESS_ROLES = Set.of("MASTER", "ADMIN");

    private static final String SCOPE_ALL = "all";
    private static final String SCOPE_PENDING = "pending";
    private static final String ROLE_ADMIN = "ADMIN";

    private final EmployeeCatalogPort employeeCatalogPort;

    /**
     * 요청 {@code scope} 를 실제로 적용할 {@code scope} 로 해석한다. role 조회는 여기서 <b>한 번</b>만 한다.
     *
     * <table>
     *   <tr><th>요청</th><th>MASTER</th><th>ADMIN</th><th>그 외</th></tr>
     *   <tr><td>{@code all}</td><td>all</td><td>all</td><td>403</td></tr>
     *   <tr><td>{@code pending}</td><td>pending</td><td>pending</td><td>pending</td></tr>
     *   <tr><td>{@code drafted}(기본)</td><td>drafted</td><td><b>all</b></td><td>drafted</td></tr>
     * </table>
     *
     * <p>⚠️ <b>{@code ADMIN} 의 기본 {@code scope} 를 {@code all} 로 올리는 것이 이 변경의 핵심이다.</b>
     * 403 만 풀면 ADMIN 은 여전히 <b>빈 목록</b>을 본다 — {@code drafted} 는 {@code a.user_id = 요청자} 로
     * 자르는데 ADMIN 은 기안자가 될 수 없어 구조적으로 0건이기 때문이다. 예외도 로그도 안 남고 화면에는
     * "결재가 없다"로만 보인다. 프론트가 ADMIN 일 때 {@code scope=all} 을 따로 보내지 않아도 되게 여기서 해석한다.
     *
     * <p>{@code pending}(내가 처리할 결재)은 승격하지 않는다 — ADMIN 은 결재자로 지정될 수 없어 0건이
     * 정답이다. 이걸 {@code all} 로 바꾸면 탭 이름과 내용이 어긋난다.
     *
     * <p>🚨 <b>참여 불가(퇴사·삭제·계정 비활성) 요청자에게는 특권을 주지 않는다.</b> role 만 보면 퇴사한
     * ADMIN·MASTER 가 회사 전체 결재를 그대로 열람한다 — 상세조회는 {@code ApprovalViewPolicy} 가
     * 참여 불가를 막는데 목록만 뚫려 있어 <b>목록에서 제목·기안자·프로젝트가 다 보이는</b> 상태가 된다.
     * 여기서 막으면 특권만 사라지고 본인 기안 건 조회({@code drafted})는 그대로라, 퇴사 처리 중인
     * 사원의 기존 동작을 건드리지 않는다.
     *
     * @return 서비스가 필터를 고를 때 쓸 최종 {@code scope}
     * @throws ForbiddenException 전체 조회 권한이 없는 요청자가 {@code scope=all} 을 요청했을 때(403)
     */
    public String resolveScope(String requestedScope, String requesterId) {
        EmployeeSummary requester = employeeCatalogPort.findEmployee(requesterId).orElse(null);
        boolean fullAccess = requester != null
                && !requester.participationUnavailable()
                && FULL_ACCESS_ROLES.contains(requester.role());

        if (SCOPE_ALL.equals(requestedScope)) {
            if (!fullAccess) {
                throw new ForbiddenException(ApprovalErrorCode.APPROVAL_SCOPE_ALL_FORBIDDEN);
            }
            return SCOPE_ALL;
        }

        if (fullAccess && ROLE_ADMIN.equals(requester.role()) && !SCOPE_PENDING.equals(requestedScope)) {
            return SCOPE_ALL;
        }

        return requestedScope;
    }
}
