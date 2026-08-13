package com.group3.vitamins.bidding.referencefile.presentation.api.response;

import com.group3.vitamins.bidding.referencefile.application.result.ReferenceFileListResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

public record ReferenceFileListResponse(

        @Schema(description = "현재 회사의 삭제되지 않은 기준자료 목록")
        List<Item> content
) {

    public static ReferenceFileListResponse from(ReferenceFileListResult result) {
        return new ReferenceFileListResponse(
                result.content().stream().map(Item::from).toList()
        );
    }

    public record Item(
            @Schema(description = "검토 요청에서 사용할 기준자료 ID", example = "501")
            Long referenceFileId,

            @Schema(description = "원본 파일명")
            String fileName,

            @Schema(description = "소문자 확장자", example = "pdf")
            String extension,

            @Schema(description = "MIME 타입")
            String mimeType,

            @Schema(description = "파일 크기(바이트)")
            long sizeBytes,

            @Schema(description = "업로드 상태")
            String uploadStatus,

            @Schema(description = "인덱싱 상태")
            String indexStatus,

            @Schema(description = "검토에 사용할 수 있는지 여부")
            boolean selectable,

            @Schema(description = "등록 시각")
            LocalDateTime createdAt
    ) {

        public static Item from(ReferenceFileListResult.Item item) {
            return new Item(
                    item.referenceFileId(), item.fileName(), item.extension(), item.mimeType(),
                    item.sizeBytes(), item.uploadStatus(), item.indexStatus(),
                    item.selectable(), item.createdAt()
            );
        }
    }
}