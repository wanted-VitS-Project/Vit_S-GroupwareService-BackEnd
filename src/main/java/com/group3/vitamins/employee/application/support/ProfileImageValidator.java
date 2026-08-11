package com.group3.vitamins.employee.application.support;

import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Set;

/**
 * 프로필 사진 업로드 파일 검증. 이미지 블록 도메인의 {@code ImageEligibilityPolicy} 와 같은 원칙을
 * 프로필 사진 계약(단건·5MB)에 맞춰 옮긴 것이다 — 확장자 화이트리스트 + 매직 바이트 + 실제 디코딩.
 *
 * <p>도메인 경계상 이미지 도메인 정책을 직접 재사용하지 않고 별도로 둔다(사원 → 이미지 의존을 만들지
 * 않기 위함). 두 곳을 공용 유틸로 합치는 것은 백로그(`.ai/local/STATE.md`).
 */
@Component
@Slf4j
public class ProfileImageValidator {

    // webp 제외 — JDK 에 내장 디코더가 없어 실제 디코딩·픽셀 수 검사를 못 한다. 매직바이트만으로 통과시키면
    // 대형 webp 가 픽셀 폭탄 상한을 우회한다(코드 리뷰 지적). 안전한 webp 검증기(외부 라이브러리)가 붙으면 그때 추가한다.
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;
    // 디코딩 폭탄 방어 — 바이트 상한(5MB)은 압축 해제 후 픽셀 메모리를 못 막는다(작은 파일이 수억 픽셀로 팽창 가능).
    // 실제 디코딩 전에 헤더의 가로×세로만 읽어 픽셀 수를 제한한다. 50MP 는 고화소 휴대폰 사진(48MP)까지는 통과.
    private static final long MAX_PIXELS = 50_000_000L;

    /**
     * 업로드 파일을 검증한다. 업로드를 시작하기 <b>전에</b> 전부 확인한다.
     *
     * @return 검증을 통과한 확장자(소문자, 점 없음)
     */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(EmployeeErrorCode.EMP_PROFILE_IMAGE_REQUIRED);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            log.warn("프로필 사진 용량 초과 - size={}", file.getSize());
            throw new ValidationException(EmployeeErrorCode.EMP_PROFILE_IMAGE_SIZE_EXCEEDED);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("지원하지 않는 프로필 사진 형식 - originalFilename={}", file.getOriginalFilename());
            throw new ValidationException(EmployeeErrorCode.EMP_PROFILE_IMAGE_TYPE_INVALID);
        }
        assertActualImageContent(file, extension);
        return extension;
    }

    /**
     * 확장자(이름)가 아니라 실제 바이트가 그 포맷의 이미지인지 매직 바이트로 확인하고(위장 업로드 방지),
     * {@code ImageIO} 로 실제 디코딩까지(픽셀 수 상한 검사 포함) 확인한다(헤더만 맞고 본문이 깨진 파일·디코딩 폭탄 차단).
     * 허용 포맷(jpg·jpeg·png·gif)은 모두 JDK 로 디코딩 가능하다 — webp 는 디코더가 없어 허용 목록에서 제외했다.
     */
    private void assertActualImageContent(MultipartFile file, String extension) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!matchesSignature(content, extension)) {
            log.warn("프로필 사진 내용이 확장자와 불일치(위장 의심) - originalFilename={}, extension={}",
                    file.getOriginalFilename(), extension);
            throw new ValidationException(EmployeeErrorCode.EMP_PROFILE_IMAGE_TYPE_INVALID);
        }
        if (!isDecodableImage(content)) {
            log.warn("프로필 사진 헤더는 맞지만 디코딩 실패/픽셀 과다(손상·위장·폭탄 의심) - originalFilename={}, extension={}",
                    file.getOriginalFilename(), extension);
            throw new ValidationException(EmployeeErrorCode.EMP_PROFILE_IMAGE_TYPE_INVALID);
        }
    }

    /**
     * 실제 디코딩까지 확인하되, <b>디코딩 전에</b> 헤더의 가로×세로로 픽셀 수를 먼저 검사해 디코딩 폭탄을 막는다.
     * {@code ImageIO.read} 를 바로 부르면 헤더가 주장하는 크기대로 전체를 메모리에 펼쳐 OOM 을 유발할 수 있다.
     */
    private boolean isDecodableImage(byte[] content) {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (iis == null) {
                return false;
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                return false;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels > MAX_PIXELS) {
                    log.warn("프로필 사진 픽셀 수 상한 초과(디코딩 폭탄 의심) - pixels={}", pixels);
                    return false;
                }
                return reader.read(0) != null;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            // getWidth/read 가 던지는 IOException·손상 이미지의 런타임 예외 모두 '디코딩 불가'로 본다.
            return false;
        }
    }

    private boolean matchesSignature(byte[] content, String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> startsWith(content, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            case "gif" -> startsWith(content, 0x47, 0x49, 0x46, 0x38, 0x37, 0x61)
                    || startsWith(content, 0x47, 0x49, 0x46, 0x38, 0x39, 0x61);
            default -> false;
        };
    }

    private boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase();
    }
}
