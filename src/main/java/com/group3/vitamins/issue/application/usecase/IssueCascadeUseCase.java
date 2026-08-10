package com.group3.vitamins.issue.application.usecase;

import java.util.Collection;

/**
 * 스텝 삭제가 하위 이슈를 정리하기 위해 쓰는 인바운드 유스케이스 (STP-013).
 *
 * <p>⚠️ <b>권한 검사를 하지 않는다</b> — 호출자(스텝 삭제)가 이미 프로젝트 EDITOR 를 확인한 뒤 부른다.
 * {@link IssueCommandUseCase#deleteIssue} 를 그대로 쓰면 이슈마다 <b>스텝</b> EDITOR 를 다시 요구해서,
 * 요청자가 그 스텝에 NONE·VIEWER 오버라이드를 갖고 있으면 명세상 허용된 삭제가 403 으로 롤백된다.
 */
public interface IssueCascadeUseCase {

    /** 지정한 이슈를 담당자·블록 연결과 함께 논리 삭제한다. */
    void deleteIssues(Collection<Long> issueIds);
}
