package com.group3.vitamins.employee.presentation.api;

import com.group3.vitamins.employee.application.usecase.ProfileImageUseCase;
import com.group3.vitamins.employee.presentation.api.response.ProfileImageResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 마이페이지 프로필 사진 등록/삭제 API — 본인만 (`.ai/api/auth.md` §5-1·§5-2).
 *
 * <p>경로는 auth 마이페이지({@code /api/v1/auth/me/...})지만, 저장 데이터가 사원 속성이라 구현은 사원
 * 도메인이 소유한다. 서빙(조회)은 {@link EmployeeProfileImageController} 가 담당한다.
 */
@Tag(name = "Auth - 인증", description = "로그인 / 로그아웃 / 내 정보 / 비밀번호 변경 (담당: 김동현)")
@RestController
@RequestMapping("/api/v1/auth/me/profile-image")
@RequiredArgsConstructor
public class MyProfileImageController {

    private final ProfileImageUseCase profileImageUseCase;

    @Operation(summary = "프로필 사진 등록/변경",
            description = "본인 프로필 사진을 업로드한다(멱등 — 기존 사진이 있으면 교체). multipart/form-data 의 file 파트로 "
                    + "이미지 1장을 보낸다. 형식: jpg·jpeg·png·gif, 최대 5MB.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록/변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "EMP_PROFILE_IMAGE_REQUIRED(파일 없음) · EMP_PROFILE_IMAGE_TYPE_INVALID(형식/위장) · "
                            + "EMP_PROFILE_IMAGE_SIZE_EXCEEDED(5MB 초과)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProfileImageResponse> upload(
            @AuthenticationPrincipal String userId,
            // required=false 로 받아 "파일 없음" 을 Spring 기본 예외가 아니라 도메인 코드(EMP_PROFILE_IMAGE_REQUIRED)로 처리한다
            @RequestParam(value = "file", required = false) MultipartFile file) {
        String url = profileImageUseCase.uploadMyProfileImage(userId, file);
        return ApiResponse.success("프로필 사진 등록 성공", ProfileImageResponse.of(url));
    }

    @Operation(summary = "프로필 사진 삭제",
            description = "본인 프로필 사진을 삭제한다(기본 아바타로 돌아간다). 사진이 없어도 성공(멱등).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료")
    })
    @DeleteMapping
    public ApiResponse<Void> delete(@AuthenticationPrincipal String userId) {
        profileImageUseCase.deleteMyProfileImage(userId);
        return ApiResponse.success("프로필 사진 삭제 성공");
    }
}
