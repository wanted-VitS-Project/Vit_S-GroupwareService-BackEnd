package com.group3.vitamins.vitamate.infrastructure.blockdetail;

import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.application.result.VitamateDetail;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.vitamate.analysis.application.service.VitamateBlockHandlerService;
import com.group3.vitamins.vitamate.analysis.infrastructure.blockdetail.VitamateBlockDetailAdapter;
import com.group3.vitamins.vitamate.analysis.infrastructure.blockdetail.VitamateDetailMapper;
import com.group3.vitamins.vitamate.analysis.infrastructure.blockdetail.VitamateDetailRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VitamateBlockDetailAdapter")
class VitamateBlockDetailAdapterTest {

    private static final Long BLOCK_ID = 10L;
    private static final Long VITAMATE_BLOCK_ID = 20L;
    private static final String USER_ID = "EMP001";

    private VitamateDetailMapper vitamateDetailMapper;
    private VitamateBlockHandlerService vitamateBlockHandlerService;
    private VitamateBlockDetailAdapter adapter;

    @BeforeEach
    void setUp() {
        vitamateDetailMapper = mock(VitamateDetailMapper.class);
        vitamateBlockHandlerService = mock(VitamateBlockHandlerService.class);
        adapter = new VitamateBlockDetailAdapter(vitamateDetailMapper, vitamateBlockHandlerService);
    }

    @Nested
    @DisplayName("BlockDetailPort 계약")
    class BlockDetailPortContract {

        @Test
        @DisplayName("AI 타입을 지원한다")
        void supportsAiType() {
            assertThat(adapter.supportedType()).isEqualTo(BlockType.AI);
        }

        @Test
        @DisplayName("AI 블록 상세 생성은 handler service에 위임한다")
        void delegatesCreateDetail() {
            when(vitamateBlockHandlerService.create(BLOCK_ID)).thenReturn(VITAMATE_BLOCK_ID);

            Long typeId = adapter.createDetail(BLOCK_ID);

            assertThat(typeId).isEqualTo(VITAMATE_BLOCK_ID);
        }

        @Test
        @DisplayName("AI 블록 상세 삭제는 handler service에 위임한다")
        void delegatesDeleteDetail() {
            LocalDateTime deletedAt = LocalDateTime.of(2026, 8, 5, 18, 0);

            adapter.deleteDetail(VITAMATE_BLOCK_ID, USER_ID, "AI 블록", deletedAt);

            verify(vitamateBlockHandlerService).delete(VITAMATE_BLOCK_ID, USER_ID, "AI 블록", deletedAt);
        }

        @Test
        @DisplayName("AI 블록 상세를 typeId 기준으로 조회해 BlockDetail로 변환한다")
        void loadsDetailsByTypeId() {
            when(vitamateDetailMapper.findByVitamateBlockIds(List.of(VITAMATE_BLOCK_ID)))
                    .thenReturn(List.of(new VitamateDetailRow(VITAMATE_BLOCK_ID, "안녕하세요")));

            Map<Long, BlockDetail> details = adapter.loadDetails(List.of(VITAMATE_BLOCK_ID));

            assertThat(details)
                    .containsEntry(VITAMATE_BLOCK_ID, new VitamateDetail("안녕하세요"));
        }
    }
}
