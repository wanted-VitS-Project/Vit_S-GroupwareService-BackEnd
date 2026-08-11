package com.group3.vitamins.project.step.application.port;

/**
 * 이슈를 완료 상태로 바꾸는 아웃바운드 포트 (STP-006 의 CLOSE).
 *
 * <p>이슈 테이블을 직접 쓰지 않고 이슈 애그리게이트의 인바운드 유스케이스에 위임한다 —
 * 상태 전이 규칙과 권한 판정이 그쪽에 있고, 여기서 복제하면 두 벌이 갈라진다.
 */
public interface IssueCloseCommandPort {

    /** 이슈 하나를 완료 처리한다. 권한 검사는 이슈 쪽이 스텝 EDITOR 기준으로 수행한다. */
    void close(Long issueId, String requesterUserId, String role);
}
