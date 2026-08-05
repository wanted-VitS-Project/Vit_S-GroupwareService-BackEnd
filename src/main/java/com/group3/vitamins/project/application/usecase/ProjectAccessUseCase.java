package com.group3.vitamins.project.application.usecase;

import com.group3.vitamins.project.domain.model.MemberPermission;

/**
 * 프로젝트 존재·권한 확인을 하위 애그리게이트(stage · step · block)에 제공하는 인바운드 유스케이스.
 * 소비자가 project·project_member 를 직접 읽지 않게 한다.
 */
public interface ProjectAccessUseCase {

    /** 프로젝트 존재를 확인하고 요청자의 유효 권한을 돌려준다. 없으면 404, 접근 권한이 없으면 403. */
    MemberPermission requireAccess(Long projectId, String requesterUserId, String role);

    /** 프로젝트 존재를 확인하고 편집 권한을 요구한다. 없으면 404, EDITOR 가 아니면 403. */
    void requireEditable(Long projectId, String requesterUserId, String role);

    /**
     * 예외를 던지지 않고 요청자의 유효 프로젝트 권한만 돌려준다. 참여자가 아니면 NONE.
     * 프로젝트 에러코드를 낼 수 없는 하위 리소스 조회에서 쓴다.
     */
    MemberPermission resolvePermission(Long projectId, String requesterUserId, String role);
}