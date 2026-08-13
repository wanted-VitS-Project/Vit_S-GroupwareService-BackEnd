package com.group3.vitamins.bidding.referencefile.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import com.group3.vitamins.bidding.referencefile.infrastructure.persistence.entity.BidReferenceFileJpaEntity;
import com.group3.vitamins.bidding.referencefile.infrastructure.persistence.entity.BidReferenceFileOutboxJpaEntity;
import com.group3.vitamins.bidding.referencefile.infrastructure.persistence.repository.BidReferenceFileJpaRepository;
import com.group3.vitamins.bidding.referencefile.infrastructure.persistence.repository.BidReferenceFileOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaBidReferenceFileRepositoryAdapter implements BidReferenceFileRepository {

    private static final String INDEX_REQUESTED_EVENT = "REFERENCE_FILE_INDEX_REQUESTED";
    private static final String DELETE_REQUESTED_EVENT = "REFERENCE_FILE_DELETE_REQUESTED";

    private final BidReferenceFileJpaRepository repository;
    private final BidReferenceFileOutboxJpaRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public BidReferenceFile save(BidReferenceFile referenceFile) {
        BidReferenceFileJpaEntity entity;
        if (referenceFile.referenceFileId() == null) {
            entity = BidReferenceFileJpaEntity.from(referenceFile);
        } else {
            entity = repository.findById(referenceFile.referenceFileId())
                    .orElseThrow(() -> new IllegalStateException("입찰 기준자료를 찾을 수 없습니다."));
            entity.apply(referenceFile);
        }
        return repository.save(entity).toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BidReferenceFile> findByIdAndCompanyId(Long referenceFileId, Long companyId) {
        return repository
                .findByReferenceFileIdAndCompanyIdAndDeletedAtIsNull(referenceFileId, companyId)
                .map(BidReferenceFileJpaEntity::toDomain);
    }

    @Override
    @Transactional
    public Optional<BidReferenceFile> findByIdAndCompanyIdForUpdate(Long referenceFileId, Long companyId) {
        return repository
                .findByReferenceFileIdAndCompanyIdAndDeletedAtIsNullForUpdate(referenceFileId, companyId)
                .map(BidReferenceFileJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BidReferenceFile> findAllActiveByCompanyId(Long companyId) {
        return repository
                .findAllByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDescReferenceFileIdDesc(companyId)
                .stream()
                .map(BidReferenceFileJpaEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public BidReferenceFile saveCompletedWithIndexOutbox(BidReferenceFile referenceFile) {
        BidReferenceFileJpaEntity entity = repository.findById(referenceFile.referenceFileId())
                .orElseThrow(() -> new IllegalStateException("입찰 기준자료를 찾을 수 없습니다."));
        entity.apply(referenceFile);
        BidReferenceFileJpaEntity saved = repository.saveAndFlush(entity);

        JsonNode payload = objectMapper.createObjectNode()
                .put("referenceFileId", saved.getReferenceFileId())
                .put("companyId", saved.getCompanyId())
                .put("attemptId", saved.getIndexAttemptId());

        outboxRepository.save(BidReferenceFileOutboxJpaEntity.pending(
                UUID.randomUUID().toString(),
                saved.getReferenceFileId(),
                saved.getIndexAttemptId(),
                INDEX_REQUESTED_EVENT,
                payload,
                saved.getUpdatedAt()
        ));

        return saved.toDomain();
    }

    @Override
    @Transactional
    public BidReferenceFile saveDeletedWithCleanupOutbox(BidReferenceFile referenceFile) {
        BidReferenceFileJpaEntity entity = repository.findById(referenceFile.referenceFileId())
                .orElseThrow(() -> new IllegalStateException("입찰 기준자료를 찾을 수 없습니다."));
        entity.apply(referenceFile);
        BidReferenceFileJpaEntity saved = repository.saveAndFlush(entity);

        JsonNode payload = objectMapper.createObjectNode()
                .put("referenceFileId", saved.getReferenceFileId())
                .put("companyId", saved.getCompanyId());

        // attemptId를 매번 새로 뽑으면 (파일ID, attemptId, 이벤트유형) 유니크 제약이
        // 중복 삭제 요청을 못 걸러낸다. 파일당 정리 시도는 하나면 충분하므로 ID로 고정한다.
        outboxRepository.save(BidReferenceFileOutboxJpaEntity.pending(
                UUID.randomUUID().toString(),
                saved.getReferenceFileId(),
                "delete-" + saved.getReferenceFileId(),
                DELETE_REQUESTED_EVENT,
                payload,
                saved.getUpdatedAt()
        ));

        return saved.toDomain();
    }
}