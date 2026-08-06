package com.group3.vitamins.issue.infrastructure.persistence;

import com.group3.vitamins.issue.domain.model.Issue;
import com.group3.vitamins.issue.domain.repository.IssueRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IssueRepositoryAdapter implements IssueRepository {

    private final SpringDataIssueRepository springDataIssueRepository;
    private final SpringDataIssueAssignRepository springDataIssueAssignRepository;
    private final SpringDataIssueBlockRepository springDataIssueBlockRepository;
    private final EntityManager entityManager;

    @Override
    public Issue save(Issue issue) {
        IssueEntity saved = springDataIssueRepository.saveAndFlush(IssueMapper.toEntity(issue));
        entityManager.refresh(saved);
        return IssueMapper.toDomain(saved);
    }

    @Override
    public Optional<Issue> findActiveById(Long issueId) {
        return springDataIssueRepository.findByIssueIdAndDeletedAtIsNull(issueId)
                .map(IssueMapper::toDomain);
    }

    @Override
    public void saveAssignees(Long issueId, List<String> userIds) {
        springDataIssueAssignRepository.saveAll(userIds.stream()
                .map(userId -> IssueAssignEntity.link(issueId, userId))
                .toList());
    }

    @Override
    public void saveBlockLinks(Long issueId, List<Long> blockIds) {
        springDataIssueBlockRepository.saveAll(blockIds.stream()
                .map(blockId -> IssueBlockEntity.link(issueId, blockId))
                .toList());
    }

    @Override
    public void deleteAssignees(Long issueId) {
        springDataIssueAssignRepository.deleteByIssueId(issueId);
    }

    @Override
    public void deleteBlockLinks(Long issueId) {
        springDataIssueBlockRepository.deleteByIssueId(issueId);
    }
}
