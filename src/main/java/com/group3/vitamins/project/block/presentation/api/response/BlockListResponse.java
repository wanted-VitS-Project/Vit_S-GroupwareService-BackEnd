package com.group3.vitamins.project.block.presentation.api.response;

import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.application.result.BlockSummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스텝 블록 일괄 조회 응답")
public record BlockListResponse(

        @Schema(description = "블록 목록 (rowIndex → sortOrder 순)")
        List<BlockItemResponse> blocks
) {

    /** 조회 결과를 응답으로 옮긴다. */
    public static BlockListResponse from(List<BlockSummary> summaries) {
        return new BlockListResponse(summaries.stream()
                .map(BlockItemResponse::from)
                .toList());
    }

    @Schema(description = "블록 요약")
    public record BlockItemResponse(

            @Schema(description = "블록 ID", example = "15")
            Long blockId,

            @Schema(description = "블록 타입 (10종)", example = "CHECKLIST")
            String type,

            @Schema(description = "블록 제목. 추가 직후에는 null", example = "제안서 작성 체크리스트",
                    nullable = true)
            String title,

            @Schema(description = "블록 담당자. 미지정이면 null")
            BlockOwnerResponse owner,

            @Schema(description = "행 인덱스", example = "0")
            int rowIndex,

            @Schema(description = "행 내 순서", example = "0")
            int sortOrder,

            @Schema(description = "열 병합 수 (1~3)", example = "1")
            int colSpan,

            @Schema(description = "타입별 상세. 타입마다 구조가 다르다. "
                    + "TEXT = {txtId, content(마크다운·미작성이면 null)} · "
                    + "CHECKLIST = {chkBlockId, totalCount, completedCount, items[{chkId, content, isCompleted}]}. "
                    + "상세를 아직 지원하지 않는 타입은 null")
            BlockDetail detail,

            @Schema(description = "연결된 이슈 수", example = "2")
            int linkedIssueTotal,

            @Schema(description = "연결된 이슈 중 완료 수", example = "0")
            int linkedIssueDone
    ) {

        static BlockItemResponse from(BlockSummary summary) {
            BlockOwnerResponse owner = summary.owner() == null
                    ? null
                    : new BlockOwnerResponse(summary.owner().userId(), summary.owner().name(), summary.owner().deleted());

            return new BlockItemResponse(
                    summary.blockId(), summary.type(), summary.title(), owner,
                    summary.rowIndex(), summary.sortOrder(), summary.colSpan(),
                    summary.detail(), summary.linkedIssueTotal(), summary.linkedIssueDone());
        }
    }
}