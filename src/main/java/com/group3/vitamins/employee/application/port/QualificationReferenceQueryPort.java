package com.group3.vitamins.employee.application.port;

import java.util.Collection;
import java.util.Set;

/**
 * 등록·수정 시 사원 학력/자격증이 참조하는 마스터(전공·자격증)의 존재를 확인하는 아웃바운드 포트
 * (`employee.md` §3·§4 · qualification.md). 부서·직급의 {@link EmployeeReferenceQueryPort} 와 같은 역할이며,
 * 여러 건을 한 번에 검증하도록 <b>배치</b>로 조회한다(요청 배열에 여러 학력/자격증이 담긴다).
 *
 * <p>회사 스코프 — 타사 마스터는 없는 것으로 취급한다. 반환 집합에 빠진 id 가 참조 대상이면
 * {@code MAJOR_NOT_FOUND} / {@code CERT_NOT_FOUND} 로 막는다.
 */
public interface QualificationReferenceQueryPort {

    /** 주어진 전공 id 중 이 회사에 실재하는 것들. */
    Set<Long> findExistingMajorIds(Collection<Long> majorIds, Long companyId);

    /** 주어진 자격증 id 중 이 회사에 실재하는 것들. */
    Set<Long> findExistingCertificateIds(Collection<Long> certificateIds, Long companyId);
}
