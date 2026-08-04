package com.group3.vitamins.project.step.domain.repository;

import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;

import java.util.List;
import java.util.Optional;

public interface StepRepository {

    Step save(Step step);

    /**
     * 프로젝트 전체 기준 최대 sortOrder. 스텝이 하나도 없으면 empty.
     * 스테이지별이 아니라 프로젝트 전체다 — FE 가 스테이지를 넘어 연속 번호로 그린다.
     */
    Optional<Integer> findMaxSortOrder(Long projectId);

    /** 논리 삭제분은 조회하지 않는다. */
    Optional<Step> findById(Long stepId);

    /**
     * sortOrder 오름차순 목록.
     *
     * @param stageId null 이면 전체, 0 이면 미소속 스텝만, 그 외는 해당 스테이지만
     * @param status  null 이면 전체
     */
    List<Step> search(Long projectId, Long stageId, StepStatus status);
}