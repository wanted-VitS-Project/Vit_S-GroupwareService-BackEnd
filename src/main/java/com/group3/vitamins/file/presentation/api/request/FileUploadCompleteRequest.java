package com.group3.vitamins.file.presentation.api.request;

import com.group3.vitamins.file.application.command.CompleteFileUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업로드 완료 통보 요청. checksum 은 선택.")
public record FileUploadCompleteRequest(
        @Schema(description = "클라이언트가 계산한 체크섬(선택). 버전에 기록된다.", example = "d41d8cd98f00b204e9800998ecf8427e")
        String checksum
) {

    public CompleteFileUploadCommand toCommand(Long fileVersionId, String requesterUserId, String role) {
        return new CompleteFileUploadCommand(fileVersionId, checksum, requesterUserId, role);
    }
}
