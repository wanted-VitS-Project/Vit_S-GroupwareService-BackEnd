package com.group3.vitamins.file.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.file.application.policy.FileAdminPolicy;
import com.group3.vitamins.file.application.port.FileQueryPort;
import com.group3.vitamins.file.application.query.CompanyFileCriteria;
import com.group3.vitamins.file.application.query.CompanyFileQuery;
import com.group3.vitamins.file.application.query.MyProjectFileCriteria;
import com.group3.vitamins.file.application.query.MyProjectFileQuery;
import com.group3.vitamins.file.application.result.CompanyFilePageResult;
import com.group3.vitamins.file.application.result.FileViewProjection;
import com.group3.vitamins.file.application.result.FileViewResult;
import com.group3.vitamins.file.application.service.FileListViewService;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("FileListViewService 파일 관리 화면 조회(FILE-Q)")
class FileListViewServiceTest {

    private static final long COMPANY_ID = 9L;
    private static final String USER = "EMP001";

    private FileQueryPort fileQueryPort;
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private FileListViewService service;

    @BeforeEach
    void setUp() {
        fileQueryPort = Mockito.mock(FileQueryPort.class);
        currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        // 실제 정책을 써서 role 판정까지 통합 검증한다.
        service = new FileListViewService(fileQueryPort, currentCompanyIdProvider, new FileAdminPolicy());
    }

    private FileViewProjection projection(String originalName, String ext) {
        return new FileViewProjection(
                5L, "제안", 12L, "블록", false, 100L, "스마트시티", 31L, "문서",
                74L, 3, 3, originalName, ext, 5000L, "박지영", "개발팀", "선임", LocalDateTime.now());
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return t -> assertThat(t).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(expected);
    }

    @Test
    @DisplayName("전사 — ADMIN 이면 회사 스코프 페이지 조회, previewable 파생, total 반영")
    void companyFilesAsAdmin() {
        when(fileQueryPort.countCompanyFiles(any())).thenReturn(42L);
        when(fileQueryPort.findCompanyFiles(any()))
                .thenReturn(List.of(projection("a.pdf", "pdf"), projection("b.xlsx", "xlsx")));

        CompanyFilePageResult result = service.getCompanyFiles(
                new CompanyFileQuery(USER, "ADMIN", "kw", null, null, 1, 20));

        assertThat(result.totalElements()).isEqualTo(42);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.content()).extracting(FileViewResult::previewable).containsExactly(true, false);

        ArgumentCaptor<CompanyFileCriteria> cap = ArgumentCaptor.forClass(CompanyFileCriteria.class);
        verify(fileQueryPort).findCompanyFiles(cap.capture());
        assertThat(cap.getValue().companyId()).isEqualTo(COMPANY_ID);
        assertThat(cap.getValue().offset()).isEqualTo(20);   // page 1 * size 20
        assertThat(cap.getValue().limit()).isEqualTo(20);
        assertThat(cap.getValue().keyword()).isEqualTo("kw");
    }

    @Test
    @DisplayName("전사 — ADMIN 아니면 ACC_ADMIN_REQUIRED, 조회 안 함")
    void companyFilesRejectsNonAdmin() {
        assertThatThrownBy(() -> service.getCompanyFiles(
                new CompanyFileQuery(USER, "MEMBER", null, null, null, 0, 20)))
                .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));

        verify(fileQueryPort, never()).findCompanyFiles(any());
        verify(fileQueryPort, never()).countCompanyFiles(any());
    }

    @Test
    @DisplayName("전사 — page 음수는 0, size 상한 100 으로 정규화")
    void companyFilesClampsPaging() {
        when(fileQueryPort.countCompanyFiles(any())).thenReturn(0L);
        when(fileQueryPort.findCompanyFiles(any())).thenReturn(List.of());

        service.getCompanyFiles(new CompanyFileQuery(USER, "ADMIN", null, null, null, -5, 500));

        ArgumentCaptor<CompanyFileCriteria> cap = ArgumentCaptor.forClass(CompanyFileCriteria.class);
        verify(fileQueryPort).findCompanyFiles(cap.capture());
        assertThat(cap.getValue().offset()).isEqualTo(0);
        assertThat(cap.getValue().limit()).isEqualTo(100);
    }

    @Test
    @DisplayName("전사 — 큰 page 에서도 offset 이 int overflow 로 음수가 되지 않는다")
    void companyFilesOffsetNoOverflow() {
        when(fileQueryPort.countCompanyFiles(any())).thenReturn(0L);
        when(fileQueryPort.findCompanyFiles(any())).thenReturn(List.of());

        service.getCompanyFiles(new CompanyFileQuery(USER, "ADMIN", null, null, null, Integer.MAX_VALUE, 100));

        ArgumentCaptor<CompanyFileCriteria> cap = ArgumentCaptor.forClass(CompanyFileCriteria.class);
        verify(fileQueryPort).findCompanyFiles(cap.capture());
        assertThat(cap.getValue().offset()).isEqualTo((long) Integer.MAX_VALUE * 100);
        assertThat(cap.getValue().offset()).isPositive();
    }

    @Test
    @DisplayName("내 파일 — MEMBER 는 adminAll=false(스텝 필터 적용), 빈 검색어는 null")
    void myFilesMemberKeepsStepFilter() {
        when(fileQueryPort.findMyProjectFiles(any())).thenReturn(List.of(projection("a.pdf", "pdf")));

        List<FileViewResult> result = service.getMyProjectFiles(
                new MyProjectFileQuery(USER, "MEMBER", "  ", null, null));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).previewable()).isTrue();

        ArgumentCaptor<MyProjectFileCriteria> cap = ArgumentCaptor.forClass(MyProjectFileCriteria.class);
        verify(fileQueryPort).findMyProjectFiles(cap.capture());
        assertThat(cap.getValue().adminAll()).isFalse();
        assertThat(cap.getValue().companyId()).isEqualTo(COMPANY_ID);
        assertThat(cap.getValue().requesterUserId()).isEqualTo(USER);
        assertThat(cap.getValue().keyword()).isNull();
    }

    @Test
    @DisplayName("내 파일 — ADMIN/MASTER 는 adminAll=true(스텝 필터 스킵), 멤버십은 유지")
    void myFilesAdminSkipsStepFilter() {
        when(fileQueryPort.findMyProjectFiles(any())).thenReturn(List.of());

        service.getMyProjectFiles(new MyProjectFileQuery(USER, "MASTER", null, null, null));

        ArgumentCaptor<MyProjectFileCriteria> cap = ArgumentCaptor.forClass(MyProjectFileCriteria.class);
        verify(fileQueryPort).findMyProjectFiles(cap.capture());
        assertThat(cap.getValue().adminAll()).isTrue();
    }
}
