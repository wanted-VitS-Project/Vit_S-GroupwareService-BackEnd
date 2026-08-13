package com.group3.vitamins.certificate.presentation.api.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자격증 삭제 응답")
public record CertificateDeleteResponse(
        @Schema(description = "삭제된 자격증 번호") Long certificateId
) {
}
