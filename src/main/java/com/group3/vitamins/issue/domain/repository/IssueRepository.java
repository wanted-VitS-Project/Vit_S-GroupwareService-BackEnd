package com.group3.vitamins.issue.domain.repository;

import com.group3.vitamins.issue.domain.model.Issue;

import java.util.List;
import java.util.Optional;

public interface IssueRepository {

    Issue save(Issue issue);

    Optional<Issue> findActiveById(Long issueId);

    /** 기대 버전이 현재 버전과 같을 때만 일반 필드를 수정하고 새 버전을 만든다. */
    int updateFieldsIfVersionMatches(Issue issue, int expectedVersion);

    /** 관계 목록만 수정할 때 Issue 집합의 버전을 조건부로 증가시킨다. */
    int touchIfVersionMatches(Long issueId, int expectedVersion);

    /** 기대 버전이 현재 버전과 같을 때만 상태와 완료 시각을 수정하고 새 버전을 만든다. */
    int changeStatusIfVersionMatches(Issue issue, int expectedVersion);

    /** issue_assign 관계를 저장한다. userIds 는 이미 중복 제거된 최종 목록이다. */
    void saveAssignees(Long issueId, List<String> userIds);

    /** issue_block 관계를 저장한다. blockIds 는 이미 중복 제거된 최종 목록이다. */
    void saveBlockLinks(Long issueId, List<Long> blockIds);

    void deleteAssignees(Long issueId);

    void deleteBlockLinks(Long issueId);
}
