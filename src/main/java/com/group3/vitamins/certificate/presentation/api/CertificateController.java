package com.group3.vitamins.certificate.presentation.api;

import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import com.group3.vitamins.certificate.application.query.CertificateListQuery;
import com.group3.vitamins.certificate.application.usecase.CertificateCommandUseCase;
import com.group3.vitamins.certificate.application.usecase.CertificateQueryUseCase;
import com.group3.vitamins.certificate.presentation.api.request.CertificateCreateRequest;
import com.group3.vitamins.certificate.presentation.api.request.CertificateUpdateRequest;
import com.group3.vitamins.certificate.presentation.api.response.CertificateDeleteResponse;
import com.group3.vitamins.certificate.presentation.api.response.CertificateListResponse;
import com.group3.vitamins.certificate.presentation.api.response.CertificateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Certificate - 자격증 마스터", description = "자격증 마스터 CRUD — 사원 자격증(employee_certificate)의 원본 (ADMIN 전용 · HR-V1 CRT)")
@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
public class CertificateController {

    private final CertificateCommandUseCase certificateCommandUseCase;
    private final CertificateQueryUseCase certificateQueryUseCase;

    @Operation(summary = "자격증 마스터 목록",
            description = "회사의 자격증 목록을 사용 사원 수와 함께 이름 오름차순으로 조회한다. ADMIN 전용. keyword 로 이름 검색.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED")
    })
    @GetMapping
    public ApiResponse<CertificateListResponse> list(
            @RequestParam(required = false) String keyword,
            Authentication authentication
    ) {
        return ApiResponse.success(CertificateResponseMessage.CERT_LIST, CertificateListResponse.from(
                certificateQueryUseCase.list(new CertificateListQuery(keyword, RequesterRole.from(authentication)))));
    }

    @Operation(summary = "자격증 생성", description = "새 자격증을 만든다. 회사 내 이름 UNIQUE. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CERT_INVALID_REQUEST"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "CERT_NAME_DUPLICATED")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CertificateResponse> create(
            @RequestBody CertificateCreateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.created(CertificateResponseMessage.CERT_CREATED, CertificateResponse.from(
                certificateCommandUseCase.create(request.toCommand(RequesterRole.from(authentication)))));
    }

    @Operation(summary = "자격증 수정", description = "자격증명을 수정한다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "CERT_INVALID_REQUEST"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CERT_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "CERT_NAME_DUPLICATED")
    })
    @PatchMapping("/{certificateId}")
    public ApiResponse<CertificateResponse> update(
            @PathVariable Long certificateId,
            @RequestBody CertificateUpdateRequest request,
            Authentication authentication
    ) {
        return ApiResponse.success(CertificateResponseMessage.CERT_UPDATED, CertificateResponse.from(
                certificateCommandUseCase.update(request.toCommand(certificateId, RequesterRole.from(authentication)))));
    }

    @Operation(summary = "자격증 삭제",
            description = "자격증을 삭제한다(hard delete). 참조하는 사원 자격증이 있으면 CERT_IN_USE 로 막는다. ADMIN 전용.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "CERT_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "CERT_IN_USE — 사용 중")
    })
    @DeleteMapping("/{certificateId}")
    public ApiResponse<CertificateDeleteResponse> delete(
            @PathVariable Long certificateId,
            Authentication authentication
    ) {
        certificateCommandUseCase.delete(new com.group3.vitamins.certificate.application.command.DeleteCertificateCommand(
                certificateId, RequesterRole.from(authentication)));
        return ApiResponse.success(CertificateResponseMessage.CERT_DELETED, new CertificateDeleteResponse(certificateId));
    }
}
