package com.group3.vitamins.employee.application.port;

import java.util.Collection;
import java.util.Map;

/**
 * 엑셀 일괄 등록의 <b>전공/자격증 마스터 자동 생성</b> 아웃바운드 포트 (employee.md §8 {@code autoCreateMasters}).
 *
 * <p>사원 등록 <b>전에</b> 마스터에 없는 이름을 만들어 name→id 를 돌려준다. 다른 도메인(major·certificate) 테이블에
 * 쓰는 것이라 포트는 소비자(employee)가 소유한다(아키텍처 §2-2). 이름 규칙·회사 내 UNIQUE 는 상대 유스케이스가 판정한다.
 *
 * <p>같은 이름이 이미 있으면(다른 관리자가 먼저 만든 경우·대소문자만 다른 경우) 새로 만들지 않고 <b>그 마스터의 id</b> 를 돌려준다.
 * 호출자는 이름이 전부 이름 규칙을 통과했음을 보장한다(검증 단계에서 행 오류로 걸렀다).
 */
public interface QualificationMasterCreatePort {

    /** 전공명 목록 → 생성(또는 동명 매칭)된 전공 ID. 반환 맵은 요청한 모든 이름을 키로 갖는다. */
    Map<String, Long> createMajors(Collection<String> names, Long companyId, String actorRole);

    /** 자격증명 목록 → 생성(또는 동명 매칭)된 자격증 ID. 반환 맵은 요청한 모든 이름을 키로 갖는다. */
    Map<String, Long> createCertificates(Collection<String> names, Long companyId, String actorRole);
}
