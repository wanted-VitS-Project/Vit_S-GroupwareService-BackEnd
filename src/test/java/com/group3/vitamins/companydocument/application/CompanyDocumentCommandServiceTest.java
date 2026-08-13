package com.group3.vitamins.companydocument.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.companydocument.application.command.DeleteCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.command.RestoreCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.command.UpdateCompanyDocumentCommand;
import com.group3.vitamins.companydocument.application.policy.CompanyDocumentAdminPolicy;
import com.group3.vitamins.companydocument.application.port.CompanyDocumentIndexTriggerPort;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentRestoreResult;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentUpdateResult;
import com.group3.vitamins.companydocument.application.service.CompanyDocumentCommandService;
import com.group3.vitamins.companydocument.domain.exception.CompanyDocumentErrorCode;
import com.group3.vitamins.companydocument.domain.model.CompanyDocument;
import com.group3.vitamins.companydocument.domain.model.DocumentCategory;
import com.group3.vitamins.companydocument.domain.repository.CompanyDocumentRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CompanyDocumentCommandService 사내 문서 수정·삭제·복구")
class CompanyDocumentCommandServiceTest {

    private static final long COMPANY_ID = 9L;
    private static final String USER = "EMP001";

    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private CompanyDocumentRepository documentRepository;
    private CompanyDocumentIndexTriggerPort indexTriggerPort;
    private CompanyDocumentCommandService service;

    @BeforeEach
    void setUp() {
        currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        documentRepository = Mockito.mock(CompanyDocumentRepository.class);
        indexTriggerPort = Mockito.mock(CompanyDocumentIndexTriggerPort.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        when(documentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new CompanyDocumentCommandService(
                new CompanyDocumentAdminPolicy(), currentCompanyIdProvider, documentRepository, indexTriggerPort);
    }

    private CompanyDocument doc(LocalDateTime deletedAt) {
        return CompanyDocument.restore(12L, COMPANY_ID, DocumentCategory.FINANCE, "재무", USER, deletedAt);
    }

    private void expectCode(Runnable r, Object code) {
        assertThatThrownBy(r::run).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(code);
    }

    @Test
    @DisplayName("수정 — 표시명·카테고리를 반영한다")
    void updateNameAndCategory() {
        when(documentRepository.findById(12L)).thenReturn(Optional.of(doc(null)));

        CompanyDocumentUpdateResult result = service.update(
                new UpdateCompanyDocumentCommand(12L, "재무제표(확정)", "PERFORMANCE", USER, "ADMIN"));

        assertThat(result.name()).isEqualTo("재무제표(확정)");
        assertThat(result.category()).isEqualTo("PERFORMANCE");
    }

    @Test
    @DisplayName("수정 — name·category 둘 다 비면 CDOC_INVALID_REQUEST")
    void updateRejectsEmpty() {
        expectCode(() -> service.update(new UpdateCompanyDocumentCommand(12L, "  ", null, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_INVALID_REQUEST);
        verify(documentRepository, never()).findById(any());
    }

    @Test
    @DisplayName("수정 — 비 ADMIN 은 ACC_ADMIN_REQUIRED")
    void updateRejectsNonAdmin() {
        expectCode(() -> service.update(new UpdateCompanyDocumentCommand(12L, "새이름", null, USER, "MEMBER")),
                AccountErrorCode.ACC_ADMIN_REQUIRED);
    }

    @Test
    @DisplayName("삭제 — soft delete + 인덱스 제외 트리거")
    void deleteSoftAndTriggersRemoval() {
        when(documentRepository.findById(12L)).thenReturn(Optional.of(doc(null)));

        service.delete(new DeleteCompanyDocumentCommand(12L, USER, "ADMIN"));

        verify(indexTriggerPort).triggerRemoval(12L);
    }

    @Test
    @DisplayName("삭제 — 이미 삭제된 문서는 CDOC_ALREADY_DELETED")
    void deleteRejectsAlreadyDeleted() {
        when(documentRepository.findById(12L)).thenReturn(Optional.of(doc(LocalDateTime.now())));

        expectCode(() -> service.delete(new DeleteCompanyDocumentCommand(12L, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_ALREADY_DELETED);
        verify(indexTriggerPort, never()).triggerRemoval(any());
    }

    @Test
    @DisplayName("복구 — 삭제 상태가 아니면 CDOC_NOT_DELETED")
    void restoreRejectsNotDeleted() {
        when(documentRepository.findById(12L)).thenReturn(Optional.of(doc(null)));

        expectCode(() -> service.restore(new RestoreCompanyDocumentCommand(12L, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_NOT_DELETED);
    }

    @Test
    @DisplayName("복구 — 삭제된 문서를 복구하고 인덱스 재등록 트리거")
    void restoreTriggersReindex() {
        when(documentRepository.findById(12L)).thenReturn(Optional.of(doc(LocalDateTime.now())));

        CompanyDocumentRestoreResult result = service.restore(new RestoreCompanyDocumentCommand(12L, USER, "ADMIN"));

        assertThat(result.companyDocumentId()).isEqualTo(12L);
        verify(indexTriggerPort).triggerReindex(12L);
    }

    @Test
    @DisplayName("수정·삭제·복구 — 타 회사 문서면 CDOC_NOT_FOUND, 저장·트리거 안 함(회사 경계)")
    void rejectsOtherCompany() {
        // 같은 documentId 지만 companyId 가 다른 회사(999) — 현재 회사 스코프에서 제외되어야 한다.
        when(documentRepository.findById(12L)).thenReturn(Optional.of(
                CompanyDocument.restore(12L, 999L, DocumentCategory.FINANCE, "남의문서", "EMPX01", null)));

        expectCode(() -> service.update(new UpdateCompanyDocumentCommand(12L, "새이름", null, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_NOT_FOUND);
        expectCode(() -> service.delete(new DeleteCompanyDocumentCommand(12L, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_NOT_FOUND);
        expectCode(() -> service.restore(new RestoreCompanyDocumentCommand(12L, USER, "ADMIN")),
                CompanyDocumentErrorCode.CDOC_NOT_FOUND);

        verify(documentRepository, never()).save(any());
        verify(indexTriggerPort, never()).triggerRemoval(any());
        verify(indexTriggerPort, never()).triggerReindex(any());
    }
}
