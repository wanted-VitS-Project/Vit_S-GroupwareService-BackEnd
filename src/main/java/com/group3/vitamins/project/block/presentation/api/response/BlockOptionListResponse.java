package com.group3.vitamins.project.block.presentation.api.response;

import com.group3.vitamins.project.block.application.result.BlockOption;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "스텝 블록 선택 후보 조회 응답")
public record BlockOptionListResponse(

        @Schema(description = "블록 목록 (rowIndex → sortOrder 순)")
        List<BlockOptionResponse> blocks
) {

    /** 조회 결과를 응답으로 옮긴다. */
    public static BlockOptionListResponse from(List<BlockOption> options) {
        return new BlockOptionListResponse(options.stream()
                .map(BlockOptionResponse::from)
                .toList());
    }

    @Schema(description = "선택 후보 블록")
    public record BlockOptionResponse(

            @Schema(description = "블록 ID. 이슈 생성 시 blockIds[] 로 보내는 값", example = "101")
            Long blockId,

            @Schema(description = "블록 타입 (아이콘 결정용)", example = "CHECKLIST")
            String type,

            @Schema(description = "블록 제목. 추가 직후라 비어 있으면 null", example = "제안서 작성 체크리스트",
                    nullable = true)
            String title
    ) {

        static BlockOptionResponse from(BlockOption option) {
            return new BlockOptionResponse(option.blockId(), option.type(), option.title());
        }
    }
}