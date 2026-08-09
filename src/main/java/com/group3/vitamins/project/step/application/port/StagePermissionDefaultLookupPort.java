package com.group3.vitamins.project.step.application.port;

import com.group3.vitamins.project.domain.model.MemberPermission;

import java.util.Map;

/**
 * 새 스텝에 찍을 권한 기본값을 스테이지에 물어보는 아웃바운드 포트 (STG-004).
 * 스텝 <b>생성 시점에만</b> 호출한다 — 판정 경로에서는 절대 부르지 않는다 (INV-01).
 */
public interface StagePermissionDefaultLookupPort {

    /** 사번 → 권한. 기본값이 없으면 빈 맵. */
    Map<String, MemberPermission> findDefaults(Long stageId);
}
