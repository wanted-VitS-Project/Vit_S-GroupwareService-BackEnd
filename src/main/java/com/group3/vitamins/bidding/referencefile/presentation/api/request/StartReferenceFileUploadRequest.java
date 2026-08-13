package com.group3.vitamins.bidding.referencefile.presentation.api.request;

import com.group3.vitamins.bidding.referencefile.application.command.StartReferenceFileUploadCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartReferenceFileUploadRequest(

        @NotBlank(message = "BIDDING_INVALID_REFERENCE_FILE_REQUEST|파일명을 입력해 주세요.")
        @Schema(description = "경로 문자를 제거한 원본 파일명", example = "원가계산_기준.pdf")
        String fileName,

        @NotBlank(message = "BIDDING_INVALID_REFERENCE_FILE_REQUEST|MIME 타입을 입력해 주세요.")
        @Schema(description = "파일 MIME 타입", example = "application/pdf")
        String mimeType,

        @NotNull(message = "BIDDING_INVALID_REFERENCE_FILE_REQUEST|파일 크기를 입력해 주세요.")
        @Min(value = 1, message = "BIDDING_INVALID_REFERENCE_FILE_REQUEST|파일 크기가 올바르지 않습니다.")
        @Max(value = 52428800, message = "BIDDING_INVALID_REFERENCE_FILE_REQUEST|파일은 50MB를 넘을 수 없습니다.")
        @Schema(description = "파일 크기(바이트)", example = "204800")
        Long sizeBytes
) {

    public StartReferenceFileUploadCommand toCommand(String userId, String role) {
        return new StartReferenceFileUploadCommand(fileName, mimeType, sizeBytes, userId, role);
    }
}