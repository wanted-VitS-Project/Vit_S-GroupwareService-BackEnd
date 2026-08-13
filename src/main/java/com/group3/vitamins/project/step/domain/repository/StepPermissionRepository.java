package com.group3.vitamins.project.step.domain.repository;

import com.group3.vitamins.project.domain.model.MemberPermission;

import java.time.LocalDateTime;
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

    /** 스텝 하나에 걸린 오버라이드 전부. 사번 → 권한. 행이 없는 사람은 키가 없다. */
    Map<String, MemberPermission> findAllByStepId(Long stepId);

    /** 오버라이드를 만들거나 갱신한다. (step_id, user_id) 는 UNIQUE 라 사람당 한 행이다. */
    void save(Long stepId, String userId, MemberPermission permission, LocalDateTime now);

    /** 오버라이드 행을 지운다. 지울 행이 없었으면 false. */
    boolean deleteByStepIdAndUserId(Long stepId, String userId);

    /** 프로젝트에 속한 스텝들에서 그 사용자의 오버라이드를 전부 지운다. 참여자 제거 시 호출된다. */
    void deleteByProjectIdAndUserId(Long projectId, String userId);

    /**
     * 스텝 삭제 시 그 스텝의 오버라이드를 전부 지운다.
     *
     * <p>⚠️ <b>D-3 위반이 아니다</b> ({@code .ai/docs/global/DELETE.md} §2-2 예외).
     * 스텝은 복구가 없고(§6-5), 이 행은 판정 체인에서 살아있는 스텝만 조회하므로(INV-01)
     * 남겨도 아무것도 하지 않는다. {@code StageCommandService.deleteStage} 와 같은 구조다.
     */
    void deleteByStepId(Long stepId);
}
