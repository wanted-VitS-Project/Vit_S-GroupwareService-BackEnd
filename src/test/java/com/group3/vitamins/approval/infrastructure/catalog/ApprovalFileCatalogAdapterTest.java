package com.group3.vitamins.approval.infrastructure.catalog;

import com.group3.vitamins.approval.application.port.FileVersionSummary;
import com.group3.vitamins.file.domain.model.File;
import com.group3.vitamins.file.domain.model.FileVersion;
import com.group3.vitamins.file.domain.model.UploadStatus;
import com.group3.vitamins.file.domain.repository.FileRepository;
import com.group3.vitamins.file.domain.repository.FileVersionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("결재 첨부 파일 조회 — 휴지통 상태")
class ApprovalFileCatalogAdapterTest {

    private static final Long FILE_VERSION_ID = 300L;
    private static final Long FILE_ID = 30L;
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 8, 10, 12, 0);

    @Mock private FileVersionRepository fileVersionRepository;
    @Mock private FileRepository fileRepository;
    @InjectMocks private ApprovalFileCatalogAdapter adapter;

    @Test
    @DisplayName("원본 문서가 휴지통에 있으면 fileDeleted 가 true 이고 이름·크기는 그대로 내려간다")
    void marksTrashedFileButKeepsMetadata() {
        given(fileVersionRepository.findById(FILE_VERSION_ID)).willReturn(Optional.of(fileVersion()));
        given(fileRepository.findById(FILE_ID)).willReturn(Optional.of(
                File.restore(FILE_ID, 5L, "제안서", "EMP001", LocalDateTime.of(2026, 8, 10, 18, 0), 0)));

        FileVersionSummary summary = adapter.findFileVersion(FILE_VERSION_ID).orElseThrow();

        assertThat(summary.fileDeleted()).isTrue();
        // D-6 — 값을 숨기지 않는다. 증빙 이력이라 무슨 파일이었는지는 남아야 한다
        assertThat(summary.fileName()).isEqualTo("제안서_v1.pdf");
        assertThat(summary.fileSize()).isEqualTo(4404019L);
        assertThat(summary.uploadedAt()).isEqualTo(COMPLETED_AT);
    }

    @Test
    @DisplayName("살아있는 문서면 fileDeleted 가 false 다")
    void marksAliveFileAsNotDeleted() {
        given(fileVersionRepository.findById(FILE_VERSION_ID)).willReturn(Optional.of(fileVersion()));
        given(fileRepository.findById(FILE_ID)).willReturn(Optional.of(
                File.restore(FILE_ID, 5L, "제안서", "EMP001", null, 0)));

        assertThat(adapter.findFileVersion(FILE_VERSION_ID).orElseThrow().fileDeleted()).isFalse();
    }

    @Test
    @DisplayName("소유 문서를 못 찾으면 판단 근거가 없으므로 false 다")
    void defaultsToNotDeletedWhenOwnerFileMissing() {
        given(fileVersionRepository.findById(FILE_VERSION_ID)).willReturn(Optional.of(fileVersion()));
        given(fileRepository.findById(FILE_ID)).willReturn(Optional.empty());

        assertThat(adapter.findFileVersion(FILE_VERSION_ID).orElseThrow().fileDeleted()).isFalse();
    }

    @Test
    @DisplayName("파일 버전이 없으면 빈 값이다 — 문서 조회를 시도하지 않는다")
    void returnsEmptyWhenVersionMissing() {
        given(fileVersionRepository.findById(FILE_VERSION_ID)).willReturn(Optional.empty());

        assertThat(adapter.findFileVersion(FILE_VERSION_ID)).isEmpty();
        // 버전이 없으면 소유 문서를 물어볼 이유가 없다 — 불필요한 조회가 늘지 않게 고정한다
        verifyNoInteractions(fileRepository);
    }

    private FileVersion fileVersion() {
        return FileVersion.restore(FILE_VERSION_ID, FILE_ID, 1, UploadStatus.COMPLETED,
                "projects/5/files/30/v1", "제안서_v1.pdf", "pdf", "application/pdf",
                4404019L, "checksum", 12, null,
                "EMP001", "김결재", "영업팀", "과장", COMPLETED_AT, null, null);
    }
}
