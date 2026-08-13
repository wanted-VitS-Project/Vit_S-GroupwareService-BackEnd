package com.group3.vitamins.file.application.service;

import com.group3.vitamins.file.application.port.FileIndexTriggerPort;
import com.group3.vitamins.file.application.port.UploaderLookupPort;
import com.group3.vitamins.file.application.result.AttachStagedFileResult;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 귀속(§2-G)의 DB 트랜잭션 경계 — 오케스트레이터({@link AttachStagedFileService})와 <b>별도 빈</b>으로 둔다.
 *
 * <p>귀속은 <b>2개의 커밋된 트랜잭션</b>으로 나뉜다(결정 B). 이유: 단일 트랜잭션이면 복사 실패 시
 * {@code FAILED} 전이가 롤백돼 감사 흔적이 사라진다({@link FileVersionFailureRecorder} 는 선커밋 행에만 동작).
 * <ol>
 *   <li>{@link #prepareOrResume} — 멱등 조회 + (없으면) File+FileVersion(UPLOADING) 생성 후 커밋</li>
 *   <li>(그 사이 오케스트레이터가 S3 복사·검증)</li>
 *   <li>{@link #completeAndIndex} — COMPLETED 전이 + 인덱싱 트리거 후 커밋</li>
 * </ol>
 * self-invocation 으로는 propagation 이 안 걸리므로 오케스트레이터가 이 빈의 메서드를 프록시 경유로 호출한다.
 */
@Component
@RequiredArgsConstructor
public class AttachStagedFileTxSupport {

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final StorageKeyBuilder storageKeyBuilder;
    private final FileIndexTriggerPort fileIndexTriggerPort;

    /** 귀속 준비/재개 결과 — 대상 버전과, 이미 완료된 버전(재시도)인지 여부. */
    public record Prepared(FileVersion version, boolean alreadyCompleted) {
    }

    /**
     * 멱등키로 기존 귀속을 찾고, 없으면 새 문서 + UPLOADING 버전을 만든다(커밋).
     * <p>동시 경합(같은 멱등키 동시 INSERT)은 UNIQUE 위반으로 이 트랜잭션이 롤백된다 —
     * 오케스트레이터가 {@link #findByIdempotencyKey} 로 승자 행을 재조회한다.
     */
    @Transactional
    public Prepared prepareOrResume(long companyId, long projectId, String requesterUserId,
                                    String originalFileName, long sizeBytes, String name, String comment,
                                    String extension, String mimeType, String idempotencyKey,
                                    UploaderLookupPort.UploaderSnapshot uploader) {
        Optional<FileVersion> existing = fileVersionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            FileVersion v = existing.get();
            return new Prepared(v, v.isCompleted());
        }

        // 항상 새 문서로 귀속(PROMOTE-009). block 링크가 없어 이름 중복 검사는 하지 않는다.
        File file = fileRepository.save(File.create(projectId, resolveDocumentName(name, originalFileName), requesterUserId));
        long fileId = file.getFileId();
        int versionNo = 1;
        String storageKey = storageKeyBuilder.build(companyId, projectId, fileId, versionNo, extension);

        FileVersion saved = fileVersionRepository.save(FileVersion.startUpload(
                fileId, versionNo, storageKey, originalFileName, extension, mimeType, sizeBytes, comment,
                requesterUserId, uploader.name(), uploader.department(), uploader.position(), idempotencyKey));
        return new Prepared(saved, false);
    }

    /** 멱등키로 기존 귀속을 별도 트랜잭션에서 조회한다(경합 흡수용). */
    @Transactional(readOnly = true)
    public Optional<Prepared> findByIdempotencyKey(String idempotencyKey) {
        return fileVersionRepository.findByIdempotencyKey(idempotencyKey)
                .map(v -> new Prepared(v, v.isCompleted()));
    }

    /** COMPLETED 전이 + 인덱싱 트리거(커밋). 이미 완료된 버전이면 그대로 결과만 만든다(멱등). */
    @Transactional
    public AttachStagedFileResult completeAndIndex(Long fileVersionId, long verifiedSizeBytes,
                                                   String checksum, Integer pageCount) {
        FileVersion version = fileVersionRepository.findById(fileVersionId)
                .orElseThrow(() -> new NotFoundException(FileErrorCode.FILE_VERSION_NOT_FOUND));
        if (!version.isCompleted()) {
            version.complete(verifiedSizeBytes, checksum, pageCount, LocalDateTime.now());
            version = fileVersionRepository.save(version);
            fileIndexTriggerPort.triggerIndexing(version.getFileVersionId());
        }
        return new AttachStagedFileResult(version.getFileId(), version.getFileVersionId(),
                version.getVersionNo(), AttachStagedFileResult.INDEX_PENDING);
    }

    /** 표시명 — 명시 name 이 있으면 그대로, 없으면 원본 파일명에서 확장자를 뗀 값(VER-004). */
    private String resolveDocumentName(String name, String originalFileName) {
        if (name != null && !name.isBlank()) {
            return name.strip();
        }
        int dot = originalFileName.lastIndexOf('.');
        return dot > 0 ? originalFileName.substring(0, dot) : originalFileName;
    }
}
