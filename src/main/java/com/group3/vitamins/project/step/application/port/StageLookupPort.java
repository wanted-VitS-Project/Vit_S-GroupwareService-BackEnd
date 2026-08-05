package com.group3.vitamins.project.step.application.port;

/**
 * 스테이지 애그리게이트에 물어보는 아웃바운드 포트 — 소속 스테이지 유효성 확인용.
 */
public interface StageLookupPort {

    /**
     * 스테이지가 해당 프로젝트에 존재하는지 확인한다 (논리 삭제분 제외).
     * projectId 를 같이 보는 이유는 남의 프로젝트 스테이지에 스텝을 붙이는 것을 막기 위해서다.
     */
    boolean existsInProject(Long stageId, Long projectId);
}