package com.group3.vitamins.project.step.domain.repository;

import com.group3.vitamins.project.domain.model.MemberPermission;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * {@code step_permission} 은 스텝 권한 오버라이드 테이블이다 — 행이 없으면 프로젝트 권한을 상속한다.
 * 스텝과 생명주기가 같아 스텝 애그리게이트 내부로 본다.
 */
public interface StepPermissionRepository {

    /** 스텝 ID → 요청자의 오버라이드 권한. 행이 없는 스텝은 키 자체가 없다. */
    Map<Long, MemberPermission> findOverrides(Collection<Long> stepIds, String userId);

    /** 단건 오버라이드. 없으면 empty. */
    Optional<MemberPermission> findOverride(Long stepId, String userId);
}
