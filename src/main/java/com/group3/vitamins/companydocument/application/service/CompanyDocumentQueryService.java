package com.group3.vitamins.companydocument.application.service;

import com.group3.vitamins.companydocument.application.policy.CompanyDocumentAdminPolicy;
import com.group3.vitamins.companydocument.application.port.CompanyDocumentQueryPort;
import com.group3.vitamins.companydocument.application.query.CompanyDocumentListCriteria;
import com.group3.vitamins.companydocument.application.query.CompanyDocumentListQuery;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentDownloadResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentListItemResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentPageResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentPreviewResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionHistoryResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentVersionProjection;
import com.group3.vitamins.companydocument.application.usecase.CompanyDocumentQueryUseCase;
import com.group3.vitamins.companydocument.domain.exception.CompanyDocumentErrorCode;
import com.group3.vitamins.companydocument.domain.exception.CompanyDocumentPreviewException;
import com.group3.vitamins.companydocument.domain.model.CompanyDocument;
import com.group3.vitamins.companydocument.domain.model.CompanyDocumentVersion;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentRepository;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentVersionRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.port.PdfPreviewPort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사내 문서 조회 서비스 (§3 목록 · §7 버전 이력 · §8 다운로드 · §9 미리보기). 읽기 전용. 전부 ADMIN 전용.
 *
 * <p>모든 조회에 회사 스코프를 강제한다(INV-02). 다운로드·미리보기는 versionId 로 진입해 소속 문서가 현재 회사 것인지,
 * 삭제되지 않았는지 검증한 뒤 저장소에 접근한다. 저장소·PDF 인프라는 file 도메인 포트를 재사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyDocumentQueryService implements CompanyDocumentQueryUseCase {

    /** 페이지 크기 상한(FILE-Q·ProjectQueryService 와 통일). */
    private static final int MAX_PAGE_SIZE = 100;

    /** 미리보기 최대 페이지 수(§9). */
    private static final int MAX_PREVIEW_PAGES = 5;

    private final CompanyDocumentAdminPolicy adminPolicy;
    private final CurrentCompanyIdProvider currentCompanyIdProvider;
    private final CompanyDocumentQueryPort queryPort;
    private final CompanyDocumentRepository documentRepository;
    private final CompanyDocumentVersionRepository versionRepository;
    private final FileStoragePort fileStoragePort;
    private final PdfPreviewPort pdfPreviewPort;

    @Override
    public CompanyDocumentPageResult getDocuments(CompanyDocumentListQuery query) {
        adminPolicy.assertAdmin(query.role());
        long companyId = currentCompanyIdProvider.currentCompanyId();
        int page = Math.max(query.page(), 0);
        int size = Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE);

        // ⚠️ (long) 캐스팅 — page 가 크면 int 곱셈이 음수로 넘쳐 잘못된 OFFSET 이 나간다(FILE-Q 선례).
        CompanyDocumentListCriteria criteria = new CompanyDocumentListCriteria(
                companyId, blankToNull(query.category()), blankToNull(query.keyword()),
                (long) page * size, size);

        long total = queryPort.countDocuments(criteria);
        List<CompanyDocumentListItemResult> content = queryPort.findDocuments(criteria).stream()
                .map(p -> CompanyDocumentListItemResult.from(p, isPreviewable(p.extension())))
                .toList();

        return new CompanyDocumentPageResult(content, page, size, total);
    }

    @Override
    public CompanyDocumentVersionHistoryResult getVersionHistory(Long companyDocumentId, String requesterUserId, String role) {
        adminPolicy.assertAdmin(role);
        long companyId = currentCompanyIdProvider.currentCompanyId();
        CompanyDocument document = requireOwnedDocument(companyDocumentId, companyId);

        List<CompanyDocumentVersionProjection> versions = queryPort.findCompletedVersions(companyDocumentId);
        // 차수 내림차순이라 첫 행이 최신. 비어 있으면 0.
        int latestVersionNo = versions.isEmpty() ? 0 : versions.get(0).versionNo();

        List<CompanyDocumentVersionHistoryResult.Item> items = versions.stream()
                .map(p -> new CompanyDocumentVersionHistoryResult.Item(
                        p.versionId(), p.versionNo(), p.versionNo() == latestVersionNo,
                        p.originalFileName(), p.extension(), p.sizeBytes(), p.pageCount(),
                        isPreviewable(p.extension()), p.comment(),
                        p.uploaderName(), p.uploaderDepartment(), p.uploaderPosition(), p.completedAt()))
                .toList();

        return new CompanyDocumentVersionHistoryResult(
                document.getCompanyDocumentId(), document.getName(), document.getCategory().name(),
                items.size(), items);
    }

    @Override
    public CompanyDocumentDownloadResult getDownloadUrl(Long versionId, String requesterUserId, String role) {
        adminPolicy.assertAdmin(role);
        long companyId = currentCompanyIdProvider.currentCompanyId();
        CompanyDocumentVersion version = requireOwnedVersion(versionId, companyId);

        if (!version.isCompleted()) {
            throw new ConflictException(CompanyDocumentErrorCode.CDOC_UPLOAD_NOT_COMPLETED);
        }

        FileStoragePort.PresignedUrl presigned =
                fileStoragePort.presignDownload(version.getStorageKey(), version.getOriginalFileName());

        return new CompanyDocumentDownloadResult(
                version.getVersionId(), version.getOriginalFileName(), version.getSizeBytes(),
                presigned.url(), presigned.expiresAt());
    }

    @Override
    public CompanyDocumentPreviewResult getPreview(Long versionId, String requesterUserId, String role) {
        adminPolicy.assertAdmin(role);
        long companyId = currentCompanyIdProvider.currentCompanyId();
        CompanyDocumentVersion version = requireOwnedVersion(versionId, companyId);

        if (!version.isCompleted()) {
            throw new ConflictException(CompanyDocumentErrorCode.CDOC_UPLOAD_NOT_COMPLETED);
        }
        if (!version.isPreviewable()) {
            throw new ConflictException(CompanyDocumentErrorCode.CDOC_PREVIEW_NOT_SUPPORTED);
        }

        // getObject 도 try 안에 둔다 — S3 조회 실패도 CDOC_PREVIEW_FAILED 로 변환해 원시 런타임 예외가 500 계열로 새는 것을 막는다.
        try {
            byte[] bytes = fileStoragePort.getObject(version.getStorageKey());
            PdfPreviewPort.Preview preview = pdfPreviewPort.render(bytes, MAX_PREVIEW_PAGES);
            return new CompanyDocumentPreviewResult(
                    preview.content(), preview.previewPageCount(), preview.totalPageCount());
        } catch (RuntimeException e) {
            throw new CompanyDocumentPreviewException(CompanyDocumentErrorCode.CDOC_PREVIEW_FAILED, e);
        }
    }

    /** 현재 회사 소속이며 삭제되지 않은 문서. 아니면 CDOC_NOT_FOUND. */
    private CompanyDocument requireOwnedDocument(Long companyDocumentId, long companyId) {
        return documentRepository.findById(companyDocumentId)
                .filter(d -> d.getCompanyId() == companyId && !d.isDeleted())
                .orElseThrow(() -> new NotFoundException(CompanyDocumentErrorCode.CDOC_NOT_FOUND));
    }

    /**
     * versionId 로 버전을 찾고, 소속 문서가 현재 회사 것이며 삭제되지 않았는지 검증한다.
     * 버전 없음·타 회사·문서 삭제 모두 CDOC_VERSION_NOT_FOUND 로 통일한다(존재를 노출하지 않는다).
     */
    private CompanyDocumentVersion requireOwnedVersion(Long versionId, long companyId) {
        CompanyDocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException(CompanyDocumentErrorCode.CDOC_VERSION_NOT_FOUND));
        documentRepository.findById(version.getCompanyDocumentId())
                .filter(d -> d.getCompanyId() == companyId && !d.isDeleted())
                .orElseThrow(() -> new NotFoundException(CompanyDocumentErrorCode.CDOC_VERSION_NOT_FOUND));
        return version;
    }

    private boolean isPreviewable(String extension) {
        return "pdf".equalsIgnoreCase(extension);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
