package com.group3.vitamins.project.stage.domain.repository;

import com.group3.vitamins.project.domain.model.MemberPermission;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 스테이지별 「새 스텝 권한 기본값」 (STG-004).
 *
 * <p>⚠️ <b>권한 판정에 쓰지 않는다</b> (INV-01). 스텝이 <b>생성될 때만</b> 읽혀서
 * {@code step_permission} 행으로 복사된다 — 그래서 스텝을 다른 스테이지로 옮겨도 권한이 안 바뀐다.
 */
public interface StagePermissionDefaultRepository {

    /** 스테이지의 기본값 전부. 사번 → 권한. 행이 없는 사람은 키가 없다. */
    Map<String, MemberPermission> findAllByStageId(Long stageId);

    /** 기본값을 만들거나 갱신한다. (stage_id, user_id) 는 UNIQUE 라 사람당 한 행이다. */
    void save(Long stageId, String userId, MemberPermission permission, LocalDateTime now);

    /** 스테이지 삭제 시 그 스테이지의 기본값을 전부 지운다. */
    void deleteByStageId(Long stageId);

    /**
     * 참여자 제거 시 그 프로젝트 전 스테이지에서 그 사용자의 기본값을 지운다.
     * 안 지우면 재초대 후 새 스텝에서 죽은 기본값이 되살아난다.
     */
    void deleteByProjectIdAndUserId(Long projectId, String userId);
}
