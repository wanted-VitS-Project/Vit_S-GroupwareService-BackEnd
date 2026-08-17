package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.port.FileVersionSummary;
import com.group3.vitamins.approval.infrastructure.persistence.mapper.ApprovalQueryMapper;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalFileVersionRow;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * File 도메인(김동현님 소관) 실 연동. File 코드가 실제로 존재하게 된 뒤로는 스텁을 유지할 이유가 없어
 * 정식 리포지토리를 직접 참조한다(`ApprovalBlockCatalogAdapter`와 동일 패턴).
 *
 * <p>{@code uploadedAt}은 완료 시점({@code completedAt})을 쓴다 — {@code UPLOADING}/{@code FAILED}면
 * 아직 완료되지 않았으니 자연스럽게 {@code null}이다.
 *
 * <p>휴지통 여부는 {@code file_version}이 아니라 <b>{@code file}</b>을 봐야 한다(DEL-010 · D-6) —
 * 휴지통 이동·복구가 문서 단위라서다. 그래서 단건 조회는 버전을 찾은 뒤 소유 문서를 한 번 더 조회한다.
 *
 * <p>목록 경로는 {@link #findFileVersions(Collection)} 로 <b>조인 1발</b>을 쓴다. 예전에는 상세조회가
 * 첨부마다 단건 조회를 돌려 첨부당 2쿼리가 나갔다(첨부 5개 = 10쿼리). 단건 메서드를 남겨둔 것은
 * 문서 추가·제거(APR-005·007)가 실제로 대상이 한 건이기 때문이지, 목록에서 써도 된다는 뜻이 아니다.
 */
@Component
@RequiredArgsConstructor
public class ApprovalFileCatalogAdapter implements FileCatalogPort {

    private final FileVersionRepository fileVersionRepository;
    private final FileRepository fileRepository;
    private final ApprovalQueryMapper approvalQueryMapper;

    /**
     * 상세조회 첨부 목록용 배치 조회 — 첨부 수와 무관하게 쿼리 1발(단건 반복은 첨부당 2쿼리였다).
     *
     * <p>⚠️ <b>빈 컬렉션이면 쿼리를 보내지 않고 즉시 빈 맵이다.</b> MyBatis {@code <foreach>} 가
     * {@code IN ()} 을 만들어 SQL 문법 오류가 나는데, 첨부 0건은 초안 회차의 정상 상태라
     * 여기서 막지 않으면 <b>문서를 아직 안 붙인 결재의 상세조회가 통째로 500</b> 이 된다.
     *
     * <p>같은 {@code fileVersionId} 가 중복으로 들어와도 병합 함수로 방어한다 — 같은 버전이면
     * 내용도 같으니 어느 쪽을 남겨도 결과가 같다.
     */
    @Override
    public Map<Long, FileVersionSummary> findFileVersions(Collection<Long> fileVersionIds) {
        if (fileVersionIds == null || fileVersionIds.isEmpty()) {
            return Map.of();
        }
        return approvalQueryMapper.findFileVersionsByIds(fileVersionIds).stream()
                .collect(Collectors.toMap(ApprovalFileVersionRow::fileVersionId, this::toSummary,
                        (first, second) -> first, LinkedHashMap::new));
    }

    private FileVersionSummary toSummary(ApprovalFileVersionRow row) {
        return new FileVersionSummary(row.fileVersionId(), row.uploadStatus(), row.fileName(),
                row.fileSize(), row.uploadedAt(), row.fileDeleted());
    }

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
