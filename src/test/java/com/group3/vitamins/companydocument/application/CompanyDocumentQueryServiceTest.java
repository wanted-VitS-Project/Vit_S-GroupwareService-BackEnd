package com.group3.vitamins.companydocument.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.companydocument.application.policy.CompanyDocumentAdminPolicy;
import com.group3.vitamins.companydocument.application.port.CompanyDocumentQueryPort;
import com.group3.vitamins.companydocument.application.query.CompanyDocumentListCriteria;
import com.group3.vitamins.companydocument.application.query.CompanyDocumentListQuery;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentListItemResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentListProjection;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentPageResult;
import com.group3.vitamins.companydocument.application.service.CompanyDocumentQueryService;
import com.group3.vitamins.companydocument.domain.exception.CompanyDocumentErrorCode;
import com.group3.vitamins.companydocument.domain.model.CompanyDocument;
import com.group3.vitamins.companydocument.domain.model.CompanyDocumentVersion;
import com.group3.vitamins.companydocument.domain.model.DocumentCategory;
import com.group3.vitamins.companydocument.domain.model.UploadStatus;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentRepository;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentVersionRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.port.PdfPreviewPort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CompanyDocumentQueryService 사내 문서 조회")
class CompanyDocumentQueryServiceTest {

    private static final long COMPANY_ID = 9L;
    private static final String USER = "EMP001";

    private CompanyDocumentQueryPort queryPort;
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private CompanyDocumentRepository documentRepository;
    private CompanyDocumentVersionRepository versionRepository;
    private FileStoragePort fileStoragePort;
    private PdfPreviewPort pdfPreviewPort;
    private CompanyDocumentQueryService service;

    @BeforeEach
    void setUp() {
        queryPort = Mockito.mock(CompanyDocumentQueryPort.class);
        currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        documentRepository = Mockito.mock(CompanyDocumentRepository.class);
        versionRepository = Mockito.mock(CompanyDocumentVersionRepository.class);
        fileStoragePort = Mockito.mock(FileStoragePort.class);
        pdfPreviewPort = Mockito.mock(PdfPreviewPort.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        service = new CompanyDocumentQueryService(
                new CompanyDocumentAdminPolicy(), currentCompanyIdProvider, queryPort,
                documentRepository, versionRepository, fileStoragePort, pdfPreviewPort);
    }

    private CompanyDocumentListProjection projection(String ext) {
        return new CompanyDocumentListProjection(
                12L, "FINANCE", "재무제표", 34L, 2, 2, "a." + ext, ext, 1024L,
                "박지영", "경영지원팀", "팀장", LocalDateTime.now());
    }

    private void expectCode(Runnable r, Object code) {
        assertThatThrownBy(r::run).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(code);
    }

    @Test
    @DisplayName("목록 — ADMIN 이면 회사 스코프 페이지 조회, previewable 파생, total 반영")
    void listAsAdmin() {
        when(queryPort.countDocuments(any())).thenReturn(42L);
        when(queryPort.findDocuments(any())).thenReturn(List.of(projection("pdf"), projection("xlsx")));

        CompanyDocumentPageResult result = service.getDocuments(
                new CompanyDocumentListQuery(USER, "ADMIN", "FINANCE", "재무", 1, 20));

        assertThat(result.totalElements()).isEqualTo(42);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.content()).extracting(CompanyDocumentListItemResult::previewable)
                .containsExactly(true, false);

        ArgumentCaptor<CompanyDocumentListCriteria> cap = ArgumentCaptor.forClass(CompanyDocumentListCriteria.class);
        verify(queryPort).findDocuments(cap.capture());
        assertThat(cap.getValue().companyId()).isEqualTo(COMPANY_ID);
        assertThat(cap.getValue().offset()).isEqualTo(20);
        assertThat(cap.getValue().category()).isEqualTo("FINANCE");
    }

    @Test
    @DisplayName("목록 — 비 ADMIN 은 ACC_ADMIN_REQUIRED, 조회 안 함")
    void listRejectsNonAdmin() {
        expectCode(() -> service.getDocuments(new CompanyDocumentListQuery(USER, "MEMBER", null, null, 0, 20)),
                AccountErrorCode.ACC_ADMIN_REQUIRED);
        verify(queryPort, never()).findDocuments(any());
    }

    @Test
    @DisplayName("목록 — page 음수는 0, size 상한 100 으로 정규화하고 offset overflow 없음")
    void listClampsPaging() {
        when(queryPort.countDocuments(any())).thenReturn(0L);
        when(queryPort.findDocuments(any())).thenReturn(List.of());

        service.getDocuments(new CompanyDocumentListQuery(USER, "ADMIN", null, null, Integer.MAX_VALUE, 500));

        ArgumentCaptor<CompanyDocumentListCriteria> cap = ArgumentCaptor.forClass(CompanyDocumentListCriteria.class);
        verify(queryPort).findDocuments(cap.capture());
        assertThat(cap.getValue().limit()).isEqualTo(100);
        assertThat(cap.getValue().offset()).isEqualTo((long) Integer.MAX_VALUE * 100);
        assertThat(cap.getValue().offset()).isPositive();
    }

    @Test
    @DisplayName("다운로드 — 미완료 버전은 CDOC_UPLOAD_NOT_COMPLETED")
    void downloadRejectsIncomplete() {
        CompanyDocumentVersion uploading = version(UploadStatus.UPLOADING, "a.pdf");
        when(versionRepository.findById(34L)).thenReturn(Optional.of(uploading));
        when(documentRepository.findById(12L)).thenReturn(Optional.of(ownedDoc()));

        expectCode(() -> service.getDownloadUrl(34L, USER, "ADMIN"),
                CompanyDocumentErrorCode.CDOC_UPLOAD_NOT_COMPLETED);
    }

    @Test
    @DisplayName("다운로드 — 타 회사 버전은 CDOC_VERSION_NOT_FOUND(존재 미노출)")
    void downloadRejectsOtherCompany() {
        when(versionRepository.findById(34L)).thenReturn(Optional.of(version(UploadStatus.COMPLETED, "a.pdf")));
        when(documentRepository.findById(12L)).thenReturn(Optional.of(
                CompanyDocument.restore(12L, 999L, DocumentCategory.ETC, "남", "X", null)));

        expectCode(() -> service.getDownloadUrl(34L, USER, "ADMIN"),
                CompanyDocumentErrorCode.CDOC_VERSION_NOT_FOUND);
    }

    @Test
    @DisplayName("미리보기 — PDF 아니면 CDOC_PREVIEW_NOT_SUPPORTED")
    void previewRejectsNonPdf() {
        when(versionRepository.findById(34L)).thenReturn(Optional.of(version(UploadStatus.COMPLETED, "a.xlsx")));
        when(documentRepository.findById(12L)).thenReturn(Optional.of(ownedDoc()));

        expectCode(() -> service.getPreview(34L, USER, "ADMIN"),
                CompanyDocumentErrorCode.CDOC_PREVIEW_NOT_SUPPORTED);
    }

    @Test
    @DisplayName("버전 이력 — 문서 없으면 CDOC_NOT_FOUND")
    void versionHistoryMissingDocument() {
        when(documentRepository.findById(12L)).thenReturn(Optional.empty());

        expectCode(() -> service.getVersionHistory(12L, USER, "ADMIN"),
                CompanyDocumentErrorCode.CDOC_NOT_FOUND);
    }

    @Test
    @DisplayName("미리보기 — S3/PDF 처리 중 런타임 예외는 CDOC_PREVIEW_FAILED(500)로 변환")
    void previewFailedOnRuntimeError() {
        when(versionRepository.findById(34L)).thenReturn(Optional.of(version(UploadStatus.COMPLETED, "a.pdf")));
        when(documentRepository.findById(12L)).thenReturn(Optional.of(ownedDoc()));
        when(fileStoragePort.getObject("key")).thenThrow(new RuntimeException("s3 down"));

        expectCode(() -> service.getPreview(34L, USER, "ADMIN"),
                CompanyDocumentErrorCode.CDOC_PREVIEW_FAILED);
    }

    private CompanyDocument ownedDoc() {
        return CompanyDocument.restore(12L, COMPANY_ID, DocumentCategory.FINANCE, "재무", USER, null);
    }

    private CompanyDocumentVersion version(UploadStatus status, String originalName) {
        String ext = originalName.substring(originalName.lastIndexOf('.') + 1);
        return CompanyDocumentVersion.restore(34L, 12L, 1, status, "key", originalName, ext,
                "application/pdf", 1024L, null, null, null, USER, null, null, null,
                status == UploadStatus.COMPLETED ? LocalDateTime.now() : null, null);
    }
}
