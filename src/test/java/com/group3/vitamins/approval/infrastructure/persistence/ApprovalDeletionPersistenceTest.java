package com.group3.vitamins.approval.infrastructure.persistence;

import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("결재 삭제 전파 영속성")
class ApprovalDeletionPersistenceTest {

    @Autowired private SpringDataApprovalRepository approvalRepository;
    @Autowired private SpringDataApprovalRevisionRepository revisionRepository;
    @Autowired private SpringDataApprovalLineRepository lineRepository;
    @Autowired private SpringDataApprovalDocumentRepository documentRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("미종결 상태는 CANCELED가 되고 네 테이블은 같은 시각에 논리 삭제된다")
    void cancelsUnfinishedRowsAndSoftDeletesDocuments() {
        ApprovalJpaEntity approval = approvalRepository.saveAndFlush(
                ApprovalJpaEntity.createDraft(10L, "EMP001"));
        ApprovalRevisionJpaEntity revision = revisionRepository.saveAndFlush(
                ApprovalRevisionJpaEntity.createDraft(approval.getApprovalId(), 1, "품의", "내용"));
        List<ApprovalLineJpaEntity> lines = lineRepository.saveAllAndFlush(List.of(
                ApprovalLineJpaEntity.createDraft(revision.getApprovalRevisionId(), "EMP002", 1),
                ApprovalLineJpaEntity.createDraft(revision.getApprovalRevisionId(), "EMP003", 2)));
        ApprovalDocumentJpaEntity document = documentRepository.saveAndFlush(
                ApprovalDocumentJpaEntity.create(revision.getApprovalRevisionId(), 900L));

        approvalRepository.markInProgress(approval.getApprovalId(), 1, ApprovalStatus.IN_PROGRESS);
        revisionRepository.markSubmitted(revision.getApprovalRevisionId(), ApprovalStatus.IN_PROGRESS);
        lineRepository.activateFirstAndWaitRest(
                revision.getApprovalRevisionId(), ApprovalLineStatus.ACTIVE, ApprovalLineStatus.WAITING);
        lineRepository.markProcessed(lines.get(0).getApprovalLineId(), ApprovalLineStatus.APPROVED, null);

        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 10, 17, 30);
        documentRepository.softDeleteAllByApprovalId(approval.getApprovalId(), deletedAt);
        lineRepository.softDeleteByApprovalId(
                approval.getApprovalId(), deletedAt,
                List.of(ApprovalLineStatus.DRAFT, ApprovalLineStatus.WAITING, ApprovalLineStatus.ACTIVE),
                ApprovalLineStatus.CANCELED);
        revisionRepository.softDeleteByApprovalId(
                approval.getApprovalId(), deletedAt,
                List.of(ApprovalStatus.DRAFT, ApprovalStatus.IN_PROGRESS), ApprovalStatus.CANCELED);
        approvalRepository.softDelete(
                approval.getApprovalId(), deletedAt,
                List.of(ApprovalStatus.DRAFT, ApprovalStatus.IN_PROGRESS), ApprovalStatus.CANCELED);
        entityManager.clear();

        ApprovalJpaEntity deletedApproval = approvalRepository.findById(approval.getApprovalId()).orElseThrow();
        ApprovalRevisionJpaEntity deletedRevision = revisionRepository
                .findById(revision.getApprovalRevisionId()).orElseThrow();
        ApprovalLineJpaEntity processedLine = lineRepository
                .findById(lines.get(0).getApprovalLineId()).orElseThrow();
        ApprovalLineJpaEntity waitingLine = lineRepository
                .findById(lines.get(1).getApprovalLineId()).orElseThrow();
        ApprovalDocumentJpaEntity deletedDocument = documentRepository
                .findById(document.getApprovalDocumentId()).orElseThrow();

        assertThat(deletedApproval.getStatus()).isEqualTo(ApprovalStatus.CANCELED);
        assertThat(deletedRevision.getStatus()).isEqualTo(ApprovalStatus.CANCELED);
        assertThat(processedLine.getStatus()).isEqualTo(ApprovalLineStatus.APPROVED);
        assertThat(waitingLine.getStatus()).isEqualTo(ApprovalLineStatus.CANCELED);
        assertThat(deletedApproval.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(deletedRevision.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(processedLine.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(waitingLine.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(deletedDocument.getDeletedAt()).isEqualTo(deletedAt);

        assertThat(approvalRepository.findByApprovalIdAndDeletedAtIsNull(approval.getApprovalId())).isEmpty();
        assertThat(revisionRepository.findByApprovalRevisionIdAndDeletedAtIsNull(
                revision.getApprovalRevisionId())).isEmpty();
        assertThat(lineRepository.findByApprovalRevisionIdAndDeletedAtIsNullOrderBySequenceNo(
                revision.getApprovalRevisionId())).isEmpty();
        assertThat(documentRepository.findByApprovalRevisionIdAndDeletedAtIsNull(
                revision.getApprovalRevisionId())).isEmpty();
    }

    @Test
    @DisplayName("이미 완료된 결재 결과는 삭제가 덮어쓰지 않는다")
    void preservesCompletedStatus() {
        ApprovalJpaEntity approval = approvalRepository.saveAndFlush(
                ApprovalJpaEntity.createDraft(20L, "EMP001"));
        ApprovalRevisionJpaEntity revision = revisionRepository.saveAndFlush(
                ApprovalRevisionJpaEntity.createDraft(approval.getApprovalId(), 1, "완료 품의", "내용"));

        approvalRepository.finalizeApproval(approval.getApprovalId(), ApprovalStatus.COMPLETED);
        revisionRepository.finalizeRevision(revision.getApprovalRevisionId(), ApprovalStatus.COMPLETED);

        LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 10, 18, 0);
        revisionRepository.softDeleteByApprovalId(
                approval.getApprovalId(), deletedAt,
                List.of(ApprovalStatus.DRAFT, ApprovalStatus.IN_PROGRESS), ApprovalStatus.CANCELED);
        approvalRepository.softDelete(
                approval.getApprovalId(), deletedAt,
                List.of(ApprovalStatus.DRAFT, ApprovalStatus.IN_PROGRESS), ApprovalStatus.CANCELED);
        entityManager.clear();

        assertThat(approvalRepository.findById(approval.getApprovalId()).orElseThrow().getStatus())
                .isEqualTo(ApprovalStatus.COMPLETED);
        assertThat(revisionRepository.findById(revision.getApprovalRevisionId()).orElseThrow().getStatus())
                .isEqualTo(ApprovalStatus.COMPLETED);
    }
}
