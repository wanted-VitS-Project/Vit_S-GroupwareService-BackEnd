package com.group3.vitamins.employee.application.service;

import com.group3.vitamins.employee.application.port.ProfileImageStoragePort;
import com.group3.vitamins.employee.application.support.ProfileImageValidator;
import com.group3.vitamins.employee.application.usecase.ProfileImageUseCase;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 프로필 사진 유스케이스 구현. 저장은 사원 속성({@code employee.profile_image_key})이라 사원 도메인이
 * 소유하고, 파일 저장은 {@link ProfileImageStoragePort}(S3), 검증은 {@link ProfileImageValidator} 가 맡는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileImageService implements ProfileImageUseCase {

    private final EmployeeRepository employeeRepository;
    private final ProfileImageStoragePort profileImageStoragePort;
    private final ProfileImageValidator profileImageValidator;

    @Override
    @Transactional
    public String uploadMyProfileImage(String userId, MultipartFile file) {
        // 검증(확장자·용량·매직바이트·디코딩)을 업로드 시작 전에 끝낸다 — 통과 못 하면 S3 를 건드리지 않는다.
        String extension = profileImageValidator.validate(file);
        // 교체여도 이전 S3 객체는 지우지 않는다(소프트 정책, 이미지 도메인과 통일 — 하드삭제 정책 대기).
        String key = profileImageStoragePort.upload(userId, file, extension);
        employeeRepository.updateProfileImageKey(userId, key);
        log.info("프로필 사진 등록/변경 - userId={}", userId);
        return ProfileImagePath.of(userId);
    }

    @Override
    @Transactional
    public void deleteMyProfileImage(String userId) {
        // 키만 비운다. 사진이 없어도(이미 null) 그대로 성공 — 멱등. S3 객체는 남긴다.
        employeeRepository.updateProfileImageKey(userId, null);
        log.info("프로필 사진 삭제 - userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveViewUrl(String userId) {
        if (!employeeRepository.existsById(userId)) {
            throw new NotFoundException(EmployeeErrorCode.EMP_NOT_FOUND);
        }
        String key = employeeRepository.findProfileImageKey(userId).orElse(null);
        if (key == null) {
            throw new NotFoundException(EmployeeErrorCode.EMP_PROFILE_IMAGE_NOT_FOUND);
        }
        return profileImageStoragePort.presignViewUrl(key);
    }

    /** 서빙 엔드포인트 경로 — 응답의 {@code profileImageUrl} 값. auth {@code /me} 도 같은 경로를 만든다. */
    private static final class ProfileImagePath {
        static String of(String userId) {
            return "/api/v1/employees/" + userId + "/profile-image";
        }
    }
}
