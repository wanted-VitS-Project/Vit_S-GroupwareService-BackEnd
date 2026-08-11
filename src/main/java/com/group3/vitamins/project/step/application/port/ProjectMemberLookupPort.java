package com.group3.vitamins.project.step.application.port;

import com.group3.vitamins.project.domain.model.MemberPermission;

import java.util.List;
import java.util.Optional;

/**
 * 프로젝트 참여자를 물어보는 아웃바운드 포트 — 스텝 권한 화면이 「참여자별 판정 결과」를 그리기 위해 쓴다.
 * project_member 를 직접 읽지 않고 프로젝트 애그리게이트의 인바운드 유스케이스에 위임한다.
 */
public interface ProjectMemberLookupPort {

    /** 프로젝트 참여자 전원. 권한이 NONE 인 참여자도 포함된다. */
    List<Member> findMembers(Long projectId, String requesterUserId, String role);

    /** 참여자 한 명. 참여자가 아니면 empty. */
    Optional<Member> findMember(Long projectId, String userId, String requesterUserId, String role);

    record Member(String userId, String name, MemberPermission permission) {
    }
}
