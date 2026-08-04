package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * File 도메인(김동현님 소관) 연동 지점. {@code file_version} 조회 인프라가 아직 없어
 * 임시로 항상 존재·업로드 완료(COMPLETED)로 간주한다
 * (`ApprovalBlockCatalogAdapter`와 동일한 임시 처리 — 상세 필드는 알 수 없어 null).
 */
@Component
public class ApprovalFileCatalogAdapter implements FileCatalogPort {

    private static final String COMPLETED = "COMPLETED";

    @Override
    public Optional<FileVersionSummary> findFileVersion(Long fileVersionId) {
        // TODO: 공용 file_version 테이블 조회 인프라가 아직 없어 임시로 항상 COMPLETED 로 간주한다.
        return Optional.of(new FileVersionSummary(fileVersionId, COMPLETED, null, null, null));
    }
}
