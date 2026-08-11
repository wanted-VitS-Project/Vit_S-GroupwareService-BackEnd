package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * File 도메인(김동현님 소관) 실 연동. File 코드가 실제로 존재하게 된 뒤로는 스텁을 유지할 이유가 없어
 * 정식 리포지토리를 직접 참조한다(`ApprovalBlockCatalogAdapter`와 동일 패턴).
 *
 * <p>{@code uploadedAt}은 완료 시점({@code completedAt})을 쓴다 — {@code UPLOADING}/{@code FAILED}면
 * 아직 완료되지 않았으니 자연스럽게 {@code null}이다.
 *
 * <p>휴지통 여부는 {@code file_version}이 아니라 <b>{@code file}</b>을 봐야 한다(DEL-010 · D-6) —
 * 휴지통 이동·복구가 문서 단위라서다. 그래서 버전을 찾은 뒤 소유 문서를 한 번 더 조회한다.
 * 문서 수만큼 조회가 늘지만 한 회차의 첨부는 소수라 그대로 둔다.
 */
@Component
@RequiredArgsConstructor
public class ApprovalFileCatalogAdapter implements FileCatalogPort {

    private final FileVersionRepository fileVersionRepository;
    private final FileRepository fileRepository;

    @Override
    public Optional<FileVersionSummary> findFileVersion(Long fileVersionId) {
        return fileVersionRepository.findById(fileVersionId)
                .map(version -> new FileVersionSummary(
                        version.getFileVersionId(), version.getUploadStatus().name(),
                        version.getOriginalFileName(), version.getSizeBytes(), version.getCompletedAt(),
                        isOwnerFileDeleted(version.getFileId())));
    }

    /** 소유 문서가 휴지통에 있으면 true. 문서를 못 찾으면 판단할 근거가 없으니 false 로 둔다. */
    private boolean isOwnerFileDeleted(Long fileId) {
        return fileRepository.findById(fileId)
                .map(File::isDeleted)
                .orElse(false);
    }
}
