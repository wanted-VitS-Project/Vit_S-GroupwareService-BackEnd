package com.group3.vitamins.companydocument.presentation.api.request;

import com.group3.vitamins.companydocument.application.command.CompleteCompanyDocumentUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업로드 완료 통보 요청. checksum 은 선택.")
public record CompanyDocumentUploadCompleteRequest(
        @Schema(description = "클라이언트가 계산한 체크섬(선택). 보내면 서버가 대조한다.",
                example = "d41d8cd98f00b204e9800998ecf8427e", nullable = true)
        String checksum
) {

    public CompleteCompanyDocumentUploadCommand toCommand(Long versionId, String requesterUserId, String role) {
        return new CompleteCompanyDocumentUploadCommand(versionId, checksum, requesterUserId, role);
    }
}
