package com.group3.vitamins.project.application.port;

/**
 * 프로젝트 삭제 시 스테이지 애그리게이트에 정리를 요청하는 아웃바운드 포트 (PRJ-014).
 *
 * <p>스테이지와 {@code stage_permission_default} 를 <b>포트 하나로 묶는다.</b> 둘로 쪼개면
 * 「기본값 하드 삭제 → 스테이지 논리 삭제」 순서를 프로젝트 도메인이 알아야 하는데,
 * 그 순서는 스테이지 도메인의 사정이다.
 */
public interface StageCascadePort {

    /** 프로젝트의 스테이지를 권한 기본값과 함께 정리한다. 논리 삭제한 스테이지 수를 돌려준다. */
    int deleteByProjectId(Long projectId);
}
