package com.group3.vitamins.project.application.port;

import java.util.Collection;
import java.util.Map;

/** 사번으로 이름을 물어보는 아웃바운드 포트 ({@code employee} 테이블 소관은 아직 특정 도메인이 없다). */
public interface EmployeeLookupPort {

    /**
     * 쓰기 경로의 사원 존재 검증용 — <b>삭제된 사원은 못 찾는다</b>(이름이 {@code null}).
     * 담당자 지정·권한 부여·참여자 추가는 살아있는 사원만 대상이어야 하므로 이게 맞다.
     */
    String findNameByUserId(String userId);

    /**
     * 사번 → 이름·삭제여부. 목록 조회에서 책임자 이름을 N+1 없이 채우기 위한 배치 조회.
     * 없는 사번은 키가 없다.
     *
     * <p>⚠️ {@link #findNameByUserId} 와 달리 <b>삭제된 사원도 반환한다</b> —
     * 이미 지정돼 있던 담당자를 화면에서 지우지 않고 「삭제됨」으로 표시하기 위해서다
     * ({@code .ai/docs/global/DELETE.md} D-6).
     */
    Map<String, EmployeeRef> findRefsByUserIds(Collection<String> userIds);

    /**
     * @param name    사원 이름
     * @param deleted 논리 삭제 여부. 퇴사({@code resigned_at})와 다른 값이다
     */
    record EmployeeRef(String name, boolean deleted) {
    }
}
