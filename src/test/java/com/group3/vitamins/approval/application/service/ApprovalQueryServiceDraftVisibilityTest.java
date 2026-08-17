package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.policy.ApprovalListScopePolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalViewPolicy;
import com.group3.vitamins.approval.application.port.ApprovalLineDetailPort;
import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.BlockSummary;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.query.GetApprovalDetailQuery;
import com.group3.vitamins.approval.application.query.GetApprovalHistoryQuery;
import com.group3.vitamins.approval.application.query.GetApprovalRevisionQuery;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.approval.infrastructure.persistence.mapper.ApprovalListMapper;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 결재 블록 열람자는 {@code DRAFT}를 포함한 내부 내용을 조회할 수 있어야 한다. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalQueryService — DRAFT 열람")
class ApprovalQueryServiceDraftVisibilityTest {

    private static final Long APPROVAL_ID = 100L;
    private static final Long REVISION_ID = 200L;
    private static final Long BLOCK_ID = 300L;
    private static final String DRAFTER = "vitas-1234567";
    private static final String VIEWER = "vitas-7654321";

    @Mock
    private ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;
    @Mock
    private ApprovalViewPolicy viewPolicy;
    @Mock
    private ApprovalListScopePolicy listScopePolicy;
    @Mock
    private EmployeeCatalogPort employeeCatalogPort;
    @Mock
    private FileCatalogPort fileCatalogPort;
    @Mock
    private ApprovalRepository approvalRepository;
    @Mock
    private ApprovalLineDetailPort approvalLineDetailPort;
    @Mock
    private ApprovalListMapper approvalListMapper;
    @Mock
    private BlockCatalogPort blockCatalogPort;
    @Mock
    private CurrentCompanyIdProvider currentCompanyIdProvider;

    @InjectMocks
    private ApprovalQueryService service;

    private Approval approval;
    private ApprovalRevision draft;

    @BeforeEach
    void setUp() {
        approval = Approval.reconstruct(APPROVAL_ID, BLOCK_ID, DRAFTER, null,
                ApprovalStatus.DRAFT, 1, null, null, null, null);
        draft = ApprovalRevision.reconstruct(REVISION_ID, APPROVAL_ID, 1,
                "휴가 품의", "휴가를 신청합니다.", ApprovalStatus.DRAFT,
                null, null, null, null, null);

        when(revisionEligibilityPolicy.getApprovalOrThrow(APPROVAL_ID)).thenReturn(approval);
    }

    @Test
    @DisplayName("스텝 열람자는 DRAFT 회차 상세의 제목과 내용을 조회한다")
    void viewerReadsDraftRevisionDetail() {
        givenDrafter();
        when(revisionEligibilityPolicy.getRevisionOrThrow(APPROVAL_ID, REVISION_ID)).thenReturn(draft);
        when(approvalRepository.findLinesByRevisionId(REVISION_ID)).thenReturn(List.of());
        when(approvalRepository.findDocumentsByRevisionId(REVISION_ID)).thenReturn(List.of());
        when(approvalLineDetailPort.findLineDetails(REVISION_ID)).thenReturn(List.of());

        var result = service.getRevisionDetail(new GetApprovalRevisionQuery(APPROVAL_ID, REVISION_ID, VIEWER));

        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(result.title()).isEqualTo("휴가 품의");
        assertThat(result.content()).isEqualTo("휴가를 신청합니다.");
        verify(viewPolicy).assertViewable(approval, List.of(), VIEWER);
    }

    @Test
    @DisplayName("스텝 열람자는 결재 상세에서 현재 DRAFT 회차를 그대로 조회한다")
    void viewerReadsCurrentDraftApprovalDetail() {
        givenDrafter();
        when(approvalRepository.findLinesByApprovalId(APPROVAL_ID)).thenReturn(List.of());
        when(approvalRepository.findLatestRevisionReadOnly(APPROVAL_ID)).thenReturn(Optional.of(draft));
        when(approvalRepository.findDocumentsByRevisionId(REVISION_ID)).thenReturn(List.of());
        when(approvalLineDetailPort.findLineDetails(REVISION_ID)).thenReturn(List.of());
        when(blockCatalogPort.findBlock(BLOCK_ID))
                .thenReturn(Optional.of(new BlockSummary(BLOCK_ID, "APPROVAL", 10L, 20L, DRAFTER)));

        var result = service.getApprovalDetail(new GetApprovalDetailQuery(APPROVAL_ID, VIEWER));

        assertThat(result.revisionId()).isEqualTo(REVISION_ID);
        assertThat(result.status()).isEqualTo("DRAFT");
        assertThat(result.title()).isEqualTo("휴가 품의");
    }

    @Test
    @DisplayName("스텝 열람자의 결재 이력에서도 DRAFT 회차를 제외하지 않는다")
    void viewerSeesDraftInHistory() {
        when(approvalRepository.findLinesByApprovalId(APPROVAL_ID)).thenReturn(List.of());
        when(approvalRepository.findRevisionsByApprovalId(APPROVAL_ID)).thenReturn(List.of(draft));

        var result = service.getApprovalHistory(new GetApprovalHistoryQuery(APPROVAL_ID, VIEWER));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).status()).isEqualTo("DRAFT");
        assertThat(result.content().get(0).isCurrent()).isTrue();
    }

    private void givenDrafter() {
        when(employeeCatalogPort.findEmployee(DRAFTER)).thenReturn(Optional.of(new EmployeeSummary(
                DRAFTER, "기안자", "사원", "개발팀", "MEMBER", 1L,
                "ACTIVE", null, null)));
    }
}
