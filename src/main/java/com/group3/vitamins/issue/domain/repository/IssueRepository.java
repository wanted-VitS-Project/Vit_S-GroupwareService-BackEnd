package com.group3.vitamins.issue.domain.repository;

import com.group3.vitamins.issue.domain.model.Issue;

public interface IssueRepository {

    Issue save(Issue issue);
}
