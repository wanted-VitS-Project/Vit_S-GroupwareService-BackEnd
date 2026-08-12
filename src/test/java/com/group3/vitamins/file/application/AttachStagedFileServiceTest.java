package com.group3.vitamins.file.application;

import com.group3.vitamins.file.application.command.AttachStagedFileCommand;
import com.group3.vitamins.file.application.port.FileStoragePort;
import com.group3.vitamins.file.application.port.PdfPageCounterPort;
import com.group3.vitamins.file.application.port.UploaderLookupPort;
import com.group3.vitamins.file.application.result.AttachStagedFileResult;
import com.group3.vitamins.file.application.service.AttachStagedFileService;
import com.group3.vitamins.file.application.service.AttachStagedFileTxSupport;
import com.group3.vitamins.file.application.service.AttachStagedFileTxSupport.Prepared;
import com.group3.vitamins.file.application.service.FileVersionFailureRecorder;
import com.group3.vitamins.file.application.service.MimeTypeResolver;
import com.group3.vitamins.file.domain.exception.FileErrorCode;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.model.UploadStatus;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AttachStagedFileService 입찰 검토 파일 귀속(§2-G)")
class AttachStagedFileServiceTest {

    private static final long COMPANY = 1L;
    private static final long PROJECT = 100L;
    private static final String USER = "EMP001";
    private static final String TEMP_KEY = "companies/1/staging/bidrev-777/tmp.pdf";
    private static final String STORAGE_KEY = "companies/1/projects/100/files/31/versions/1/uuid.pdf";
    private static final String IDEM = "bidrev-777";

    private AttachStagedFileTxSupport txSupport;
    private UploaderLookupPort uploaderLookupPort;
    private FileStoragePort fileStoragePort;
    private PdfPageCounterPort pdfPageCounterPort;
    private FileVersionFailureRecorder failureRecorder;
    private AttachStagedFileService service;

    @BeforeEach
    void setUp() {
        txSupport = Mockito.mock(AttachStagedFileTxSupport.class);
        uploaderLookupPort = Mockito.mock(UploaderLookupPort.class);
        fileStoragePort = Mockito.mock(FileStoragePort.class);
        pdfPageCounterPort = Mockito.mock(PdfPageCounterPort.class);
        failureRecorder = Mockito.mock(FileVersionFailureRecorder.class);
        service = new AttachStagedFileService(
                txSupport, uploaderLookupPort, fileStoragePort, pdfPageCounterPort,
                new MimeTypeResolver(), failureRecorder);
    }

    private void stubUploader() {
        when(uploaderLookupPort.findByUserId(USER)).thenReturn(
                Optional.of(new UploaderLookupPort.UploaderSnapshot("이영희", "제안팀", "선임연구원")));
    }

    private void stubPrepare(Prepared prepared) {
        when(txSupport.prepareOrResume(anyLong(), anyLong(), any(), any(), anyLong(),
                any(), any(), any(), any(), any(), any())).thenReturn(prepared);
    }

    private AttachStagedFileCommand cmd() {
        return new AttachStagedFileCommand(COMPANY, PROJECT, USER, TEMP_KEY,
                "재정상태.pdf", 5000L, null, null, "AI 검토 첨부", true, IDEM);
    }

    private FileVersion uploadingVersion() {
        return FileVersion.restore(74L, 31L, 1, UploadStatus.UPLOADING, STORAGE_KEY,
                "재정상태.pdf", "pdf", "application/pdf", 5000L, null, null, "AI 검토 첨부",
                USER, "이영희", "제안팀", "선임연구원", null, null, IDEM);
    }

    private FileVersion completedVersion() {
        return FileVersion.restore(74L, 31L, 1, UploadStatus.COMPLETED, STORAGE_KEY,
                "재정상태.pdf", "pdf", "application/pdf", 5000L, null, 3, "AI 검토 첨부",
                USER, "이영희", "제안팀", "선임연구원", LocalDateTime.now(), null, IDEM);
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("정상 — 복사·검증 후 완료·인덱싱, 결과 반환")
    void attaches() {
        stubUploader();
        FileVersion version = uploadingVersion();
        stubPrepare(new Prepared(version, false));
        when(fileStoragePort.head(anyString())).thenReturn(Optional.of(new FileStoragePort.StoredObject(5000L)));
        when(fileStoragePort.getObject(anyString())).thenReturn(new byte[]{1, 2, 3});
        when(pdfPageCounterPort.countPages(any())).thenReturn(Optional.of(12));
        when(txSupport.completeAndIndex(74L, 5000L, null, 12))
                .thenReturn(new AttachStagedFileResult(31L, 74L, 1, AttachStagedFileResult.INDEX_PENDING));

        AttachStagedFileResult result = service.attach(cmd());

        assertThat(result.fileId()).isEqualTo(31L);
        assertThat(result.fileVersionId()).isEqualTo(74L);
        assertThat(result.versionNo()).isEqualTo(1);
        assertThat(result.indexStatus()).isEqualTo("PENDING");
        verify(fileStoragePort, times(1)).copyObject(TEMP_KEY, STORAGE_KEY);
        verify(txSupport, times(1)).completeAndIndex(74L, 5000L, null, 12);
    }

    @Test
    @DisplayName("요청자가 employee 가 아니면 FILE_REQUESTER_NOT_EMPLOYEE — 준비 자체를 안 한다")
    void requesterNotEmployee() {
        when(uploaderLookupPort.findByUserId(USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attach(cmd()))
                .satisfies(hasCode(FileErrorCode.FILE_REQUESTER_NOT_EMPLOYEE));
        verify(txSupport, never()).prepareOrResume(anyLong(), anyLong(), any(), any(), anyLong(),
                any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 완료된 귀속(멱등 재호출)이면 복사 없이 기존 결과 반환")
    void idempotentReplay() {
        stubUploader();
        stubPrepare(new Prepared(completedVersion(), true));

        AttachStagedFileResult result = service.attach(cmd());

        assertThat(result.fileVersionId()).isEqualTo(74L);
        assertThat(result.indexStatus()).isEqualTo("PENDING");
        verify(fileStoragePort, never()).copyObject(anyString(), anyString());
        verify(txSupport, never()).completeAndIndex(any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("복사 실패 시 버전을 FAILED 로 기록하고 FILE_OBJECT_NOT_FOUND")
    void copyFails() {
        stubUploader();
        FileVersion version = uploadingVersion();
        stubPrepare(new Prepared(version, false));
        doThrow(new RuntimeException("s3 copy failed")).when(fileStoragePort).copyObject(anyString(), anyString());

        assertThatThrownBy(() -> service.attach(cmd()))
                .satisfies(hasCode(FileErrorCode.FILE_OBJECT_NOT_FOUND));
        verify(failureRecorder, times(1)).markFailed(version);
        verify(txSupport, never()).completeAndIndex(any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("복사 후 객체가 없으면 FAILED + FILE_OBJECT_NOT_FOUND")
    void headMissing() {
        stubUploader();
        FileVersion version = uploadingVersion();
        stubPrepare(new Prepared(version, false));
        when(fileStoragePort.head(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attach(cmd()))
                .satisfies(hasCode(FileErrorCode.FILE_OBJECT_NOT_FOUND));
        verify(failureRecorder, times(1)).markFailed(version);
        verify(txSupport, never()).completeAndIndex(any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("복사본 크기가 다르면 FAILED + FILE_SIZE_MISMATCH")
    void sizeMismatch() {
        stubUploader();
        FileVersion version = uploadingVersion();
        stubPrepare(new Prepared(version, false));
        when(fileStoragePort.head(anyString())).thenReturn(Optional.of(new FileStoragePort.StoredObject(9999L)));

        assertThatThrownBy(() -> service.attach(cmd()))
                .satisfies(hasCode(FileErrorCode.FILE_SIZE_MISMATCH));
        verify(failureRecorder, times(1)).markFailed(version);
    }

    @Test
    @DisplayName("동시 경합(멱등키 UNIQUE 위반)이면 승자 행을 재조회해 흡수한다")
    void concurrentRace() {
        stubUploader();
        when(txSupport.prepareOrResume(anyLong(), anyLong(), any(), any(), anyLong(),
                any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("dup idempotency_key"));
        when(txSupport.findByIdempotencyKey(IDEM))
                .thenReturn(Optional.of(new Prepared(completedVersion(), true)));

        AttachStagedFileResult result = service.attach(cmd());

        assertThat(result.fileVersionId()).isEqualTo(74L);
        verify(fileStoragePort, never()).copyObject(anyString(), anyString());
    }

    @Test
    @DisplayName("멱등 경합이 아닌 무결성 위반(승자 없음)은 원래 예외를 그대로 던진다")
    void unrelatedIntegrityViolationPropagates() {
        stubUploader();
        when(txSupport.prepareOrResume(anyLong(), anyLong(), any(), any(), anyLong(),
                any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("fk violation"));
        when(txSupport.findByIdempotencyKey(IDEM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.attach(cmd()))
                .isInstanceOf(DataIntegrityViolationException.class);
        verify(fileStoragePort, never()).copyObject(anyString(), anyString());
    }

    @Test
    @DisplayName("경합 재조회에서 미완료(UPLOADING) 승자면 그 행으로 복사·완료를 재개한다")
    void resumesUploadingWinner() {
        stubUploader();
        FileVersion winner = uploadingVersion();
        when(txSupport.prepareOrResume(anyLong(), anyLong(), any(), any(), anyLong(),
                any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("dup"));
        when(txSupport.findByIdempotencyKey(IDEM)).thenReturn(Optional.of(new Prepared(winner, false)));
        when(fileStoragePort.head(anyString())).thenReturn(Optional.of(new FileStoragePort.StoredObject(5000L)));
        when(fileStoragePort.getObject(anyString())).thenReturn(new byte[]{1});
        when(pdfPageCounterPort.countPages(any())).thenReturn(Optional.of(3));
        when(txSupport.completeAndIndex(74L, 5000L, null, 3))
                .thenReturn(new AttachStagedFileResult(31L, 74L, 1, AttachStagedFileResult.INDEX_PENDING));

        AttachStagedFileResult result = service.attach(cmd());

        assertThat(result.fileVersionId()).isEqualTo(74L);
        verify(fileStoragePort, times(1)).copyObject(TEMP_KEY, STORAGE_KEY);
        verify(txSupport, times(1)).completeAndIndex(74L, 5000L, null, 3);
    }

    @Test
    @DisplayName("temporaryStorageKey 가 다른 회사 프리픽스면 FILE_INVALID_REQUEST — 준비 안 함")
    void rejectsForeignTenantTempKey() {
        stubUploader();
        AttachStagedFileCommand foreign = new AttachStagedFileCommand(
                COMPANY, PROJECT, USER, "companies/999/staging/x.pdf",
                "재정상태.pdf", 5000L, null, null, "AI 검토 첨부", true, IDEM);

        assertThatThrownBy(() -> service.attach(foreign))
                .satisfies(hasCode(FileErrorCode.FILE_INVALID_REQUEST));
        verify(txSupport, never()).prepareOrResume(anyLong(), anyLong(), any(), any(), anyLong(),
                any(), any(), any(), any(), any(), any());
    }
}
