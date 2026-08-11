package com.group3.vitamins.file.infrastructure.adapter;

import com.group3.vitamins.project.block.domain.model.Block;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.project.block.domain.repository.BlockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("BlockCatalogAdapter 블록 타입 게이트")
class BlockCatalogAdapterTest {

    private static final Long BLOCK_ID = 12L;
    private static final Long STEP_ID = 5L;

    private BlockRepository blockRepository;
    private BlockCatalogAdapter adapter;

    @BeforeEach
    void setUp() {
        blockRepository = Mockito.mock(BlockRepository.class);
        adapter = new BlockCatalogAdapter(blockRepository);
    }

    private Block blockOfType(BlockType type) {
        LocalDateTime now = LocalDateTime.now();
        return Block.restore(BLOCK_ID, STEP_ID, "블록", type, null,
                "EMP001", 0, 12, 0, 1, "EMP001", now, now, null);
    }

    @Nested
    @DisplayName("resolveFileBlockStepId (FILE 전용 · §3 목록)")
    class FileOnly {

        @Test
        @DisplayName("FILE 블록이면 스텝 ID 반환")
        void fileBlock() {
            when(blockRepository.findById(BLOCK_ID)).thenReturn(Optional.of(blockOfType(BlockType.FILE)));

            assertThat(adapter.resolveFileBlockStepId(BLOCK_ID)).contains(STEP_ID);
        }

        @Test
        @DisplayName("APPROVAL 블록이면 empty — 결재 파일은 §3 목록에 노출하지 않는다")
        void approvalBlockRejected() {
            when(blockRepository.findById(BLOCK_ID)).thenReturn(Optional.of(blockOfType(BlockType.APPROVAL)));

            assertThat(adapter.resolveFileBlockStepId(BLOCK_ID)).isEmpty();
        }

        @Test
        @DisplayName("다른 타입 블록이면 empty")
        void otherTypeRejected() {
            when(blockRepository.findById(BLOCK_ID)).thenReturn(Optional.of(blockOfType(BlockType.TEXT)));

            assertThat(adapter.resolveFileBlockStepId(BLOCK_ID)).isEmpty();
        }

        @Test
        @DisplayName("블록이 없거나 soft delete 됐으면 empty")
        void missingBlock() {
            when(blockRepository.findById(BLOCK_ID)).thenReturn(Optional.empty());

            assertThat(adapter.resolveFileBlockStepId(BLOCK_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("resolveAttachableBlockStepId (FILE 또는 APPROVAL · 업로드·조회·수정)")
    class Attachable {

        @Test
        @DisplayName("FILE 블록이면 스텝 ID 반환")
        void fileBlock() {
            when(blockRepository.findById(BLOCK_ID)).thenReturn(Optional.of(blockOfType(BlockType.FILE)));

            assertThat(adapter.resolveAttachableBlockStepId(BLOCK_ID)).contains(STEP_ID);
        }

        @Test
        @DisplayName("APPROVAL 블록도 스텝 ID 반환 — 결재 블록 매달기 허용")
        void approvalBlockAllowed() {
            when(blockRepository.findById(BLOCK_ID)).thenReturn(Optional.of(blockOfType(BlockType.APPROVAL)));

            assertThat(adapter.resolveAttachableBlockStepId(BLOCK_ID)).contains(STEP_ID);
        }

        @Test
        @DisplayName("FILE·APPROVAL 이 아닌 타입이면 empty")
        void otherTypeRejected() {
            when(blockRepository.findById(BLOCK_ID)).thenReturn(Optional.of(blockOfType(BlockType.CHECKLIST)));

            assertThat(adapter.resolveAttachableBlockStepId(BLOCK_ID)).isEmpty();
        }

        @Test
        @DisplayName("블록이 없거나 soft delete 됐으면 empty")
        void missingBlock() {
            when(blockRepository.findById(BLOCK_ID)).thenReturn(Optional.empty());

            assertThat(adapter.resolveAttachableBlockStepId(BLOCK_ID)).isEmpty();
        }
    }
}
