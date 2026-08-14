package com.group3.vitamins.project.application.port;

import java.util.Map;

/**
 * 프로젝트 복제 시 스테이지 애그리게이트에 복사를 요청하는 아웃바운드 포트 (PRJ-018).
 *
 * <p>{@code StageCascadePort} 의 반대 방향이다 — 삭제와 마찬가지로 계층별 포트를 프로젝트가
 * 순서대로 부르고, 새로 생긴 id 매핑만 다음 계층에 넘긴다.
 */
public interface StageClonePort {

    /** 스테이지를 복사하고 <b>원본 stageId → 새 stageId</b> 매핑을 돌려준다. 크기가 곧 복사된 스테이지 수다. */
    Map<Long, Long> cloneStages(Long sourceProjectId, Long targetProjectId);
}
