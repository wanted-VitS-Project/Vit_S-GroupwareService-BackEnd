package com.group3.vitamins.project.step.application.port;

import java.util.Collection;

/**
 * 이슈를 논리 삭제하는 아웃바운드 포트 (STP-013).
 * 스텝을 지우면 하위 이슈는 함께 삭제된다 — 다른 스텝으로 옮기는 선택지는 없다 (STP-008 폐기).
 *
 * <p>이슈 테이블을 직접 쓰지 않고 이슈 애그리게이트의 인바운드 유스케이스에 위임한다 —
 * 담당자·블록 연결 정리가 그쪽에 있다.
 *
 * <p>⚠️ 요청자·역할을 넘기지 않는다. 스텝 삭제 권한(프로젝트 EDITOR)은 호출부에서 이미 판정했고,
 * 이슈마다 스텝 EDITOR 를 다시 물으면 오버라이드 하나로 전체가 403 롤백된다.
 */
public interface IssueDeleteCommandPort {

    void delete(Collection<Long> issueIds);
}
