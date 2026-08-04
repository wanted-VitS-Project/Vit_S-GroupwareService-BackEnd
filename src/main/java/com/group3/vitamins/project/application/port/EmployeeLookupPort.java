package com.group3.vitamins.project.application.port;

import java.util.Collection;
import java.util.Map;

/** 사번으로 이름을 물어보는 아웃바운드 포트 ({@code employee} 테이블 소관은 아직 특정 도메인이 없다). */
public interface EmployeeLookupPort {

    String findNameByUserId(String userId);

    /** 사번 → 이름. 목록 조회에서 책임자 이름을 N+1 없이 채우기 위한 배치 조회. 없는 사번은 키가 없다. */
    Map<String, String> findNamesByUserIds(Collection<String> userIds);
}