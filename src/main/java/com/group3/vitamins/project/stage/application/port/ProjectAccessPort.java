package com.group3.vitamins.project.stage.application.port;

import com.group3.vitamins.project.domain.model.MemberPermission;

import java.util.Optional;

/**
 * 프로젝트 애그리게이트에 물어보는 아웃바운드 포트 — 존재 확인·권한 조회용.
 * 스테이지 모듈은 이 인터페이스만 알고, 실제 조회는 infrastructure/adapter 구현체가 처리한다.
 */
public interface ProjectAccessPort {

    /** 프로젝트가 존재하는지 확인한다 (논리 삭제분 제외). */
    boolean existsProject(Long projectId);

    /** 요청자의 프로젝트 권한. 참여자가 아니면 empty. */
    Optional<MemberPermission> findPermission(Long projectId, String userId);
}