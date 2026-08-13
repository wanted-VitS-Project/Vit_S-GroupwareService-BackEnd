package com.group3.vitamins.certificate.presentation.api.request;

import com.group3.vitamins.certificate.application.command.UpdateCertificateCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자격증 수정 요청")
public record CertificateUpdateRequest(
        @Schema(description = "새 자격증명(최대 100자)", example = "소프트웨어공학")
        String name
) {

    public UpdateCertificateCommand toCommand(Long certificateId, String role) {
        return new UpdateCertificateCommand(certificateId, name, role);
    }
}
