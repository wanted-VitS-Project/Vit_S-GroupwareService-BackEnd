package com.group3.vitamins.project.application.port;

/**
 * 프로젝트 삭제 시 스텝 애그리게이트에 정리를 요청하는 아웃바운드 포트 (PRJ-014).
 *
 * <p>스텝·블록·이슈·{@code step_permission} 을 <b>포트 하나로 묶는다.</b> 쪼개면 그 정리 순서를
 * 프로젝트 도메인이 알아야 하는데, 그 순서는 스텝 도메인의 사정이다.
 * {@link StageCascadePort} 와 같은 구조다.
 */
public interface StepCascadePort {

    /** 프로젝트의 스텝을 하위 블록·이슈·권한 오버라이드와 함께 정리한다. 논리 삭제한 스텝 수를 돌려준다. */
    int deleteByProjectId(Long projectId, String requesterUserId);
}
