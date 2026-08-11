package com.group3.vitamins.project.application.port;

/**
 * 스텝 애그리게이트에 정리를 요청하는 아웃바운드 포트 — 참여자 제거 시 스텝 권한 오버라이드를 함께 지운다.
 * 프로젝트 도메인은 이 인터페이스만 알고, 실제 삭제는 infrastructure/adapter 구현체가 처리한다.
 */
public interface StepPermissionCleanupPort {

    /** 해당 프로젝트에 속한 스텝들에서 그 사용자의 오버라이드 행을 전부 지운다. 없으면 아무 일도 하지 않는다. */
    void deleteByProjectIdAndUserId(Long projectId, String userId);
}
