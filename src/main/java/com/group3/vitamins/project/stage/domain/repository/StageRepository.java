package com.group3.vitamins.project.stage.domain.repository;

import com.group3.vitamins.project.stage.domain.model.Stage;

import java.util.List;
import java.util.Optional;

public interface StageRepository {

    Stage save(Stage stage);

    /** 프로젝트의 최대 sortOrder. 스테이지가 하나도 없으면 empty. */
    Optional<Integer> findMaxSortOrder(Long projectId);

    /** sortOrder 오름차순으로 조회한다. 논리 삭제분은 제외한다. */
    List<Stage> findAllByProjectId(Long projectId);

    /** 스테이지가 해당 프로젝트에 존재하는지 확인한다. 논리 삭제분은 제외한다. */
    boolean existsInProject(Long stageId, Long projectId);
}