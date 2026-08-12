package com.group3.vitamins.file.application.service;

import com.group3.vitamins.file.application.command.AttachStagedFileCommand;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.port.PdfPageCounterPort;
import com.group3.vitamins.file.application.port.UploaderLookupPort;
import com.group3.vitamins.file.application.result.AttachStagedFileResult;
import com.group3.vitamins.file.application.service.AttachStagedFileTxSupport.Prepared;
import com.group3.vitamins.file.application.usecase.AttachStagedFileUseCase;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 입찰 검토 파일 귀속 서비스 (FILE-V1 §2-G). 오케스트레이터 — <b>클래스 단위 {@code @Transactional} 없음</b>.
 *
 * <p>기존 2단계 업로드의 "완료 통보(FILE-013)"의 변형이다 — 클라이언트 PUT 자리에 서버측 S3 복사가 들어간다.
 * DB 커밋은 {@link AttachStagedFileTxSupport}(2-트랜잭션)에 맡기고, 그 사이 S3 복사·검증을 트랜잭션 밖에서 한다.
 *
 * <p>흐름: 업로더 조회 → 준비/재개(tx1) → S3 복사·head 검증 → PDF 페이지 수 → 완료·인덱싱(tx2).
 * 복사·검증 실패 시 선커밋된 버전을 {@link FileVersionFailureRecorder} 로 FAILED 확정한다.
 */
@Service
@RequiredArgsConstructor
public class AttachStagedFileService implements AttachStagedFileUseCase {

    private final AttachStagedFileTxSupport txSupport;
    private final UploaderLookupPort uploaderLookupPort;
    private final FileStoragePort fileStoragePort;
    private final PdfPageCounterPort pdfPageCounterPort;
    private final MimeTypeResolver mimeTypeResolver;
    private final FileVersionFailureRecorder failureRecorder;

    @Override
    public AttachStagedFileResult attach(AttachStagedFileCommand command) {
        // 1) 업로더 스냅샷은 requesterUserId 로 조회한다(PROMOTE-005). 실제 사원이 아니면 명확히 실패(PROMOTE-010).
        UploaderLookupPort.UploaderSnapshot uploader = uploaderLookupPort.findByUserId(command.requesterUserId())
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_REQUESTER_NOT_EMPLOYEE));

        // temporaryStorageKey 는 요청 회사(테넌트) 프리픽스여야 한다 — 다른 회사 S3 객체를 정식 키로 복사하는 것을 방어적으로 차단.
        // 임시 객체의 전체 소유·수명 검증은 입찰 도메인 소관(§2-G) — 여기선 멀티테넌시 경계(companies/{companyId}/)만 확인한다.
        requireTenantScopedTempKey(command.temporaryStorageKey(), command.companyId());

        String extension = extractExtension(command.originalFileName());
        String mimeType = mimeTypeResolver.resolve(extension);

        // 2) 준비/재개(tx1) — 멱등 조회 + (없으면) File+FileVersion(UPLOADING) 선커밋.
        //    동시 경합(같은 멱등키)은 UNIQUE 위반으로 tx1 이 롤백 → 승자 행을 재조회해 흡수한다.
        Prepared prepared;
        try {
            prepared = txSupport.prepareOrResume(
                    command.companyId(), command.projectId(), command.requesterUserId(),
                    command.originalFileName(), command.sizeBytes(), command.name(), command.comment(),
                    extension, mimeType, command.idempotencyKey(), uploader);
        } catch (DataIntegrityViolationException race) {
            // 멱등키 UNIQUE 경합만 흡수한다 — 승자 행이 있으면 그걸 쓴다. 승자가 없으면 이 DIVE 는 멱등 경합이 아니라
            // 무관한 제약 위반(FK·NOT NULL)이므로 원인을 삼키지 말고 원래 예외를 그대로 던진다.
            prepared = txSupport.findByIdempotencyKey(command.idempotencyKey())
                    .orElseThrow(() -> race);
        }

        FileVersion version = prepared.version();
        // 재시도로 이미 완료된 귀속이면 그대로 반환한다(멱등 · 결정 D).
        if (prepared.alreadyCompleted()) {
            return new AttachStagedFileResult(version.getFileId(), version.getFileVersionId(),
                    version.getVersionNo(), AttachStagedFileResult.INDEX_PENDING);
        }

        // 3) S3 복사(tx 밖) — 임시 키 → 정식 키. 원본 없음/권한 등 실패 시 버전 FAILED 확정.
        String storageKey = version.getStorageKey();
        try {
            fileStoragePort.copyObject(command.temporaryStorageKey(), storageKey);
        } catch (RuntimeException e) {
            failureRecorder.markFailed(version);
            throw new ConflictException(FileErrorCode.FILE_OBJECT_NOT_FOUND);
        }

        // 4) 복사 후 존재·크기 재검증(완료 통보와 대칭). checksum 은 head 가 제공하지 않아 저장만 한다(기존 업로드와 동일).
        FileStoragePort.StoredObject stored = fileStoragePort.head(storageKey).orElse(null);
        if (stored == null) {
            failureRecorder.markFailed(version);
            throw new ConflictException(FileErrorCode.FILE_OBJECT_NOT_FOUND);
        }
        if (stored.sizeBytes() != command.sizeBytes()) {
            failureRecorder.markFailed(version);
            throw new ConflictException(FileErrorCode.FILE_SIZE_MISMATCH);
        }

        // 5) PDF 페이지 수(실패해도 null · VER-008).
        Integer pageCount = null;
        if (version.isPreviewable()) {
            pageCount = pdfPageCounterPort.countPages(fileStoragePort.getObject(storageKey)).orElse(null);
        }

        // 6) 완료 + 인덱싱(tx2).
        return txSupport.completeAndIndex(version.getFileVersionId(), stored.sizeBytes(), command.checksum(), pageCount);
    }

    /** 임시 키가 요청 회사 프리픽스({@code companies/{companyId}/}) 아래인지 확인한다(테넌트 경계 방어). */
    private void requireTenantScopedTempKey(String temporaryStorageKey, long companyId) {
        String tenantPrefix = "companies/" + companyId + "/";
        if (temporaryStorageKey == null || !temporaryStorageKey.startsWith(tenantPrefix)) {
            throw new ValidationException(FileErrorCode.FILE_INVALID_REQUEST);
        }
    }

    /** 확장자(소문자, 점 제외). 없으면 빈 문자열. {@code FileUploadService} 와 동일 규약. */
    private String extractExtension(String originalFileName) {
        if (originalFileName == null) {
            return "";
        }
        int dot = originalFileName.lastIndexOf('.');
        if (dot < 0 || dot == originalFileName.length() - 1) {
            return "";
        }
        return originalFileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
