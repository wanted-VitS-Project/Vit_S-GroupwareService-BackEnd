package com.group3.vitamins.employee.presentation.api.response;

import com.group3.vitamins.employee.application.result.PendingMasters;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "자동 생성 마스터 묶음 — §7 newMasters(생성 예정) · §8 createdMasters(새로 생성했거나 동명 마스터를 재사용한 것). autoCreateMasters=false 면 둘 다 빈 배열")
public record BulkMastersResponse(
        @Schema(description = "전공") List<BulkMasterPreviewResponse> majors,
        @Schema(description = "자격증") List<BulkMasterPreviewResponse> certificates
) {
    public static BulkMastersResponse from(PendingMasters m) {
        return new BulkMastersResponse(
                m.majors().stream().map(BulkMasterPreviewResponse::from).toList(),
                m.certificates().stream().map(BulkMasterPreviewResponse::from).toList());
    }
}
