package com.group3.vitamins.employee.application.usecase;

import org.springframework.web.multipart.MultipartFile;

/**
 * 프로필 사진 유스케이스 (`.ai/api/auth.md` §5-1·§5-2 · `.ai/api/employee.md` §10).
 *
 * <p>업로드/삭제는 본인만(마이페이지), 서빙(조회 URL)은 로그인 사용자 누구나. 저장 데이터는
 * 사원 속성이라 사원 도메인이 소유한다.
 */
public interface ProfileImageUseCase {

    /**
     * 내 프로필 사진을 등록/변경한다(멱등 — 기존 사진이 있으면 교체). 검증 → S3 업로드 → 키 저장 순.
     *
     * @return 저장된 프로필 사진 조회 URL (서빙 엔드포인트 경로)
     */
    String uploadMyProfileImage(String userId, MultipartFile file);

    /** 내 프로필 사진을 삭제한다(키를 비운다). 사진이 없어도 성공(멱등). S3 객체는 남긴다(소프트 정책). */
    void deleteMyProfileImage(String userId);

    /**
     * 서빙용 — 해당 사원의 프로필 사진을 열 수 있는 presigned URL 을 발급한다.
     *
     * @throws com.group3.vitamins.global.domain.common.error.exception.NotFoundException
     *         사원이 없으면 {@code EMP_NOT_FOUND}, 사진이 없으면 {@code EMP_PROFILE_IMAGE_NOT_FOUND}
     */
    String resolveViewUrl(String userId);
}
