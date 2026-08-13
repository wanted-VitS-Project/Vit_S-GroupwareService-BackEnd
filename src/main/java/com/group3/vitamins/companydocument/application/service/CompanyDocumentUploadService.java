package com.group3.vitamins.companydocument.application.service;

import com.group3.vitamins.companydocument.application.command.CompleteCompanyDocumentUploadCommand;
import com.group3.vitamins.companydocument.application.command.StartCompanyDocumentUploadCommand;
import com.group3.vitamins.companydocument.application.policy.CompanyDocumentAdminPolicy;
import com.group3.vitamins.companydocument.application.port.CompanyDocumentIndexTriggerPort;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentUploadStartResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionDetailResult;
import com.group3.vitamins.companydocument.application.usecase.CompanyDocumentUploadUseCase;
import com.group3.vitamins.companydocument.domain.exception.CompanyDocumentErrorCode;
import com.group3.vitamins.companydocument.domain.model.CompanyDocument;
import com.group3.vitamins.companydocument.domain.model.CompanyDocumentVersion;
import com.group3.vitamins.companydocument.domain.model.DocumentCategory;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentRepository;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentVersionRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.port.PdfPageCounterPort;
import com.group3.vitamins.file.application.port.UploaderLookupPort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/**
 * 사내 문서 업로드 서비스 (§1 시작 · §2 완료 통보).
 *
 * <p>file 의 {@code FileUploadService} 를 미러링하되, (1) 블록→스텝 권한 대신 회사 ADMIN 판정,
 * (2) 업로더 조회가 비면 예외 대신 null 스냅샷(§6-6), (3) 활동로그 없음, (4) company_id 스코프 검증으로 달라진다.
 * 저장소·PDF·업로더 조회 인프라는 file 도메인 포트를 그대로 재사용한다(도메인 무관 인프라).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyDocumentUploadService implements CompanyDocumentUploadUseCase {

    /** 50MB (CDOC-003). */
    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024;

    /** 실행 파일 블랙리스트 (CDOC-003). 나머지는 허용. file 과 동일. */
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            "exe", "bat", "sh", "jar", "cmd", "com", "msi", "scr", "dll", "bin", "app");

    private final CompanyDocumentAdminPolicy adminPolicy;
    private final CompanyDocumentRepository documentRepository;
    private final CompanyDocumentVersionRepository versionRepository;
    private final CompanyDocumentStorageKeyBuilder storageKeyBuilder;
    private final CompanyDocumentVersionFailureRecorder failureRecorder;
    private final CompanyDocumentIndexTriggerPort indexTriggerPort;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    // ↓ file 도메인 인프라 포트 재사용 (S3·PDF·업로더 조회는 도메인 무관)
    private final UploaderLookupPort uploaderLookupPort;
    private final FileStoragePort fileStoragePort;
    private final PdfPageCounterPort pdfPageCounterPort;

    @Override
    public CompanyDocumentUploadStartResult startUpload(StartCompanyDocumentUploadCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();
        validateInput(command);
        String extension = extractExtension(command.originalFileName());

        long documentId;
        int versionNo;
        if (command.companyDocumentId() == null) {
            DocumentCategory category = parseCategory(command.category());
            String docName = resolveDocumentName(command.name(), command.originalFileName());
            CompanyDocument saved = documentRepository.save(
                    CompanyDocument.create(companyId, category, docName, command.requesterUserId()));
            documentId = saved.getCompanyDocumentId();
            versionNo = 1;
        } else {
            // 회사 스코프 검증 — 타 회사 문서 ID 를 주면 CDOC_NOT_FOUND(존재를 노출하지 않는다).
            CompanyDocument document = requireOwnedDocument(command.companyDocumentId(), companyId);
            documentId = document.getCompanyDocumentId();
            versionNo = versionRepository.findMaxVersionNo(documentId) + 1;
        }

        // ⚠️ file 과의 핵심 차이 — 업로더 조회가 비어도 예외를 던지지 않는다. 사내 문서는 ADMIN 이 올리는데
        //    ADMIN 은 employee 행이 없어 조회가 빈다(§6-6). 이름/부서/직책은 null 로 두고 uploadedBy(사번)만 기록한다.
        UploaderLookupPort.UploaderSnapshot uploader =
                uploaderLookupPort.findByUserId(command.requesterUserId()).orElse(null);
        String uploaderName = uploader == null ? null : uploader.name();
        String uploaderDepartment = uploader == null ? null : uploader.department();
        String uploaderPosition = uploader == null ? null : uploader.position();

        String storageKey = storageKeyBuilder.build(companyId, documentId, versionNo, extension);

        CompanyDocumentVersion version = versionRepository.save(CompanyDocumentVersion.startUpload(
                documentId, versionNo, storageKey,
                command.originalFileName(), extension, command.mimeType(), command.sizeBytes(),
                command.comment(), command.requesterUserId(),
                uploaderName, uploaderDepartment, uploaderPosition));

        FileStoragePort.PresignedUrl presigned =
                fileStoragePort.presignUpload(storageKey, command.mimeType(), command.sizeBytes());

        return new CompanyDocumentUploadStartResult(
                documentId, version.getVersionId(), versionNo, presigned.url(), presigned.expiresAt());
    }

    @Override
    public CompanyDocumentVersionDetailResult completeUpload(CompleteCompanyDocumentUploadCommand command) {
        adminPolicy.assertAdmin(command.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();

        CompanyDocumentVersion version = versionRepository.findById(command.versionId())
                .orElseThrow(() -> new NotFoundException(CompanyDocumentErrorCode.CDOC_VERSION_NOT_FOUND));

        // 회사 스코프 — 버전이 속한 문서가 현재 회사 것인지 확인한다(타 회사 버전은 존재를 노출하지 않는다).
        CompanyDocument document = requireOwnedDocument(version.getCompanyDocumentId(), companyId);

        // UPLOADING 이 아니면 거부 — 이미 COMPLETED 는 중복 통보, FAILED 는 되살리기 시도다(둘 다 종료 상태).
        if (!version.isUploading()) {
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_ALREADY_COMPLETED);
        }

        // ⚠️ 실패 전이는 별도 트랜잭션(REQUIRES_NEW)으로 확정한다 — 여기서 예외를 던지면 이 서비스의
        //    @Transactional 이 롤백돼 인라인 save 가 사라진다(버전이 UPLOADING 으로 남는다). file 과 대칭.
        FileStoragePort.StoredObject stored = fileStoragePort.head(version.getStorageKey()).orElse(null);
        if (stored == null) {
            failureRecorder.markFailed(version);
            throw new ConflictException(CompanyDocumentErrorCode.CDOC_OBJECT_NOT_FOUND);
        }
        if (stored.sizeBytes() != version.getSizeBytes()) {
            failureRecorder.markFailed(version);
            throw new ConflictException(CompanyDocumentErrorCode.CDOC_SIZE_MISMATCH);
        }

        Integer pageCount = null;
        if (version.isPreviewable()) {
            pageCount = pdfPageCounterPort
                    .countPages(fileStoragePort.getObject(version.getStorageKey()))
                    .orElse(null);
        }

        version.complete(stored.sizeBytes(), command.checksum(), pageCount, LocalDateTime.now());
        CompanyDocumentVersion saved = versionRepository.save(version);
        indexTriggerPort.triggerIndexing(saved.getVersionId());

        return toDetail(document, saved);
    }

    private CompanyDocumentVersionDetailResult toDetail(CompanyDocument doc, CompanyDocumentVersion v) {
        return new CompanyDocumentVersionDetailResult(
                doc.getCompanyDocumentId(), v.getVersionId(), v.getVersionNo(), doc.getName(),
                doc.getCategory().name(), v.getOriginalFileName(), v.getExtension(), v.getSizeBytes(),
                v.getPageCount(), v.getComment(), v.getUploaderName(), v.getUploaderDepartment(),
                v.getUploaderPosition(), v.getCompletedAt());
    }

    /** 현재 회사 소속이며 삭제되지 않은 문서를 찾는다. 아니면 CDOC_NOT_FOUND. */
    private CompanyDocument requireOwnedDocument(Long documentId, long companyId) {
        return documentRepository.findById(documentId)
                .filter(d -> d.getCompanyId() == companyId && !d.isDeleted())
                .orElseThrow(() -> new NotFoundException(CompanyDocumentErrorCode.CDOC_NOT_FOUND));
    }

    private DocumentCategory parseCategory(String category) {
        if (!DocumentCategory.isValid(category)) {
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_INVALID_REQUEST);
        }
        return DocumentCategory.valueOf(category);
    }

    private void validateInput(StartCompanyDocumentUploadCommand command) {
        if (command.originalFileName() == null || command.originalFileName().isBlank()) {
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_INVALID_REQUEST);
        }
        if (command.sizeBytes() <= 0) {
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_INVALID_REQUEST);
        }
        if (command.sizeBytes() > MAX_SIZE_BYTES) {
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_SIZE_EXCEEDED);
        }
        if (BLOCKED_EXTENSIONS.contains(extractExtension(command.originalFileName()))) {
            throw new ValidationException(CompanyDocumentErrorCode.CDOC_EXTENSION_BLOCKED);
        }
    }

    /** 확장자(소문자, 점 제외). 없으면 빈 문자열. */
    private String extractExtension(String originalFileName) {
        int dot = originalFileName.lastIndexOf('.');
        if (dot < 0 || dot == originalFileName.length() - 1) {
            return "";
        }
        return originalFileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** 표시명 — 명시 name 이 있으면 그대로, 없으면 원본 파일명에서 확장자를 뗀 값(§1). */
    private String resolveDocumentName(String name, String originalFileName) {
        if (name != null && !name.isBlank()) {
            return name.strip();
        }
        int dot = originalFileName.lastIndexOf('.');
        return dot > 0 ? originalFileName.substring(0, dot) : originalFileName;
    }
}
