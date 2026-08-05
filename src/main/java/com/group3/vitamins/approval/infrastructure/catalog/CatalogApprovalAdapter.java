package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.model.ApprovalLine;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.model.ApprovalWithRevision;
import com.group3.vitamins.approval.domain.model.NewApprovalLine;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.approval.application.port.ApprovalLineDetailPort;
import com.group3.vitamins.approval.application.result.ApprovalLineDetailView;
import com.group3.vitamins.approval.infrastructure.persistence.ApprovalDocumentJpaEntity;
import com.group3.vitamins.approval.infrastructure.persistence.ApprovalJpaEntity;
import com.group3.vitamins.approval.infrastructure.persistence.ApprovalLineJpaEntity;
import com.group3.vitamins.approval.infrastructure.persistence.ApprovalRevisionJpaEntity;
import com.group3.vitamins.approval.infrastructure.persistence.SpringDataApprovalDocumentRepository;
import com.group3.vitamins.approval.infrastructure.persistence.SpringDataApprovalLineRepository;
import com.group3.vitamins.approval.infrastructure.persistence.SpringDataApprovalRepository;
import com.group3.vitamins.approval.infrastructure.persistence.SpringDataApprovalRevisionRepository;
import com.group3.vitamins.approval.infrastructure.persistence.mapper.ApprovalQueryMapper;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalLineDetailRow;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * {@code approval}·{@code approval_revision} 영속성 어댑터 (`text.infrastructure.catalog.CatalogTextAdapter`와 동일 구조).
 *
 * <p>{@link ApprovalLineDetailPort}도 함께 구현한다 — {@code approval_line}은 이 도메인 소유 테이블이라
 * MyBatis 조인 조회({@code ApprovalQueryMapper})도 자기 영속성 어댑터가 맡는 게 자연스럽다(`MYBATIS.md`).
 */
@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CatalogApprovalAdapter implements ApprovalRepository, ApprovalLineDetailPort {

    private final SpringDataApprovalRepository springDataApprovalRepository;
    private final SpringDataApprovalRevisionRepository springDataApprovalRevisionRepository;
    private final SpringDataApprovalLineRepository springDataApprovalLineRepository;
    private final SpringDataApprovalDocumentRepository springDataApprovalDocumentRepository;
    private final ApprovalQueryMapper approvalQueryMapper;

    @Override
    @Transactional
    public ApprovalWithRevision createDraft(Long blockId, String drafterId) {
        ApprovalJpaEntity approval = springDataApprovalRepository.save(
                ApprovalJpaEntity.createDraft(blockId, drafterId));

        ApprovalRevisionJpaEntity revision = springDataApprovalRevisionRepository.save(
                ApprovalRevisionJpaEntity.createDraft(approval.getApprovalId(), approval.getCurrentRevisionNo(), "", null));

        return new ApprovalWithRevision(toApproval(approval), toRevision(revision));
    }

    @Override
    public Optional<Approval> findApproval(Long approvalId) {
        return springDataApprovalRepository.findById(approvalId).map(this::toApproval);
    }

    @Override
    public Optional<ApprovalRevision> findRevisionById(Long revisionId) {
        return springDataApprovalRevisionRepository.findById(revisionId).map(this::toRevision);
    }

    @Override
    @Transactional
    public Optional<ApprovalRevision> findRevisionByIdForUpdate(Long revisionId) {
        return springDataApprovalRevisionRepository.findByIdForUpdate(revisionId).map(this::toRevision);
    }

    @Override
    @Transactional
    public Optional<ApprovalRevision> findLatestRevision(Long approvalId) {
        return springDataApprovalRevisionRepository.findTopByApprovalIdOrderByRevisionNoDesc(approvalId)
                .map(this::toRevision);
    }

    @Override
    public Optional<ApprovalRevision> findLatestRevisionReadOnly(Long approvalId) {
        return springDataApprovalRevisionRepository.findFirstByApprovalIdOrderByRevisionNoDesc(approvalId)
                .map(this::toRevision);
    }

    @Override
    public List<ApprovalLine> findLinesByRevisionId(Long revisionId) {
        return springDataApprovalLineRepository.findByApprovalRevisionIdOrderBySequenceNo(revisionId).stream()
                .map(this::toLine)
                .toList();
    }

    @Override
    public List<ApprovalDocument> findDocumentsByRevisionId(Long revisionId) {
        return springDataApprovalDocumentRepository.findByApprovalRevisionId(revisionId).stream()
                .map(this::toDocument)
                .toList();
    }

    @Override
    public Optional<ApprovalDocument> findDocumentById(Long documentId) {
        return springDataApprovalDocumentRepository.findById(documentId).map(this::toDocument);
    }

    @Override
    public boolean existsDocument(Long revisionId, Long fileVersionId) {
        return springDataApprovalDocumentRepository
                .existsByApprovalRevisionIdAndFileVersionId(revisionId, fileVersionId);
    }

    @Override
    @Transactional
    public ApprovalDocument addDocument(Long revisionId, Long fileVersionId) {
        ApprovalDocumentJpaEntity saved = springDataApprovalDocumentRepository.save(
                ApprovalDocumentJpaEntity.create(revisionId, fileVersionId));
        return toDocument(saved);
    }

    @Override
    @Transactional
    public void deleteDocument(Long documentId) {
        springDataApprovalDocumentRepository.deleteById(documentId);
    }

    @Override
    @Transactional
    public ApprovalRevision updateDraftContent(Long revisionId, String title, String content) {
        int updated = springDataApprovalRevisionRepository.updateTitleContentIfDraft(
                revisionId, title, content, ApprovalStatus.DRAFT);
        if (updated == 0) {
            throw new ConflictException(ApprovalErrorCode.APPROVAL_REVISION_NOT_DRAFT);
        }

        return springDataApprovalRevisionRepository.findById(revisionId)
                .map(this::toRevision)
                .orElseThrow(() -> new IllegalStateException("revision not found after update: " + revisionId));
    }

    @Override
    @Transactional
    public List<ApprovalLine> replaceLines(Long revisionId, List<NewApprovalLine> lines) {
        springDataApprovalLineRepository.deleteAllByApprovalRevisionId(revisionId);

        List<ApprovalLineJpaEntity> saved = lines.stream()
                .map(line -> ApprovalLineJpaEntity.createDraft(revisionId, line.approverId(), line.sequenceNo()))
                .map(springDataApprovalLineRepository::save)
                .toList();

        return saved.stream().map(this::toLine).toList();
    }

    @Override
    @Transactional
    public ApprovalRevision createRevisionDraft(Long approvalId, int revisionNo, String title, String content) {
        ApprovalRevisionJpaEntity saved = springDataApprovalRevisionRepository.save(
                ApprovalRevisionJpaEntity.createDraft(approvalId, revisionNo, title, content));
        return toRevision(saved);
    }

    @Override
    @Transactional
    public List<ApprovalDocument> copyDocuments(Long newRevisionId, List<Long> fileVersionIds) {
        return fileVersionIds.stream()
                .map(fileVersionId -> ApprovalDocumentJpaEntity.create(newRevisionId, fileVersionId))
                .map(springDataApprovalDocumentRepository::save)
                .map(this::toDocument)
                .toList();
    }

    @Override
    @Transactional
    public ApprovalRevision markRevisionSubmitted(Long revisionId) {
        springDataApprovalRevisionRepository.markSubmitted(revisionId, ApprovalStatus.IN_PROGRESS);
        return springDataApprovalRevisionRepository.findById(revisionId)
                .map(this::toRevision)
                .orElseThrow(() -> new IllegalStateException("revision not found after submit: " + revisionId));
    }

    @Override
    @Transactional
    public void markApprovalInProgress(Long approvalId, int revisionNo) {
        springDataApprovalRepository.markInProgress(approvalId, revisionNo, ApprovalStatus.IN_PROGRESS);
    }

    @Override
    @Transactional
    public List<ApprovalLine> activateLines(Long revisionId) {
        springDataApprovalLineRepository.activateFirstAndWaitRest(
                revisionId, ApprovalLineStatus.ACTIVE, ApprovalLineStatus.WAITING);
        return findLinesByRevisionId(revisionId);
    }

    @Override
    @Transactional
    public void softDeleteCascade(Long approvalId, LocalDateTime deletedAt) {
        springDataApprovalLineRepository.softDeleteByApprovalId(approvalId, deletedAt);
        springDataApprovalRevisionRepository.softDeleteByApprovalId(approvalId, deletedAt);
        springDataApprovalRepository.softDelete(approvalId, deletedAt);
    }

    @Override
    public List<ApprovalLineDetailView> findLineDetails(Long revisionId) {
        return approvalQueryMapper.findLineDetailsByRevisionId(revisionId).stream()
                .map(this::toLineDetailView)
                .toList();
    }

    private ApprovalLineDetailView toLineDetailView(ApprovalLineDetailRow row) {
        return new ApprovalLineDetailView(row.lineId(), row.approverId(), row.approverName(),
                row.jobPositionName(), row.approverDepartment(), row.sequenceNo(),
                row.status(), row.opinion(), row.processedAt());
    }

    private Approval toApproval(ApprovalJpaEntity entity) {
        return Approval.reconstruct(
                entity.getApprovalId(), entity.getBlockId(), entity.getDrafterId(), entity.getStatus(),
                entity.getCurrentRevisionNo(), entity.getCompletedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt());
    }

    private ApprovalRevision toRevision(ApprovalRevisionJpaEntity entity) {
        return ApprovalRevision.reconstruct(
                entity.getApprovalRevisionId(), entity.getApprovalId(), entity.getRevisionNo(),
                entity.getTitle(), entity.getContent(), entity.getStatus(),
                entity.getSubmittedAt(), entity.getFinishedAt(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt());
    }

    private ApprovalLine toLine(ApprovalLineJpaEntity entity) {
        return ApprovalLine.reconstruct(
                entity.getApprovalLineId(), entity.getApprovalRevisionId(), entity.getApproverId(),
                entity.getSequenceNo(), entity.getStatus(), entity.getOpinion(), entity.getProcessedAt(),
                entity.getCreatedAt(), entity.getDeletedAt());
    }

    private ApprovalDocument toDocument(ApprovalDocumentJpaEntity entity) {
        return ApprovalDocument.reconstruct(
                entity.getApprovalDocumentId(), entity.getApprovalRevisionId(), entity.getFileVersionId(),
                entity.getCreatedAt());
    }
}
