package com.group3.vitamins.companydocument.application;

import com.group3.vitamins.companydocument.application.port.CompanyDocumentReferenceQueryPort;
import com.group3.vitamins.companydocument.application.result.CompanyDocumentReferenceView;
import com.group3.vitamins.companydocument.application.service.CompanyDocumentReferenceService;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CompanyDocumentReferenceService 참조 선택 조회")
class CompanyDocumentReferenceServiceTest {

    private static final long COMPANY_ID = 7L;

    private CompanyDocumentReferenceQueryPort port;
    private CompanyDocumentReferenceService service;

    @BeforeEach
    void setUp() {
        port = Mockito.mock(CompanyDocumentReferenceQueryPort.class);
        CurrentCompanyIdProvider companyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
        service = new CompanyDocumentReferenceService(port, companyIdProvider);
    }

    @Test
    @DisplayName("목록 — 현재 회사로 스코프하고 빈 필터는 null 로 눕혀 위임한다")
    void listScopesToCurrentCompanyAndNormalizesFilters() {
        when(port.findSelectableDocuments(eq(COMPANY_ID), eq("FINANCE"), eq(null)))
                .thenReturn(List.of(new CompanyDocumentReferenceView(1L, 11L, "FINANCE", "a.pdf", 2, null)));

        var result = service.listSelectable("FINANCE", "   "); // 공백 keyword → null

        assertThat(result).singleElement()
                .satisfies(v -> assertThat(v.companyDocumentVersionId()).isEqualTo(11L));
        verify(port).findSelectableDocuments(COMPANY_ID, "FINANCE", null);
    }

    @Test
    @DisplayName("버전 검증 — 현재 회사 스코프로 위임한다(타사 버전은 포트가 빈 값 반환)")
    void versionScopesToCurrentCompany() {
        when(port.findSelectableVersion(90L, COMPANY_ID)).thenReturn(Optional.empty());

        assertThat(service.getSelectableVersion(90L)).isEmpty();
        verify(port).findSelectableVersion(90L, COMPANY_ID);
    }
}
