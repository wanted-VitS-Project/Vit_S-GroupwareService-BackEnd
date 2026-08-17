package com.group3.vitamins.approval.application.port;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 파일 도메인(김동현님 소관)에 물어보는 아웃바운드 포트 — INV-04({@code file_version_id} 만 참조).
 * Block/Project 포트와 동일하게, 파일 도메인도 아직 구현이 없어 어댑터는 임시 스텁이다.
 * 실제 구현체가 나오면 이 인터페이스는 그대로 두고 어댑터만 교체한다.
 */
public interface FileCatalogPort {

    /** 문서 추가·제거(APR-005·007)처럼 대상이 한 건인 경로용. */
    Optional<FileVersionSummary> findFileVersion(Long fileVersionId);

    /**
     * 상세조회(MGT-005·MGT-006)의 첨부 목록용 배치 조회 — {@code fileVersionId} 로 색인해 돌려준다.
     *
     * <p>단건 조회를 회차 첨부 수만큼 반복하면 첨부 1건당 2쿼리({@code file_version} + 소유
     * {@code file})가 나가 N+1 이 된다. 여기서는 개수와 무관하게 <b>쿼리 1발</b>이다.
     *
     * <p>⚠️ <b>없는 {@code fileVersionId} 는 결과 맵에 키 자체가 없다</b>(빈 값이 들어가는 게 아니다).
     * 호출자는 {@code getOrDefault}·{@code containsKey} 로 "못 찾음"을 직접 처리해야 한다 —
     * 단건 경로의 {@code Optional.orElse(...)} 와 같은 대체값 규칙을 그대로 적용하면 된다.
     */
    Map<Long, FileVersionSummary> findFileVersions(Collection<Long> fileVersionIds);
}
