package com.group3.vitamins.file.presentation.api.response;

import com.group3.vitamins.file.application.result.FileRenameResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "문서명 수정 응답(§4).")
public record FileRenameResponse(
        @Schema(description = "문서 id", example = "31") Long fileId,
        @Schema(description = "바뀐 표시명", example = "제안서_최종") String name,
        @Schema(description = "저장 뒤 +1 된 새 낙관락 버전. 다음 저장에 이 값을 쓴다.", example = "4") int version
) {

    public static FileRenameResponse from(FileRenameResult r) {
        return new FileRenameResponse(r.fileId(), r.name(), r.version());
    }
}
