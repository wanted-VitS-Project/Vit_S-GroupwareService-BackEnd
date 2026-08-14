package com.group3.vitamins.project.application.port;

import java.util.Map;

/**
 * 프로젝트 복제 시 스텝 애그리게이트에 복사를 요청하는 아웃바운드 포트 (PRJ-018).
 */
public interface StepClonePort {

    /**
     * 스텝을 복사하고 <b>원본 stepId → 새 stepId</b> 매핑을 돌려준다. 크기가 곧 복사된 스텝 수다.
     *
     * @param stageIdMap {@link StageClonePort#cloneStages} 가 돌려준 매핑
     */
    Map<Long, Long> cloneSteps(Long sourceProjectId, Long targetProjectId,
                               Map<Long, Long> stageIdMap);
}
