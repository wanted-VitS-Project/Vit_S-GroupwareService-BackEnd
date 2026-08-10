package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.entity.CollectionConditionJpaEntity;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository.SpringDataCollectionConditionRepository;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRun;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.domain.repository.CollectionRunRepository;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.entity.CollectionRunJpaEntity;
import com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.mapper.CollectionRunPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CollectionRunRepositoryAdapter
        implements CollectionRunRepository {

    private static final List<CollectionRunStatus> ACTIVE_STATUSES =
            List.of(
                    CollectionRunStatus.PENDING,
                    CollectionRunStatus.PROCESSING
            );

    private final SpringDataCollectionRunRepository runRepository;
    private final SpringDataCollectionConditionRepository conditionRepository;
    private final CollectionRunPersistenceMapper persistenceMapper;

    // 같은 조건으로 아직 종료되지 않은 실행이 있는지 확인합니다.
    @Override
    public boolean existsActiveByConditionId(Long conditionId) {
        return runRepository
                .existsByCrawlCondition_CrawlConditionIdAndRunStatusInAndDeletedAtIsNull(
                        conditionId,
                        ACTIVE_STATUSES
                );
    }

    // 수집 조건 참조를 연결하여 실행 정보를 저장합니다.
    @Override
    public CollectionRun save(CollectionRun collectionRun) {
        CollectionConditionJpaEntity conditionEntity =
                conditionRepository.getReferenceById(
                        collectionRun.conditionId()
                );

        CollectionRunJpaEntity entity = persistenceMapper.toEntity(
                collectionRun,
                conditionEntity
        );

        CollectionRunJpaEntity saved =
                runRepository.saveAndFlush(entity);

        return persistenceMapper.toDomain(saved);
    }

    // 실행 ID와 회사 ID가 일치하는 실행 결과만 조회합니다.
    @Override
    public Optional<CollectionRun> findByIdAndCompanyId(
            Long runId,
            Long companyId
    ) {
        return runRepository
                .findByCrawlRunIdAndCrawlCondition_CompanyIdAndDeletedAtIsNull(
                        runId,
                        companyId
                )
                .map(persistenceMapper::toDomain);
    }
}