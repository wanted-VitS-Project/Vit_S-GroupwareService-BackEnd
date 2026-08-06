package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
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
 */
@Component
@RequiredArgsConstructor
public class ApprovalFileCatalogAdapter implements FileCatalogPort {

    private final FileVersionRepository fileVersionRepository;

    @Override
    public Optional<FileVersionSummary> findFileVersion(Long fileVersionId) {
        return fileVersionRepository.findById(fileVersionId)
                .map(file -> new FileVersionSummary(
                        file.getFileVersionId(), file.getUploadStatus().name(),
                        file.getOriginalFileName(), file.getSizeBytes(), file.getCompletedAt()));
    }
}
