package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.global.domain.common.error.ErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.block.application.command.DeleteBlockCommand;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.port.IssueBlockUnlinkPort;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockApprovalDeletionPropagationTest {

    @Mock private BlockRepository blockRepository;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private BlockDetailPort approvalDetailPort;
    @Mock private IssueBlockUnlinkPort issueBlockUnlinkPort;
    @Mock private StepAccessUseCase stepAccessUseCase;

    private BlockCommandService service;

    @BeforeEach
    void setUp() {
        when(approvalDetailPort.supportedType()).thenReturn(BlockType.APPROVAL);
        service = new BlockCommandService(
                blockRepository,
                employeeLookupPort,
                new BlockDetailRegistry(List.of(approvalDetailPort)),
                issueBlockUnlinkPort,
                stepAccessUseCase);
    }

    @Test
    void directBlockDeletionPropagatesSameDeletionTimeToApprovalDetail() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        Block block = Block.restore(
                10L, 20L, "구매 품의", BlockType.APPROVAL, 100L,
                null, 0, 1, 0, 0, "EMP001", createdAt, createdAt, null);
        when(blockRepository.findById(10L)).thenReturn(Optional.of(block));
        when(stepAccessUseCase.requireEditable(20L, "EMP001", "USER"))
                .thenReturn(new StepAccessUseCase.StepAccessView(20L, 30L, MemberPermission.EDITOR));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deleteBlock(new DeleteBlockCommand(10L, "EMP001", "USER"));

        ArgumentCaptor<LocalDateTime> detailDeletedAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(approvalDetailPort).deleteDetail(
                org.mockito.ArgumentMatchers.eq(100L),
                org.mockito.ArgumentMatchers.eq("EMP001"),
                org.mockito.ArgumentMatchers.eq("구매 품의"),
                detailDeletedAt.capture());
        ArgumentCaptor<Block> savedBlock = ArgumentCaptor.forClass(Block.class);
        verify(blockRepository).save(savedBlock.capture());

        assertThat(savedBlock.getValue().getDeletedAt()).isNotNull();
        assertThat(detailDeletedAt.getValue()).isEqualTo(savedBlock.getValue().getDeletedAt());
    }

    /** DEL-016 — 상세가 삭제를 거부하면 블록도 지워지지 않는다. 판정이 삭제보다 먼저다. */
    @Test
    @DisplayName("직접 삭제는 상세 판정에 막히면 블록·상세 어느 쪽도 지우지 않는다")
    void directBlockDeletionStopsWhenDetailRejects() {
        Block block = approvalBlock();
        when(blockRepository.findById(10L)).thenReturn(Optional.of(block));
        when(stepAccessUseCase.requireEditable(20L, "EMP001", "USER"))
                .thenReturn(new StepAccessUseCase.StepAccessView(20L, 30L, MemberPermission.EDITOR));
        doThrow(new ConflictException(TestErrorCode.LOCKED))
                .when(approvalDetailPort).assertDeletable(100L);

        assertThatThrownBy(() -> service.deleteBlock(new DeleteBlockCommand(10L, "EMP001", "USER")))
                .isInstanceOf(ConflictException.class);

        verify(approvalDetailPort, never()).deleteDetail(any(), any(), any(), any());
        verify(blockRepository, never()).save(any(Block.class));
    }

    /**
     * ⚠️ DEL-017 — 이 테스트가 함정을 잡는다. 판정을 {@code BlockCommandService} 의 private
     * {@code deleteBlock(block, userId)} 공유 본체에 넣으면 여기서 409 가 터진다. 실제 서비스에서는
     * 스텝 삭제 전체가 롤백되는 증상이고, 그게 폐기된 BLK-008 잠금과 같은 실패다.
     */
    @Test
    @DisplayName("스텝 삭제 cascade 는 상세 판정을 부르지 않는다 — 상신된 결재도 함께 삭제된다")
    void cascadeDeletionSkipsDetailGate() {
        Block block = approvalBlock();
        when(blockRepository.findAllByIds(List.of(10L))).thenReturn(List.of(block));
        when(blockRepository.save(any(Block.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatCode(() -> service.deleteBlocks(List.of(10L), "EMP001")).doesNotThrowAnyException();

        verify(approvalDetailPort, never()).assertDeletable(any());
        verify(approvalDetailPort).deleteDetail(eq(100L), eq("EMP001"), eq("구매 품의"), any());
        assertThat(block.getDeletedAt()).isNotNull();
    }

    private Block approvalBlock() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        return Block.restore(
                10L, 20L, "구매 품의", BlockType.APPROVAL, 100L,
                null, 0, 1, 0, 0, "EMP001", createdAt, createdAt, null);
    }

    /** 판정을 거부하는 상세를 흉내내기 위한 최소 코드. 결재 도메인 코드를 블록 테스트로 끌어오지 않는다. */
    private enum TestErrorCode implements ErrorCode {
        LOCKED;

        @Override
        public String getCode() {
            return "TEST_LOCKED";
        }

        @Override
        public String getMessage() {
            return "삭제할 수 없습니다.";
        }
    }
}
