package com.group3.vitamins.certificate.presentation.api.response;

import com.group3.vitamins.certificate.application.result.CertificateListItemResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "자격증 마스터 목록 응답")
public record CertificateListResponse(
        @Schema(description = "자격증 목록") List<Item> certificates
) {

    @Schema(description = "자격증 항목")
    public record Item(
            @Schema(description = "자격증 번호") Long certificateId,
            @Schema(description = "자격증명") String name,
            @Schema(description = "사용 사원 수(시스템·퇴사 제외)") int employeeCount,
            @Schema(description = "삭제 가능 여부(employeeCount == 0)") boolean deletable
    ) {
    }

    public static CertificateListResponse from(List<CertificateListItemResult> results) {
        List<Item> items = results.stream()
                .map(r -> new Item(r.certificateId(), r.name(), r.employeeCount(), r.deletable()))
                .toList();
        return new CertificateListResponse(items);
    }
}
