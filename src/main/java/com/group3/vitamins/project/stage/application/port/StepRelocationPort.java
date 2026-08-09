package com.group3.vitamins.project.stage.application.port;

/**
 * 스테이지 삭제 시 하위 스텝을 옮기는 아웃바운드 포트 (STG-003).
 * step 테이블을 직접 쓰지 않고 스텝 애그리게이트의 인바운드 유스케이스에 위임한다.
 */
public interface StepRelocationPort {

    /** toStageId 가 null 이면 미소속으로 뺀다. @return 옮긴 스텝 수 */
    int relocateByStage(Long fromStageId, Long toStageId);
}
