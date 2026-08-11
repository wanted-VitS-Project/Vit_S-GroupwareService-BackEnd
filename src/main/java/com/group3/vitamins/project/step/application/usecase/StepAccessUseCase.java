package com.group3.vitamins.project.step.application.usecase;

import com.group3.vitamins.project.domain.model.MemberPermission;

/**
 * 스텝 접근·편집 판정을 스텝 애그리게이트 밖(블록·타입별 도메인)에 노출하는 인바운드 유스케이스.
 * 소비자마다 포트를 복제하지 않게 스텝이 직접 제공한다.
 */
public interface StepAccessUseCase {

    /** 스텝 접근 권한을 확인하고 유효 권한과 소속 프로젝트를 돌려준다. 없으면 404 → 403. */
    StepAccessView requireAccess(Long stepId, String requesterUserId, String role);

    /** 스텝 편집 권한(EDITOR)을 확인한다. 없으면 404 → 403. */
    StepAccessView requireEditable(Long stepId, String requesterUserId, String role);

    /** 스텝 판정 결과. projectId 는 block.project_id 가 폐기돼 스텝에서만 얻을 수 있다. */
    record StepAccessView(Long stepId, Long projectId, MemberPermission permission) {
    }
}