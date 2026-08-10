package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository;

import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionSource;
import com.group3.vitamins.bidding.collectioncondition.domain.repository.CollectionSourceRepository;
import com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.mapper.CollectionSourcePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CollectionSourceRepositoryAdapter
        implements CollectionSourceRepository {

    private final SpringDataCollectionSourceRepository springDataRepository;

    // 삭제되지 않은 공용 수집처를 코드로 조회합니다.
    @Override
    public Optional<CollectionSource> findNotDeletedByCode(
            String sourceCode
    ) {
        return springDataRepository
                .findBySourceCodeAndDeletedAtIsNull(sourceCode)
                .map(CollectionSourcePersistenceMapper::toDomain);
    }
}