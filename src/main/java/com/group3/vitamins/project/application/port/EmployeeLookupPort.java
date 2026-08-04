package com.group3.vitamins.project.application.port;

/** 사번으로 이름을 물어보는 아웃바운드 포트 ({@code employee} 테이블 소관은 아직 특정 도메인이 없다). */
public interface EmployeeLookupPort {
    String findNameByUserId(String userId);
}