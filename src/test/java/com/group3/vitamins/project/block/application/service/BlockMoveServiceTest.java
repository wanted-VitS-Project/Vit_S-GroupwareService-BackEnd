package com.group3.vitamins.project.block.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.block.application.command.MoveBlockCommand;
import com.group3.vitamins.project.block.application.port.IssueBlockUnlinkPort;
import com.group3.vitamins.project.block.application.result.BlockMoveResult;
import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/** 블록 이동 (BLK-014). 생성·수정·배치·삭제는 다른 테스트 소관이다. */
@ExtendWith(MockitoExtension.class)
class BlockMoveServiceTest {

    @Mock private BlockRepository blockRepository;
    @Mock private EmployeeLookupPort employeeLookupPort;
    @Mock private BlockDetailRegistry blockDetailRegistry;
    @Mock private IssueBlockUnlinkPort issueBlockUnlinkPort;
    @Mock private StepAccessUseCase stepAccessUseCase;

    @InjectMocks private BlockCommandService blockCommandService;

    private static final Long BLOCK_ID = 21L;
    private static final Long FROM_STEP = 10L;
    private static final Long TO_STEP = 11L;
    private static final Long PROJECT_ID = 3L;
    private static final String REQUESTER = "E2024001";

    @Test
    @DisplayName("도착 스텝의 맨 아래 새 행에 붙고 이슈 연결은 끊긴다")
    void 이동() {
        givenBlock();
        givenStep(FROM_STEP, PROJECT_ID);
        givenStep(TO_STEP, PROJECT_ID);
        given(blockRepository.findMaxRowIndex(TO_STEP)).willReturn(Optional.of(4));
        given(issueBlockUnlinkPort.unlinkByBlockId(BLOCK_ID)).willReturn(2);
        given(blockRepository.save(any(Block.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        BlockMoveResult result = blockCommandService.moveBlock(command(TO_STEP));

        assertThat(result.stepId()).isEqualTo(TO_STEP);
        assertThat(result.unlinkedIssueCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("도착 스텝에 블록이 없으면 첫 행에 붙는다")
    void 이동_빈_스텝() {
        givenBlock();
        givenStep(FROM_STEP, PROJECT_ID);
        givenStep(TO_STEP, PROJECT_ID);
        given(blockRepository.findMaxRowIndex(TO_STEP)).willReturn(Optional.empty());
        given(blockRepository.save(any(Block.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        blockCommandService.moveBlock(command(TO_STEP));

        assertThat(captureSaved().getRowIndex()).isZero();
    }

    @Test
    @DisplayName("대상 스텝이 없으면 400 이다 — 블록 조회까지 가지 않는다")
    void 대상_누락() {
        assertThatThrownBy(() -> blockCommandService.moveBlock(command(null)))
                .isInstanceOf(ValidationException.class);

        Mockito.verifyNoInteractions(blockRepository, stepAccessUseCase);
    }

    @Test
    @DisplayName("같은 스텝으로는 못 옮긴다 — 400")
    void 같은_스텝() {
        givenBlock();
        givenStep(FROM_STEP, PROJECT_ID);

        assertThatThrownBy(() -> blockCommandService.moveBlock(command(FROM_STEP)))
                .isInstanceOf(ValidationException.class);

        Mockito.verifyNoInteractions(issueBlockUnlinkPort);
    }

    @Test
    @DisplayName("다른 프로젝트 스텝으로는 못 옮긴다 — 400")
    void 다른_프로젝트() {
        givenBlock();
        givenStep(FROM_STEP, PROJECT_ID);
        givenStep(TO_STEP, 99L);

        assertThatThrownBy(() -> blockCommandService.moveBlock(command(TO_STEP)))
                .isInstanceOf(ValidationException.class);

        Mockito.verifyNoInteractions(issueBlockUnlinkPort);
    }

    @Test
    @DisplayName("도착 스텝 편집 권한이 없으면 403 이다 — 출발만 보면 안 된다")
    void 도착_권한_없음() {
        givenBlock();
        givenStep(FROM_STEP, PROJECT_ID);
        willThrow(new ForbiddenException(
                com.group3.vitamins.project.step.domain.exception.StepErrorCode.STEP_EDIT_DENIED))
                .given(stepAccessUseCase).requireEditable(TO_STEP, REQUESTER, "USER");

        assertThatThrownBy(() -> blockCommandService.moveBlock(command(TO_STEP)))
                .isInstanceOf(ForbiddenException.class);

        Mockito.verifyNoInteractions(issueBlockUnlinkPort);
    }

    @Test
    @DisplayName("없는 블록은 404 다")
    void 블록_없음() {
        given(blockRepository.findById(BLOCK_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> blockCommandService.moveBlock(command(TO_STEP)))
                .isInstanceOf(NotFoundException.class);
    }

    private MoveBlockCommand command(Long stepId) {
        return new MoveBlockCommand(BLOCK_ID, stepId, REQUESTER, "USER");
    }

    private void givenBlock() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        given(blockRepository.findById(BLOCK_ID)).willReturn(Optional.of(
                Block.restore(BLOCK_ID, FROM_STEP, "제안서", BlockType.TEXT, 5L, null,
                        2, 1, 0, REQUESTER, createdAt, createdAt, null)));
    }

    private void givenStep(Long stepId, Long projectId) {
        given(stepAccessUseCase.requireEditable(stepId, REQUESTER, "USER"))
                .willReturn(new StepAccessUseCase.StepAccessView(
                        stepId, projectId, MemberPermission.EDITOR));
    }

    private Block captureSaved() {
        org.mockito.ArgumentCaptor<Block> captor =
                org.mockito.ArgumentCaptor.forClass(Block.class);
        Mockito.verify(blockRepository).save(captor.capture());
        return captor.getValue();
    }
}
