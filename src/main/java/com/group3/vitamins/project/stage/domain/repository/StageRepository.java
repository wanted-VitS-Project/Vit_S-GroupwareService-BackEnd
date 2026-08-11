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

    /**
     * 기대 버전과 DB 버전이 같을 때만 이름을 바꾸고 version 을 올린다.
     * 바뀐 행 수를 돌려준다 — <b>0 이면 그 사이 남이 먼저 저장한 것이다(충돌)</b>.
     *
     * <p>⚠️ {@code save()} 로 대체하지 마라. 검사와 저장이 한 문장 안에서 원자적으로 일어나야
     * 조회~저장 사이의 갱신 유실을 막는다 (`.ai/docs/global/CONCURRENCY.md` §1-3 · §6-4).
     */
    int renameIfVersionMatches(Long stageId, String name, int expectedVersion);

    /** 기대 버전이 같을 때만 정렬 순서를 바꾸고 version 을 올린다. 0 이면 충돌이다. */
    int moveIfVersionMatches(Long stageId, int sortOrder, int expectedVersion);
}