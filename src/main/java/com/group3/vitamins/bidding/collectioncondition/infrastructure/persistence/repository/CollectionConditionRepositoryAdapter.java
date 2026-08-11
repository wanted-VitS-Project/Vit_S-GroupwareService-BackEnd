package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionCondition;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionConditionRepository;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionConditionJpaEntity;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionSourceJpaEntity;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionRunConditionPort;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionConditionResultPort;
import com.group3.vitamins.bidding.collectionrun.application.port.ScheduledCollectionConditionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CollectionConditionRepositoryAdapter
        implements CollectionConditionRepository,
        CollectionRunConditionPort,
        CollectionConditionResultPort,
        ScheduledCollectionConditionPort {

    private final SpringDataCollectionConditionRepository conditionRepository;
    private final SpringDataCollectionSourceRepository sourceRepository;
    private final CollectionConditionPersistenceMapper persistenceMapper;

    // 현재 회사가 소유한 논리 삭제되지 않은 수집 조건만 조회합니다.
    @Override
    public List<CollectionCondition> findAllNotDeleted(Long companyId) {
        return conditionRepository
                .findAllByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(companyId)
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
    }

    // 조건 ID와 회사 ID가 모두 일치하는 수집 조건만 조회합니다.
    @Override
    public Optional<CollectionCondition> findNotDeletedById(
            Long conditionId,
            Long companyId
    ) {
        return conditionRepository
                .findByCrawlConditionIdAndCompanyIdAndDeletedAtIsNull(
                        conditionId,
                        companyId
                )
                .map(persistenceMapper::toDomain);
    }

    // 공용 수집처 Entity를 연결하여 회사별 수집 조건을 저장합니다.
    @Override
    public CollectionCondition save(CollectionCondition condition) {
        CollectionSourceJpaEntity sourceEntity = sourceRepository
                .findBySourceCodeAndDeletedAtIsNull(
                        condition.getSourceCode()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "수집 조건의 수집처를 찾을 수 없습니다."
                ));

        CollectionConditionJpaEntity entity =
                persistenceMapper.toEntity(condition, sourceEntity);

        CollectionConditionJpaEntity saved =
                conditionRepository.saveAndFlush(entity);

        return persistenceMapper.toDomain(saved);
    }

    // 현재 회사의 수집 조건을 잠금 조회하여 중복 실행 생성을 방지합니다.
    @Override
    public Optional<CollectionCondition> findOwnedConditionForUpdate(
            Long conditionId,
            Long companyId
    ) {
        return conditionRepository
                .findOwnedConditionForUpdate(conditionId, companyId)
                .map(persistenceMapper::toDomain);
    }

    // 실행 시각이 지난 자동 수집 조건을 오래된 순서대로 점유합니다.
    @Override
    public List<CollectionCondition> claimDueConditions(
            java.time.LocalDateTime now,
            int batchSize
    ) {
        return conditionRepository.findDueConditionsForUpdate(
                        now,
                        PageRequest.of(0, batchSize)
                )
                .stream()
                .map(persistenceMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void recordSuccess(
            Long conditionId,
            java.time.LocalDateTime successAt,
            int collectedCount
    ) {
        int updatedCount = conditionRepository.recordCollectionSuccess(
                conditionId,
                successAt,
                collectedCount
        );
        if (updatedCount != 1) {
            throw new IllegalStateException(
                    "완료된 수집 조건을 갱신할 수 없습니다. conditionId=" + conditionId
            );
        }
    }
}
