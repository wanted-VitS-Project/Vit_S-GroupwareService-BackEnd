package com.group3.vitamins.project.stage.application.port;

import java.util.Map;

/**
 * 스텝 애그리게이트에 물어보는 아웃바운드 포트 — 스테이지별 스텝 수 집계용.
 */
public interface StepCountLookupPort {

    /**
     * 스테이지 ID → 스텝 수. 스텝이 없는 스테이지는 키 자체가 없다.
     * 요청자의 step_permission 이 NONE 인 스텝은 세지 않는다 (STP-010 — 스텝 목록과 수를 일치시킨다).
     */
    Map<Long, Integer> countByStage(Long projectId, String userId);
}