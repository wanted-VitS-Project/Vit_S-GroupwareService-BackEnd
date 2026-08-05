package com.group3.vitamins.approval.application.policy;

import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.ApprovalDocument;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 결재 문서 추가·제거(APR-005~007)가 쓰는 검증 — 파일 버전 상태·중복 연결·존재 확인 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApprovalDocumentEligibilityPolicy {

    private static final String COMPLETED = "COMPLETED";

    private final FileCatalogPort fileCatalogPort;
    private final ApprovalRepository approvalRepository;

    /** APR-005 — file_version 존재(404) + upload_status == COMPLETED(409) 확인 */
    public FileVersionSummary getReadyFileVersionOrThrow(Long fileVersionId) {
        FileVersionSummary file = fileCatalogPort.findFileVersion(fileVersionId)
                .orElseThrow(() -> {
                    log.warn("결재 문서 추가 - 파일 버전 없음 fileVersionId={}", fileVersionId);
                    return new NotFoundException(ApprovalErrorCode.FILE_VERSION_NOT_FOUND);
                });

        if (!COMPLETED.equals(file.uploadStatus())) {
            log.warn("결재 문서 추가 - 업로드 미완료 fileVersionId={}, status={}", fileVersionId, file.uploadStatus());
            throw new ConflictException(ApprovalErrorCode.FILE_VERSION_NOT_READY);
        }
        return file;
    }

    /** APR-006 — 동일 회차에 동일 file_version_id 중복 연결 확인 */
    public void assertNotAlreadyLinked(Long revisionId, Long fileVersionId) {
        if (approvalRepository.existsDocument(revisionId, fileVersionId)) {
            log.warn("결재 문서 추가 - 중복 연결 revisionId={}, fileVersionId={}", revisionId, fileVersionId);
            throw new ConflictException(ApprovalErrorCode.DOCUMENT_ALREADY_LINKED);
        }
    }

    /** 문서가 이 회차 소속인지까지 확인한다 — 아니면 못 찾은 것과 동일하게 404 */
    public ApprovalDocument getDocumentOrThrow(Long revisionId, Long documentId) {
        return approvalRepository.findDocumentById(documentId)
                .filter(doc -> doc.getRevisionId().equals(revisionId))
                .orElseThrow(() -> {
                    log.warn("결재 문서 없음 - revisionId={}, documentId={}", revisionId, documentId);
                    return new NotFoundException(ApprovalErrorCode.APPROVAL_DOCUMENT_NOT_FOUND);
                });
    }
}
