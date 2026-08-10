package com.group3.vitamins.project.step.application.usecase;

/**
 * 스텝의 소속 스테이지를 옮기는 인바운드 유스케이스 — 스테이지 삭제(STG-003)가 쓴다.
 * 스테이지가 step 테이블을 직접 쓰지 않게 스텝이 직접 제공한다.
 */
public interface StepRelocationUseCase {

    /**
     * 한 스테이지의 스텝을 통째로 다른 스테이지로 옮긴다. toStageId 가 null 이면 미소속으로 뺀다.
     *
     * <p>권한 검사는 하지 않는다 — 호출자(스테이지 삭제)가 이미 프로젝트 EDITOR 를 확인한 뒤 부른다.
     * 정렬 순서와 <b>권한은 그대로 둔다</b> — 위치가 바뀌어도 권한은 안 따라간다 (INV-01).
     *
     * @return 옮긴 스텝 수
     */
    int relocateByStage(Long fromStageId, Long toStageId);
}
