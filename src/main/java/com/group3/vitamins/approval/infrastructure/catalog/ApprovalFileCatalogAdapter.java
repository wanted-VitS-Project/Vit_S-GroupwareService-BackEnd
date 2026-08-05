package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * File 도메인(김동현님 소관) 연동 지점. {@code file_version} 조회 인프라가 아직 없다.
 *
 * <p>{@code approval_document.file_version_id}에 실제 FK가 걸려 있어서, 존재하지 않는 파일을
 * "항상 COMPLETED"로 우겨서 통과시키면 애플리케이션 검증은 지나가고 DB INSERT에서 FK 위반(500)으로
 * 터진다(실제로 겪은 버그, CodeRabbit 지적 반영). 그래서 이 스텁은 항상 "없음"으로 응답해
 * {@code getReadyFileVersionOrThrow}가 깨끗하게 404(`FILE_VERSION_NOT_FOUND`)로 막게 한다 —
 * File 도메인이 실제로 연동되기 전까지 문서 연결은 항상 실패하는 게 맞다.
 */
@Component
public class ApprovalFileCatalogAdapter implements FileCatalogPort {

    @Override
    public Optional<FileVersionSummary> findFileVersion(Long fileVersionId) {
        // TODO: 공용 file_version 테이블 조회 인프라가 실제로 붙으면 이 메서드를 진짜 조회로 교체한다.
        return Optional.empty();
    }
}
