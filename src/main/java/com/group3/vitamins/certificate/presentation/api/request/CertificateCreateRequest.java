package com.group3.vitamins.certificate.presentation.api.request;

import com.group3.vitamins.certificate.application.command.CreateCertificateCommand;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자격증 생성 요청")
public record CertificateCreateRequest(
        @Schema(description = "자격증명(최대 100자)", example = "컴퓨터공학")
        String name
) {

    public CreateCertificateCommand toCommand(String role) {
        return new CreateCertificateCommand(name, role);
    }
}
