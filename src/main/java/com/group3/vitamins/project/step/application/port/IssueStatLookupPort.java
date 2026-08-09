package com.group3.vitamins.project.step.application.port;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 이슈 애그리게이트에 물어보는 아웃바운드 포트 — 스텝별 이슈 집계용. */
public interface IssueStatLookupPort {

    /** 스텝 ID → 이슈 집계. 이슈가 0건인 스텝은 키 자체가 없다. */
    Map<Long, IssueStatView> countByStepIds(Collection<Long> stepIds);

    /**
     * 스텝의 미완료 이슈 ID (TO_DO · IN_PROGRESS). 완료 시점 스냅샷이라 개수도 이 목록 크기로 센다.
     * 이슈가 없으면 빈 목록이다.
     */
    List<Long> findOpenIssueIds(Long stepId);

    /** 진행 전 이슈 수는 {@code totalCount - doneCount - inProgressCount} 로 FE 가 계산한다. */
    record IssueStatView(int totalCount, int doneCount, int inProgressCount) {

        static final IssueStatView EMPTY = new IssueStatView(0, 0, 0);

        public static IssueStatView empty() {
            return EMPTY;
        }
    }
}
