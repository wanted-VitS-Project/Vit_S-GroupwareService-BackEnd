package com.group3.vitamins.approval.application.service;

import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.Approval;
import com.group3.vitamins.approval.domain.model.ApprovalRevision;
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

/**
 * DEL-016 — 블록 직접 삭제 확인 판정. <b>막는 것이 아니라 되묻는 것</b>이라, 확인하고 다시 오면 통과한다.
 * cascade 경로는 이 판정을 부르지 않으므로 여기 대상이 아니다(DEL-017).
 */
@ExtendWith(MockitoExtension.class)
class ApprovalHandlerServiceDeleteGateTest {

    private static final String BLOCK_TITLE = "구매 품의";
    private static final String REVISION_TITLE = "기술 제안서";

    @Mock private ApprovalRepository approvalRepository;
    @Mock private BlockCatalogPort blockCatalogPort;
    @Mock private EmployeeCatalogPort employeeCatalogPort;
    @Mock private DomainEventPublisher domainEventPublisher;
    @InjectMocks private ApprovalHandlerService service;

    /**
     * 문구가 상태마다 다른 이유는 <b>상태별로 잃는 것이 다르기</b> 때문이다 — 완료 결재에
     * "취소됩니다"라고 하면 틀린 말이다(취소가 아니라 이력 열람을 잃는다).
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "IN_PROGRESS, 기술 제안서 결재가 진행 중입니다. 삭제하면 결재가 취소됩니다.",
            "REJECTED,    기술 제안서 결재는 반려된 상태입니다. 삭제하면 재상신할 수 없습니다.",
            "COMPLETED,   기술 제안서 결재는 완료된 상태입니다. 삭제하면 승인 이력을 다시 볼 수 없습니다."
    })
    @DisplayName("상신 이후 3개 상태는 같은 코드·상태별 문구로 확인을 요구한다")
    void requiresConfirmationAfterSubmission(ApprovalStatus status, String expectedMessage) {
        givenApproval(status, null);
        when(approvalRepository.findLatestRevisionReadOnly(100L))
                .thenReturn(Optional.of(revision(REVISION_TITLE)));

        assertThatThrownBy(() -> service.assertDeletableByBlock(100L, BLOCK_TITLE, false))
                .isInstanceOf(ConflictException.class)
                .hasFieldOrPropertyWithValue("errorCode", ApprovalErrorCode.APPROVAL_DELETE_CONFIRM_REQUIRED)
                .hasMessage(expectedMessage)
                // 파괴적 우회로(스텝 삭제)를 권하지 않는다 — 2026-08-12 결정
                .hasMessageNotContaining("스텝");
    }

    /**
     * ⭐ 확인 방식의 핵심 — 확인하고 다시 오면 <b>상태를 보지도 않고</b> 통과한다.
     * 여기서 막으면 다이얼로그를 눌러도 지워지지 않는다.
     */
    @ParameterizedTest
    @EnumSource(ApprovalStatus.class)
    @DisplayName("확인된 요청은 모든 상태에서 통과한다 — 조회조차 하지 않는다")
    void confirmedRequestPassesForEveryStatus(ApprovalStatus status) {
        assertThatCode(() -> service.assertDeletableByBlock(100L, BLOCK_TITLE, true))
                .doesNotThrowAnyException();

        verify(approvalRepository, never()).findApprovalIncludingDeletedForUpdate(100L);
    }

    /** 결재자 이름은 문구에 넣지 않는다 — 조회 API 로 막아둔 정보이고, 퇴사자·시점 불일치 문제가 있다 */
    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = {"IN_PROGRESS", "REJECTED", "COMPLETED"})
    @DisplayName("문구에 결재자 정보를 조회하지도, 담지도 않는다")
    void neverExposesApproverIdentity(ApprovalStatus status) {
        givenApproval(status, null);
        when(approvalRepository.findLatestRevisionReadOnly(100L))
                .thenReturn(Optional.of(revision(REVISION_TITLE)));

        assertThatThrownBy(() -> service.assertDeletableByBlock(100L, BLOCK_TITLE, false))
                .hasMessageNotContaining("결재자");

        verify(approvalRepository, never()).findLinesByApprovalId(100L);
    }

    /** 상태를 늘리면서 문구를 빠뜨리면 폴백 문구가 조용히 나간다. 그게 없다는 것을 고정한다. */
    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = {"IN_PROGRESS", "REJECTED", "COMPLETED"})
    @DisplayName("어느 상태도 enum 폴백 문구로 나가지 않는다")
    void neverFallsBackToUmbrellaMessage(ApprovalStatus status) {
        givenApproval(status, null);
        when(approvalRepository.findLatestRevisionReadOnly(100L))
                .thenReturn(Optional.of(revision(REVISION_TITLE)));

        assertThatThrownBy(() -> service.assertDeletableByBlock(100L, BLOCK_TITLE, false))
                .hasMessageNotContaining("확인이 필요합니다");
    }

    /** 회차 제목이 없으면 블록 제목으로 대체한다 — "결재 결재가 진행 중" 같은 문구가 나오지 않게 */
    @Test
    @DisplayName("회차 제목이 비면 블록 제목을 쓴다")
    void fallsBackToBlockTitle() {
        givenApproval(ApprovalStatus.IN_PROGRESS, null);
        when(approvalRepository.findLatestRevisionReadOnly(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertDeletableByBlock(100L, BLOCK_TITLE, false))
                .hasMessage("구매 품의 결재가 진행 중입니다. 삭제하면 결재가 취소됩니다.");
    }

    @ParameterizedTest
    @EnumSource(value = ApprovalStatus.class, names = {"DRAFT", "CANCELED"})
    @DisplayName("DRAFT(아직 요청 안 감)·CANCELED(이미 종결)는 확인 없이 삭제된다")
    void allowsDeletionWithoutConfirmBeforeSubmissionAndAfterCancel(ApprovalStatus status) {
        givenApproval(status, null);

        assertThatCode(() -> service.assertDeletableByBlock(100L, BLOCK_TITLE, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이미 삭제된 결재는 상태와 무관하게 통과한다 — deleteByBlock의 멱등 판정과 같다")
    void allowsAlreadyDeletedApproval() {
        givenApproval(ApprovalStatus.IN_PROGRESS, LocalDateTime.of(2026, 8, 11, 9, 0));

        assertThatCode(() -> service.assertDeletableByBlock(100L, BLOCK_TITLE, false))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("결재가 없으면 통과한다 — 판정이 상위 블록 삭제를 막지 않는다")
    void allowsMissingApproval() {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L)).thenReturn(Optional.empty());

        assertThatCode(() -> service.assertDeletableByBlock(100L, BLOCK_TITLE, false))
                .doesNotThrowAnyException();
    }

    /**
     * 잠금 조회를 쓰는지 본다. 잠금 없는 조회로 바꾸면 판정과 삭제 사이에 상신이 끼어들어
     * DRAFT로 통과한 결재가 IN_PROGRESS인 채 삭제된다 (DEL-006 · APR-DELETE-DRAFT.md §5).
     */
    @Test
    @DisplayName("판정은 잠금 조회로 읽는다 — 잠금 없는 조회를 쓰면 상신과 레이스가 난다")
    void readsThroughLockingQuery() {
        givenApproval(ApprovalStatus.DRAFT, null);

        service.assertDeletableByBlock(100L, BLOCK_TITLE, false);

        verify(approvalRepository).findApprovalIncludingDeletedForUpdate(100L);
        verify(approvalRepository, never()).findApproval(100L);
    }

    private void givenApproval(ApprovalStatus status, LocalDateTime deletedAt) {
        when(approvalRepository.findApprovalIncludingDeletedForUpdate(100L))
                .thenReturn(Optional.of(
                        Approval.reconstruct(100L, 10L, "EMP001", null, status, 1, null, null, null, deletedAt)));
    }

    private ApprovalRevision revision(String title) {
        return ApprovalRevision.reconstruct(200L, 100L, 1, title, "내용",
                ApprovalStatus.IN_PROGRESS, null, null, null, null, null);
    }
}
