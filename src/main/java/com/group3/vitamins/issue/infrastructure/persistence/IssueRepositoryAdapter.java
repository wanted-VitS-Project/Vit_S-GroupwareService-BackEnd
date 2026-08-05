package com.group3.vitamins.issue.infrastructure.persistence;

import com.group3.vitamins.issue.domain.model.Issue;
import com.group3.vitamins.issue.domain.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IssueRepositoryAdapter implements IssueRepository {

    private final SpringDataIssueRepository springDataIssueRepository;

    @Override
    public Issue save(Issue issue) {
        return IssueMapper.toDomain(
                springDataIssueRepository.save(IssueMapper.toEntity(issue)));
    }
}
