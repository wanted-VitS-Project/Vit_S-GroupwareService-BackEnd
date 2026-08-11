package com.group3.vitamins.file.application.port;

import java.util.Optional;

/**
 * 결재 잠금 조회 아웃바운드 포트(§5). 문서의 버전 중 하나라도 <b>진행 중인 결재</b>의 대상이면
 * 휴지통 이동을 막는다 (`file.md` §5 · `BLOCK.md` §4-4).
 *
 * <p>파일 도메인은 결재 애그리게이트에 의존하지 않는다 — 구현체가 결재 테이블을
 * 읽기 전용 조인(MyBatis)으로 조회할 뿐이다({@code infrastructure/adapter/ApprovalLockQueryAdapter}).
 * `UploaderLookupPort` 가 사원·부서를 조회하는 것과 같은 방식이다.
 */
public interface ApprovalLockQueryPort {

    /**
     * 문서를 대상으로 하는 진행 중(IN_PROGRESS) 결재를 찾는다.
     * 없으면 {@link Optional#empty()} — 삭제 가능하다는 뜻이다.
     */
    Optional<InProgressApproval> findInProgressApproval(Long fileId);

    /**
     * 문서의 버전 중 하나라도 결재({@code approval_document})의 참조 대상이면 {@code true} (§7 영구삭제).
     * 진행 중만 보는 {@link #findInProgressApproval} 과 달리 <b>완료 결재까지 포함한 모든</b> 참조를 본다 —
     * 영구 삭제는 되돌릴 수 없어 결재 이력이 열리지 않게 되는 것을 막아야 하기 때문이다.
     */
    boolean existsAnyApprovalReference(Long fileId);

    /** 진행 중 결재 스냅샷. 409 응답 메시지에 담을 최소 정보(결재 id·제목). */
    record InProgressApproval(Long approvalId, String title) {
    }
}
