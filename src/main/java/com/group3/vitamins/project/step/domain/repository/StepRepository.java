package com.group3.vitamins.project.step.domain.repository;

import com.group3.vitamins.project.step.domain.model.Step;

import java.util.Optional;

public interface StepRepository {

    Step save(Step step);

    /**
     * 프로젝트 전체 기준 최대 sortOrder. 스텝이 하나도 없으면 empty.
     * 스테이지별이 아니라 프로젝트 전체다 — FE 가 스테이지를 넘어 연속 번호로 그린다.
     */
    Optional<Integer> findMaxSortOrder(Long projectId);
}