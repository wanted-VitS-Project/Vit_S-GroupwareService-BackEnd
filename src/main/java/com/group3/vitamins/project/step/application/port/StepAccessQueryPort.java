package com.group3.vitamins.project.step.application.port;

import com.group3.vitamins.project.domain.model.MemberPermission;

import java.util.Optional;

/**
 * 스텝 접근 판정에 필요한 원시값을 한 SQL 로 조회하는 아웃바운드 포트.
 *
 * <p>이 포트는 <b>판정하지 않는다.</b> 값만 가져오고 판정은 {@code StepAccessPolicy} ·
 * {@code ProjectAccessPolicy} 가 그대로 한다 — 기존 정책 코드와 그 테스트를 손대지 않기 위해서다.
 *
 * <p>같은 패턴의 선례는 {@code ProjectDetailQueryPort} 다 (프로젝트 + 참여자 권한을 한 번에 조회).
 */
public interface StepAccessQueryPort {

    /**
     * 스텝 1건의 판정 재료를 가져온다.
     *
     * <p>비어 있으면 <b>스텝이 없거나 논리 삭제됐다</b>는 뜻이며, 호출부가 404 로 만든다.
     * 회사 경계·참여 여부는 비어 있음으로 표현하지 않는다 — 그건 403 이고 스냅샷 필드로 내려온다.
     */
    Optional<StepAccessSnapshot> findAccess(Long stepId, String requesterUserId, Long companyId);

    /**
     * 판정 재료. 이름이 {@code Snapshot} 인 이유는 {@code StepAccessUseCase.StepAccessView}
     * (판정 <b>결과</b>) 와 헷갈리지 않게 하기 위해서다 — 이건 판정 <b>전</b>의 원시값이다.
     *
     * @param projectVisible     현재 로그인 회사가 소유한, 살아있는 프로젝트인지. {@code false} 면
     *                           프로젝트 권한은 {@code NONE} 이다 (404 가 아니라 403 — 스텝 엔드포인트는
     *                           프로젝트 에러코드를 내리지 않는다)
     * @param memberPermission   {@code project_member} 권한. <b>행이 없으면 {@code null}</b>
     * @param overridePermission {@code step_permission} 오버라이드. <b>행이 없으면 {@code null}</b>
     */
    record StepAccessSnapshot(
            Long stepId,
            Long projectId,
            boolean projectVisible,
            MemberPermission memberPermission,
            MemberPermission overridePermission
    ) {
    }
}
