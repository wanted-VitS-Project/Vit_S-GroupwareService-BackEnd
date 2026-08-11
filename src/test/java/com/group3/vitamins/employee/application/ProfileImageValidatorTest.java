package com.group3.vitamins.employee.application;

import com.group3.vitamins.employee.application.support.ProfileImageValidator;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProfileImageValidator 프로필 사진 검증")
class ProfileImageValidatorTest {

    private ProfileImageValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ProfileImageValidator();
    }

    @Test
    @DisplayName("실제 PNG 는 통과하고 확장자(png)를 돌려준다")
    void acceptsRealPng() throws IOException {
        MultipartFile file = new MockMultipartFile("file", "me.png", "image/png", realPngBytes());
        assertThat(validator.validate(file)).isEqualTo("png");
    }

    @Test
    @DisplayName("파일이 없으면 EMP_PROFILE_IMAGE_REQUIRED")
    void rejectsEmptyFile() {
        MultipartFile file = new MockMultipartFile("file", "me.png", "image/png", new byte[0]);
        assertCode(file, EmployeeErrorCode.EMP_PROFILE_IMAGE_REQUIRED);
    }

    @Test
    @DisplayName("null 파일이면 EMP_PROFILE_IMAGE_REQUIRED")
    void rejectsNullFile() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getErrorCode())
                        .isEqualTo(EmployeeErrorCode.EMP_PROFILE_IMAGE_REQUIRED));
    }

    @Test
    @DisplayName("5MB 초과면 EMP_PROFILE_IMAGE_SIZE_EXCEEDED")
    void rejectsOversized() {
        byte[] tooBig = new byte[5 * 1024 * 1024 + 1];
        MultipartFile file = new MockMultipartFile("file", "me.png", "image/png", tooBig);
        assertCode(file, EmployeeErrorCode.EMP_PROFILE_IMAGE_SIZE_EXCEEDED);
    }

    @Test
    @DisplayName("허용하지 않는 확장자면 EMP_PROFILE_IMAGE_TYPE_INVALID")
    void rejectsUnsupportedExtension() {
        MultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertCode(file, EmployeeErrorCode.EMP_PROFILE_IMAGE_TYPE_INVALID);
    }

    @Test
    @DisplayName("확장자는 png 지만 내용이 이미지가 아니면(위장) EMP_PROFILE_IMAGE_TYPE_INVALID")
    void rejectsDisguisedFile() {
        MultipartFile file = new MockMultipartFile("file", "evil.png", "image/png", "not an image".getBytes());
        assertCode(file, EmployeeErrorCode.EMP_PROFILE_IMAGE_TYPE_INVALID);
    }

    @Test
    @DisplayName("PNG 헤더는 맞지만 본문이 깨졌으면(디코딩 실패) EMP_PROFILE_IMAGE_TYPE_INVALID")
    void rejectsCorruptedPngHeaderOnly() {
        byte[] pngSigOnly = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01, 0x02};
        MultipartFile file = new MockMultipartFile("file", "broken.png", "image/png", pngSigOnly);
        assertCode(file, EmployeeErrorCode.EMP_PROFILE_IMAGE_TYPE_INVALID);
    }

    private void assertCode(MultipartFile file, EmployeeErrorCode expected) {
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOf(DomainException.class)
                .satisfies(e -> assertThat(((DomainException) e).getErrorCode()).isEqualTo(expected));
    }

    private byte[] realPngBytes() throws IOException {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
