package com.group3.vitamins.issue.domain.repository;

import com.group3.vitamins.issue.domain.model.Issue;

import java.util.List;

public interface IssueRepository {

    Issue save(Issue issue);

    /** issue_assign 관계를 저장한다. userIds 는 이미 중복 제거된 최종 목록이다. */
    void saveAssignees(Long issueId, List<String> userIds);

    /** issue_block 관계를 저장한다. blockIds 는 이미 중복 제거된 최종 목록이다. */
    void saveBlockLinks(Long issueId, List<Long> blockIds);
}
