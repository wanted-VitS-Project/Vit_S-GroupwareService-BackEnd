package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.domain.repository.ApprovalRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** DEL-016 — 블록 직접 삭제 판정. cascade 경로는 이 판정을 부르지 않으므로 여기 대상이 아니다. */
@ExtendWith(MockitoExtension.class)
class ApprovalHandlerServiceDeleteGateTest {

    @Mock private ApprovalRepository approvalRepository;
    @Mock private BlockCatalogPort blockCatalogPort;
    @Mock private EmployeeCatalogPort employeeCatalogPort;
    @Mock private DomainEventPublisher domainEventPublisher;
    @InjectMocks private ApprovalHandlerService service;

    /**
     * 문구가 상태마다 다른 이유는 사용자가 <b>지금 화면의 상태</b>를 기준으로 읽기 때문이다 —
     * 반려 블록에 "상신된 결재"라고 하면 다른 것을 가리키는 것처럼 읽힌다. 코드는 하나로 유지한다.
     */
    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "IN_PROGRESS, 진행 중인 결재는 삭제할 수 없습니다.",
            "REJECTED,    반려된 결재는 삭제할 수 없습니다.",
            "COMPLETED,   완료된 결재는 삭제할 수 없습니다."
    })
    @DisplayName("상신 이후 3개 상태는 같은 코드·상태별 문구로 409를 낸다")
    void rejectsDirectDeletionAfterSubmission(ApprovalStatus status, String expectedMessage) {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(status, null)));

        assertThatThrownBy(() -> service.assertDeletableByBlock(100L))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ApprovalErrorCode.APPROVAL_ALREADY_SUBMITTED)
                .hasMessage(expectedMessage)
                // 파괴적 우회로(스텝 삭제)를 권하지 않는다 — 2026-08-12 결정
                .hasMessageNotContaining("스텝");
    }

    /** 상태를 늘리면서 문구를 빠뜨리면 폴백 문구가 조용히 나간다. 그게 없다는 것을 고정한다. */
    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = {"IN_PROGRESS", "REJECTED", "COMPLETED"})
    @DisplayName("어느 상태도 enum 폴백 문구로 나가지 않는다")
    void neverFallsBackToUmbrellaMessage(ApprovalStatus status) {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(status, null)));

        assertThatThrownBy(() -> service.assertDeletableByBlock(100L))
                .hasMessageNotContaining("이미 상신된");
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = {"DRAFT", "CANCELED"})
    @DisplayName("DRAFT(아직 요청 안 감)·CANCELED(이미 종결)는 삭제를 허용한다")
    void allowsDirectDeletionBeforeSubmissionAndAfterCancel(ApprovalStatus status) {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(status, null)));

        assertThatCode(() -> service.assertDeletableByBlock(100L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이미 삭제된 결재는 상태와 무관하게 통과한다 — deleteByBlock의 멱등 판정과 같다")
    void allowsAlreadyDeletedApproval() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(ApprovalStatus.IN_PROGRESS,
                        LocalDateTime.of(2026, 8, 11, 9, 0))));

        assertThatCode(() -> service.assertDeletableByBlock(100L)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("결재가 없으면 통과한다 — 판정이 상위 블록 삭제를 막지 않는다")
    void allowsMissingApproval() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.empty());

        assertThatCode(() -> service.assertDeletableByBlock(100L)).doesNotThrowAnyException();
    }

    /**
     * 잠금 조회를 쓰는지 본다. 잠금 없는 조회로 바꾸면 판정과 삭제 사이에 상신이 끼어들어
     * DRAFT로 통과한 결재가 IN_PROGRESS인 채 삭제된다 (DEL-006 · APR-DELETE-DRAFT.md §5).
     */
    @Test
    @DisplayName("판정은 잠금 조회로 읽는다 — 잠금 없는 조회를 쓰면 상신과 레이스가 난다")
    void readsThroughLockingQuery() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(approval(ApprovalStatus.DRAFT, null)));

        service.assertDeletableByBlock(100L);

        verify(approvalRepository).findApprovalIncludingDeletedForUpdate(100L);
        verify(approvalRepository, never()).findApproval(100L);
    }

    private Approval approval(ApprovalStatus status, LocalDateTime deletedAt) {
        return Approval.reconstruct(100L, 10L, "EMP001", null, status, 1, null, null, null, deletedAt);
    }
}
