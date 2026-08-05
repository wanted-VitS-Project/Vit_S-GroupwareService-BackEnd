package com.group3.vitamins.project.application.port;

/**
 * 스텝 애그리게이트에 물어보는 아웃바운드 포트 — 진척률 집계용.
 * 프로젝트 도메인은 이 인터페이스만 알고, 실제 조회는 infrastructure/adapter 구현체가 처리한다.
 */
public interface StepStatLookupPort {

    /** 프로젝트의 전체·완료 스텝 수를 센다 (논리 삭제분 제외). 스텝이 없으면 둘 다 0 이다. */
    StepStatView countByProjectId(Long projectId);

    record StepStatView(int totalCount, int doneCount) {}
}