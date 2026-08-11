package com.group3.vitamins.employee.presentation.api;

import com.group3.vitamins.employee.application.usecase.ProfileImageUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 프로필 사진 서빙(아바타) API — 로그인 사용자 누구나 (`.ai/api/employee.md` §10).
 *
 * <p>사원 목록 API(ADMIN 전용)와 달리 이 하위 리소스만 로그인 사용자 전체에 열린다 — 좌상단·프로젝트
 * 멤버·결재선 아바타가 남의 사진을 그려야 하기 때문. 목록/카드 응답의 {@code profileImageUrl}(= 이 경로)을
 * 프론트가 {@code <img src>} 에 그대로 박으면 된다.
 *
 * <p>presigned S3 URL 로 <b>302 redirect</b> 한다(트래픽이 S3 로 직행 → 서버 부하 최소화). presigned 는
 * 1시간마다 만료되므로 이 응답을 캐시하면 안 된다({@code no-store}) — 매 조회가 이 엔드포인트를 거쳐
 * 항상 새로 서명받는다. 업로드/삭제(본인만)는 {@link MyProfileImageController}.
 */
@Tag(name = "Employee - 사원", description = "사원 목록·상세·검색·등록·수정·퇴사 — 담당: 김동현")
@RestController
@RequiredArgsConstructor
public class EmployeeProfileImageController {

    private final ProfileImageUseCase profileImageUseCase;

    @Operation(summary = "프로필 사진 조회 (아바타 서빙)",
            description = "해당 사원의 프로필 사진 presigned URL 로 302 redirect 한다. 로그인 사용자 누구나 호출한다. "
                    + "사진이 없는 사원(profileImageUrl=null)에는 프론트가 이 API 를 부르지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "presigned URL 로 redirect"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "EMP_PROFILE_IMAGE_NOT_FOUND(사진 없음) · EMP_NOT_FOUND(사원 없음)")
    })
    @GetMapping("/api/v1/employees/{userId}/profile-image")
    public ResponseEntity<Void> serve(
            @Parameter(description = "사번", example = "vitas-EMP001") @PathVariable String userId) {
        String presignedUrl = profileImageUseCase.resolveViewUrl(userId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(presignedUrl))
                // presigned 는 곧 만료되므로 redirect 를 캐시하면 안 된다 — 매번 새로 서명받게 한다.
                .cacheControl(CacheControl.noStore().cachePrivate())
                .build();
    }
}
