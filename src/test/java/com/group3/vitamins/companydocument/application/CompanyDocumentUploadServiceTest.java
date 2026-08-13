package com.group3.vitamins.companydocument.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.companydocument.application.command.CompleteCompanyDocumentUploadCommand;
import com.group3.vitamins.companydocument.application.command.StartCompanyDocumentUploadCommand;
import com.group3.vitamins.companydocument.application.policy.CompanyDocumentAdminPolicy;
import com.group3.vitamins.companydocument.application.port.CompanyDocumentIndexTriggerPort;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentUploadStartResult;
import com.group3.vitamins.companydocument.application.service.CompanyDocumentStorageKeyBuilder;
import com.group3.vitamins.companydocument.application.service.CompanyDocumentUploadService;
import com.group3.vitamins.companydocument.application.service.CompanyDocumentVersionFailureRecorder;
import com.group3.vitamins.companydocument.domain.exception.CompanyDocumentErrorCode;
import com.group3.vitamins.companydocument.domain.model.CompanyDocument;
import com.group3.vitamins.companydocument.domain.model.CompanyDocumentVersion;
import com.group3.vitamins.companydocument.domain.model.DocumentCategory;
import com.group3.vitamins.companydocument.domain.model.UploadStatus;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentRepository;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentVersionRepository;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.port.PdfPageCounterPort;
import com.group3.vitamins.file.application.port.UploaderLookupPort;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CompanyDocumentUploadService 사내 문서 업로드 2단계")
class CompanyDocumentUploadServiceTest {

    private static final long COMPANY_ID = 9L;
    private static final String USER = "EMP001";

    private CompanyDocumentRepository documentRepository;
    private CompanyDocumentVersionRepository versionRepository;
    private CompanyDocumentVersionFailureRecorder failureRecorder;
    private CompanyDocumentIndexTriggerPort indexTriggerPort;
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private UploaderLookupPort uploaderLookupPort;
    private FileStoragePort fileStoragePort;
    private PdfPageCounterPort pdfPageCounterPort;
    private CompanyDocumentUploadService service;

    @BeforeEach
    void setUp() {
        documentRepository = Mockito.mock(CompanyDocumentRepository.class);
        versionRepository = Mockito.mock(CompanyDocumentVersionRepository.class);
        failureRecorder = Mockito.mock(CompanyDocumentVersionFailureRecorder.class);
        indexTriggerPort = Mockito.mock(CompanyDocumentIndexTriggerPort.class);
        currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        uploaderLookupPort = Mockito.mock(UploaderLookupPort.class);
        fileStoragePort = Mockito.mock(FileStoragePort.class);
        pdfPageCounterPort = Mockito.mock(PdfPageCounterPort.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        // 실제 정책·키빌더를 써서 role 판정·키 규약까지 통합 검증한다.
        service = new CompanyDocumentUploadService(
                new CompanyDocumentAdminPolicy(), documentRepository, versionRepository,
                new CompanyDocumentStorageKeyBuilder(), failureRecorder, indexTriggerPort,
                currentCompanyIdProvider, uploaderLookupPort, fileStoragePort, pdfPageCounterPort);
    }

    private void stubSaves(long docId, long versionId) {
        when(documentRepository.save(any())).thenReturn(
                CompanyDocument.restore(docId, COMPANY_ID, DocumentCategory.FINANCE, "재무제표", USER, null));
        when(versionRepository.save(any())).thenAnswer(inv -> {
            CompanyDocumentVersion v = inv.getArgument(0);
            return CompanyDocumentVersion.restore(versionId, docId, v.getVersionNo(), v.getUploadStatus(),
                    v.getStorageKey(), v.getOriginalFileName(), v.getExtension(), v.getMimeType(), v.getSizeBytes(),
                    v.getChecksum(), v.getPageCount(), v.getComment(), v.getUploadedBy(), v.getUploaderName(),
                    v.getUploaderDepartment(), v.getUploaderPosition(), v.getCompletedAt(), v.getDeletedAt());
        });
        when(fileStoragePort.presignUpload(anyString(), any(), anyLong()))
                .thenReturn(new FileStoragePort.PresignedUrl("https://up", Instant.parse("2026-08-13T00:10:00Z")));
    }

    private void expectCode(Runnable r, Object code) {
        assertThatThrownBy(r::run).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(code);
    }

    @Test
    @DisplayName("새 문서 업로드 시작 — ADMIN 이면 문서/버전 생성 + presigned 발급, 회사 프리픽스 키")
    void startNewDocumentAsAdmin() {
        stubSaves(12L, 34L);
        when(uploaderLookupPort.findByUserId(USER)).thenReturn(
                Optional.of(new UploaderLookupPort.UploaderSnapshot("박지영", "경영지원팀", "팀장")));

        CompanyDocumentUploadStartResult result = service.startUpload(new StartCompanyDocumentUploadCommand(
                "FINANCE", "2026_재무제표.pdf", 1024L, "application/pdf", null, null, "1분기", USER, "ADMIN"));

        assertThat(result.companyDocumentId()).isEqualTo(12L);
        assertThat(result.versionId()).isEqualTo(34L);
        assertThat(result.versionNo()).isEqualTo(1);
        assertThat(result.uploadUrl()).isEqualTo("https://up");

        ArgumentCaptor<CompanyDocumentVersion> cap = ArgumentCaptor.forClass(CompanyDocumentVersion.class);
        verify(versionRepository).save(cap.capture());
        assertThat(cap.getValue().getStorageKey())
                .startsWith("companies/" + COMPANY_ID + "/documents/12/versions/1/");
        assertThat(cap.getValue().getUploaderName()).isEqualTo("박지영");
    }

    @Test
    @DisplayName("업로더가 employee 가 아니어도(§6-6) 예외 없이 null 스냅샷으로 진행한다")
    void startWithMissingUploaderSnapshot() {
        stubSaves(12L, 34L);
        when(uploaderLookupPort.findByUserId(USER)).thenReturn(Optional.empty());

        service.startUpload(new StartCompanyDocumentUploadCommand(
                "FINANCE", "a.pdf", 1024L, "application/pdf", null, null, null, USER, "ADMIN"));

        ArgumentCaptor<CompanyDocumentVersion> cap = ArgumentCaptor.forClass(CompanyDocumentVersion.class);
        verify(versionRepository).save(cap.capture());
        assertThat(cap.getValue().getUploaderName()).isNull();
        assertThat(cap.getValue().getUploadedBy()).isEqualTo(USER);
    }

    @Test
    @DisplayName("비 ADMIN 은 ACC_ADMIN_REQUIRED, 아무것도 저장하지 않는다")
    void startRejectsNonAdmin() {
        expectCode(() -> service.startUpload(new StartCompanyDocumentUploadCommand(
                "FINANCE", "a.pdf", 1024L, null, null, null, null, USER, "MEMBER")),
                AccountErrorCode.ACC_ADMIN_REQUIRED);
        verify(documentRepository, never()).save(any());
        verify(versionRepository, never()).save(any());
    }

    @Test
    @DisplayName("새 문서인데 카테고리가 유효하지 않으면 CDOC_INVALID_REQUEST")
    void startRejectsInvalidCategory() {
        expectCode(() -> service.startUpload(new StartCompanyDocumentUploadCommand(
                "UNKNOWN", "a.pdf", 1024L, null, null, null, null, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_INVALID_REQUEST);
    }

    @Test
    @DisplayName("50MB 초과는 CDOC_SIZE_EXCEEDED")
    void startRejectsOversize() {
        expectCode(() -> service.startUpload(new StartCompanyDocumentUploadCommand(
                "FINANCE", "a.pdf", 51L * 1024 * 1024, null, null, null, null, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("정확히 50MB 는 허용된다(경계 > vs >= 회귀 방지)")
    void acceptsExactly50MiB() {
        stubSaves(12L, 34L);
        when(uploaderLookupPort.findByUserId(USER)).thenReturn(Optional.empty());

        service.startUpload(new StartCompanyDocumentUploadCommand(
                "FINANCE", "a.pdf", 50L * 1024 * 1024, "application/pdf", null, null, null, USER, "ADMIN"));

        verify(versionRepository).save(any());
    }

    @Test
    @DisplayName("새 버전인데 대상 문서가 타 회사면 CDOC_NOT_FOUND")
    void startNewVersionRejectsOtherCompany() {
        when(documentRepository.findById(77L)).thenReturn(Optional.of(
                CompanyDocument.restore(77L, 999L, DocumentCategory.ETC, "남의문서", "X", null)));

        expectCode(() -> service.startUpload(new StartCompanyDocumentUploadCommand(
                null, "a.pdf", 1024L, null, null, 77L, null, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_NOT_FOUND);
    }

    @Test
    @DisplayName("완료 통보 — 저장소에 객체가 없으면 markFailed + CDOC_OBJECT_NOT_FOUND")
    void completeMissingObjectFails() {
        CompanyDocumentVersion uploading = CompanyDocumentVersion.restore(34L, 12L, 1, UploadStatus.UPLOADING,
                "key", "a.pdf", "pdf", "application/pdf", 1024L, null, null, null, USER, null, null, null, null, null);
        when(versionRepository.findById(34L)).thenReturn(Optional.of(uploading));
        when(documentRepository.findById(12L)).thenReturn(Optional.of(
                CompanyDocument.restore(12L, COMPANY_ID, DocumentCategory.FINANCE, "재무", USER, null)));
        when(fileStoragePort.head("key")).thenReturn(Optional.empty());

        expectCode(() -> service.completeUpload(new CompleteCompanyDocumentUploadCommand(34L, null, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_OBJECT_NOT_FOUND);
        verify(failureRecorder).markFailed(uploading);
        verify(indexTriggerPort, never()).triggerIndexing(any());
    }

    @Test
    @DisplayName("완료 통보 — 성공하면 COMPLETED 저장 + 인덱싱 트리거")
    void completeSuccessTriggersIndexing() {
        CompanyDocumentVersion uploading = CompanyDocumentVersion.restore(34L, 12L, 1, UploadStatus.UPLOADING,
                "key", "a.pdf", "pdf", "application/pdf", 1024L, null, null, null, USER, null, null, null, null, null);
        when(versionRepository.findById(34L)).thenReturn(Optional.of(uploading));
        when(documentRepository.findById(12L)).thenReturn(Optional.of(
                CompanyDocument.restore(12L, COMPANY_ID, DocumentCategory.FINANCE, "재무", USER, null)));
        when(fileStoragePort.head("key")).thenReturn(Optional.of(new FileStoragePort.StoredObject(1024L)));
        when(fileStoragePort.getObject("key")).thenReturn(new byte[]{1});
        when(pdfPageCounterPort.countPages(any())).thenReturn(Optional.of(5));
        when(versionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.completeUpload(new CompleteCompanyDocumentUploadCommand(34L, "abc", USER, "ADMIN"));

        // AI 합의 페이로드(versionId·companyId·s3Key)가 그대로 실려야 한다.
        verify(indexTriggerPort).triggerIndexing(argThat(t ->
                t.companyDocumentVersionId().equals(34L)
                        && t.companyId().equals(COMPANY_ID)
                        && t.s3Key().equals("key")));
        assertThat(uploading.isCompleted()).isTrue();
        assertThat(uploading.getPageCount()).isEqualTo(5);
    }
}
