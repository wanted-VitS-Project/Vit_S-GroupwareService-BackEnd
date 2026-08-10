package com.group3.vitamins.project.stage.domain.repository;

import com.group3.vitamins.project.stage.domain.model.Stage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StageRepository {

    Stage save(Stage stage);

    /** 논리 삭제분은 조회하지 않는다. */
    Optional<Stage> findById(Long stageId);

    /**
     * 요청한 스테이지 중 이 프로젝트 소속인 미삭제 스테이지만 돌려준다.
     * 남의 프로젝트 것이나 없는 ID 는 결과에서 빠지므로, 개수 비교로 404 를 판정한다.
     */
    List<Stage> findAllByIdsInProject(Collection<Long> stageIds, Long projectId);

    /** 프로젝트의 최대 sortOrder. 스테이지가 하나도 없으면 empty. */
    Optional<Integer> findMaxSortOrder(Long projectId);

    /** sortOrder 오름차순으로 조회한다. 논리 삭제분은 제외한다. */
    List<Stage> findAllByProjectId(Long projectId);

    /** 스테이지가 해당 프로젝트에 존재하는지 확인한다. 논리 삭제분은 제외한다. */
    boolean existsInProject(Long stageId, Long projectId);
}