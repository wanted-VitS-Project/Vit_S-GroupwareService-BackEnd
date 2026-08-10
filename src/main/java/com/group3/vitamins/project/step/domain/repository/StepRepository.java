package com.group3.vitamins.project.step.domain.repository;

import com.group3.vitamins.project.step.domain.model.Step;
import com.group3.vitamins.project.step.domain.model.StepStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

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

    /**
     * 요청한 스텝 중 이 프로젝트 소속인 미삭제 스텝만 돌려준다.
     * 남의 프로젝트 스텝이나 없는 ID 는 결과에서 빠지므로, 개수 비교로 404 를 판정한다.
     */
    List<Step> findAllByIdsInProject(Collection<Long> stepIds, Long projectId);

    /** 스테이지 하나에 속한 미삭제 스텝 전부. 스테이지 삭제·권한 일괄 적용에서 쓴다. */
    List<Step> findAllByStageId(Long stageId);

    /**
     * 기대 버전과 DB 버전이 같을 때만 이름·기간·책임자를 덮어쓰고 version 을 올린다.
     * 바뀐 행 수를 돌려준다 — <b>0 이면 그 사이 남이 먼저 저장한 것이다(충돌)</b>.
     *
     * <p>⚠️ {@code save()} 로 대체하지 마라. 검사와 저장이 한 문장 안에서 원자적으로 일어나야
     * 조회~저장 사이의 갱신 유실을 막는다 (`.ai/docs/global/CONCURRENCY.md` §1-3 · §6-4).
     */
    int updateIfVersionMatches(Long stepId, String name, LocalDate startedOn, LocalDate endedOn,
                               String ownerUserId, LocalDateTime updatedAt, int expectedVersion);

    /**
     * 기대 버전이 같을 때만 상태를 바꾼다. 0 이면 충돌이다.
     *
     * <p>완료 정보는 <b>도메인이 계산한 값을 그대로</b> 넘긴다 — DONE 이탈 시 null 로 지우는 규칙이
     * 도메인에 있어서, SQL 에 같은 조건을 다시 쓰면 규칙이 두 곳으로 갈라진다.
     */
    int changeStatusIfVersionMatches(Long stepId, StepStatus status, LocalDateTime completedAt,
                                     String completedBy, LocalDateTime updatedAt,
                                     int expectedVersion);

    /** 기대 버전이 같을 때만 위치를 옮긴다. stageId 가 null 이면 미소속이다. 0 이면 충돌이다. */
    int moveIfVersionMatches(Long stepId, Long stageId, int sortOrder,
                             LocalDateTime updatedAt, int expectedVersion);
}