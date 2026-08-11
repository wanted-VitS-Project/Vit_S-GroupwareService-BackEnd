package com.group3.vitamins.employee.application;

import com.group3.vitamins.employee.application.port.ProfileImageStoragePort;
import com.group3.vitamins.employee.application.service.ProfileImageService;
import com.group3.vitamins.employee.application.support.ProfileImageValidator;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ProfileImageService 프로필 사진")
class ProfileImageServiceTest {

    private static final String USER_ID = "EMP001";
    private static final String KEY = "profile-images/EMP001/abc.png";
    private static final String EXPECTED_URL = "/api/v1/employees/EMP001/profile-image";

    private EmployeeRepository employeeRepository;
    private ProfileImageStoragePort storagePort;
    private ProfileImageValidator validator;
    private ProfileImageService service;

    @BeforeEach
    void setUp() {
        employeeRepository = Mockito.mock(EmployeeRepository.class);
        storagePort = Mockito.mock(ProfileImageStoragePort.class);
        validator = Mockito.mock(ProfileImageValidator.class);
        service = new ProfileImageService(employeeRepository, storagePort, validator);
    }

    @Test
    @DisplayName("업로드: 검증 → S3 업로드 → 키 저장, 서빙 경로를 돌려준다")
    void uploadStoresKeyAndReturnsServingPath() {
        MultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[]{1, 2, 3});
        when(validator.validate(file)).thenReturn("png");
        when(storagePort.upload(USER_ID, file, "png")).thenReturn(KEY);

        String url = service.uploadMyProfileImage(USER_ID, file);

        assertThat(url).isEqualTo(EXPECTED_URL);
        verify(employeeRepository).updateProfileImageKey(USER_ID, KEY);
    }

    @Test
    @DisplayName("업로드: 검증 실패면 S3 를 건드리지 않고 키도 저장하지 않는다")
    void uploadDoesNotTouchStorageWhenValidationFails() {
        MultipartFile file = new MockMultipartFile("file", "x.txt", "text/plain", new byte[]{1});
        when(validator.validate(file))
                .thenThrow(new com.group3.vitamins.global.domain.common.error.exception.ValidationException(
                        EmployeeErrorCode.EMP_PROFILE_IMAGE_TYPE_INVALID));

        assertThatThrownBy(() -> service.uploadMyProfileImage(USER_ID, file))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                        .isEqualTo(EmployeeErrorCode.EMP_PROFILE_IMAGE_TYPE_INVALID));

        verify(storagePort, never()).upload(any(), any(), any());
        verify(employeeRepository, never()).updateProfileImageKey(any(), any());
    }

    @Test
    @DisplayName("삭제: 키를 null 로 비운다 (멱등)")
    void deleteClearsKey() {
        service.deleteMyProfileImage(USER_ID);
        verify(employeeRepository).updateProfileImageKey(USER_ID, null);
    }

    @Test
    @DisplayName("서빙: 사진이 있으면 presigned URL 을 발급한다")
    void resolveReturnsPresignedUrl() {
        when(employeeRepository.existsById(USER_ID)).thenReturn(true);
        when(employeeRepository.findProfileImageKey(USER_ID)).thenReturn(Optional.of(KEY));
        when(storagePort.presignViewUrl(KEY)).thenReturn("https://s3.example/abc.png?sig=1");

        assertThat(service.resolveViewUrl(USER_ID)).isEqualTo("https://s3.example/abc.png?sig=1");
    }

    @Test
    @DisplayName("서빙: 사원이 없으면 EMP_NOT_FOUND")
    void resolveThrowsWhenEmployeeMissing() {
        when(employeeRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.resolveViewUrl(USER_ID))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                        .isEqualTo(EmployeeErrorCode.EMP_NOT_FOUND));
        verify(storagePort, never()).presignViewUrl(any());
    }

    @Test
    @DisplayName("서빙: 사원은 있으나 사진이 없으면 EMP_PROFILE_IMAGE_NOT_FOUND")
    void resolveThrowsWhenNoImage() {
        when(employeeRepository.existsById(USER_ID)).thenReturn(true);
        when(employeeRepository.findProfileImageKey(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveViewUrl(USER_ID))
                .isInstanceOf(NotFoundException.class)
                .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                        .isEqualTo(EmployeeErrorCode.EMP_PROFILE_IMAGE_NOT_FOUND));
        verify(storagePort, never()).presignViewUrl(any());
    }
}
