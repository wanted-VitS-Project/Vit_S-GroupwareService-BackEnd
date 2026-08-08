package com.group3.vitamins.employee.application.port;

import java.util.Collection;
import java.util.Map;

/**
 * 엑셀 일괄 등록(employee.md §7·§8)에서 <b>부서명·직급명 → ID</b> 를 해석하는 아웃바운드 포트.
 *
 * <p>템플릿이 ID 가 아니라 이름을 받으므로(사람이 채우는 값), 검증·등록 전에 이름을 ID 로 바꾼다.
 * N+1 을 피하려 파일에 등장한 이름을 <b>한 번에</b> 조회한다. MyBatis 어댑터가 구현한다(아키텍처 §2-1).
 */
public interface EmployeeBulkReferenceQueryPort {

    /**
     * 부서명 → 부서 ID. ⚠️ 부서명은 <b>형제 유니크</b>(전역 유일 아님, #212)라 같은 이름이 여러 부서일 수 있다.
     * 엑셀에는 부서명만 있어 어느 부서인지 특정할 수 없으므로 <b>전역에서 유일하게 매칭되는 이름만</b> 담는다.
     * 모호하거나 없는 이름은 맵에 빠지며, 호출자가 {@code DEPARTMENT_NOT_FOUND} 로 처리한다.
     */
    Map<String, Long> resolveDepartmentIdsByName(Collection<String> names);

    /** 직급명 → 직급 ID. 직급명은 전역 유니크라 그대로 매핑한다. 없는 이름은 맵에 빠진다(직급은 선택값 → null 등록). */
    Map<String, Long> resolveJobPositionIdsByName(Collection<String> names);
}
