package com.group3.vitamins.certificate.presentation.api.response;

import com.group3.vitamins.certificate.application.result.CertificateResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자격증 생성·수정 응답")
public record CertificateResponse(
        @Schema(description = "자격증 번호") Long certificateId,
        @Schema(description = "자격증명") String name
) {

    public static CertificateResponse from(CertificateResult r) {
        return new CertificateResponse(r.certificateId(), r.name());
    }
}
