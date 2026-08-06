package com.group3.vitamins.image.infrastructure.storage;

import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.image.application.port.ImageStoragePort;
import com.group3.vitamins.image.domain.exception.ImageErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * 이미지 저장소 구현체. S3 키는 {@code images/{imgBlockId}/{uuid}.{ext}} 로 둔다 — 회사/부서별
 * 접두사는 아직 그 개념 자체가 도메인에 없어 보류했고, {@code imgBlockId} 를 키에 포함해 나중에
 * 블록 기준으로 묶을 수 있게만 해둔다 (`.ai/api/image.md` §S3 저장 정책).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class S3ImageStorageAdapter implements ImageStoragePort {

    // GIF 는 리사이즈하면 애니메이션이 깨진다 (Thumbnailator 가 첫 프레임만 남김) — 원본 그대로 올린다.
    private static final Set<String> NON_RESIZABLE_EXTENSIONS = Set.of("gif");
    private static final int MAX_DIMENSION_PX = 1920;
    private static final long RESIZE_THRESHOLD_BYTES = 5L * 1024 * 1024;
    private static final Duration VIEW_URL_DURATION = Duration.ofHours(1);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public UploadedImage upload(Long imgBlockId, MultipartFile file, String extension) {
        byte[] original = readAllBytes(file);
        assertActualImageContentOrThrow(original, extension, file.getOriginalFilename());
        byte[] body = prepareBody(file, extension, original);
        String key = "images/" + imgBlockId + "/" + UUID.randomUUID() + "." + extension;

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentTypeOf(extension))
                        .contentLength((long) body.length)
                        .build(),
                RequestBody.fromInputStream(new ByteArrayInputStream(body), body.length)
        );

        log.info("이미지 업로드 완료 - imgBlockId={}, key={}, size={}", imgBlockId, key, body.length);

        return new UploadedImage(key, body.length);
    }

    @Override
    public String presignViewUrl(String storageKey) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(VIEW_URL_DURATION)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(storageKey).build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toExternalForm();
    }

    @Override
    public byte[] download(String storageKey) {
        return s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(storageKey).build()
        ).asByteArray();
    }

    @Override
    public void delete(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
        log.info("이미지 완전 삭제(S3 객체 제거) 완료 - key={}", storageKey);
    }

    @Override
    public String contentTypeOf(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private byte[] prepareBody(MultipartFile file, String extension, byte[] original) {
        if (NON_RESIZABLE_EXTENSIONS.contains(extension) || original.length <= RESIZE_THRESHOLD_BYTES) {
            return original;
        }

        try (InputStream input = new ByteArrayInputStream(original)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(input)
                    .size(MAX_DIMENSION_PX, MAX_DIMENSION_PX)
                    .keepAspectRatio(true)
                    .outputFormat(extension)
                    .toOutputStream(output);
            return output.toByteArray();
        } catch (IOException e) {
            // 리사이즈 실패(손상된 이미지 등)는 원본 그대로 올린다 — 업로드 자체를 막을 이유는 아니다.
            // 어차피 assertActualImageContentOrThrow를 이미 통과한 뒤라 콘텐츠 자체는 진짜 이미지다.
            log.warn("이미지 리사이즈 실패, 원본으로 업로드 - originalFilename={}", file.getOriginalFilename(), e);
            return original;
        }
    }

    private byte[] readAllBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedImageReadException(e);
        }
    }

    /**
     * 확장자(파일명 기준, 사용자가 임의로 붙일 수 있음)가 아니라 실제 바이트 내용이 그 확장자가
     * 맞는 이미지 포맷인지 매직 바이트로 확인한다. 확장자만 검사하면 HTML·스크립트 파일도 이름만
     * `.png`/`.gif`로 바꿔서 그대로 통과·저장될 수 있다(위장 업로드).
     */
    private void assertActualImageContentOrThrow(byte[] content, String extension, String originalFilename) {
        if (!matchesSignature(content, extension)) {
            log.warn("파일 내용이 확장자와 일치하지 않음(위장 업로드 의심) - originalFilename={}, extension={}",
                    originalFilename, extension);
            throw new ValidationException(ImageErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private boolean matchesSignature(byte[] content, String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> startsWith(content, 0xFF, 0xD8, 0xFF);
            case "png" -> startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
            // "GIF87a" 또는 "GIF89a"
            case "gif" -> startsWith(content, 0x47, 0x49, 0x46, 0x38, 0x37, 0x61)
                    || startsWith(content, 0x47, 0x49, 0x46, 0x38, 0x39, 0x61);
            // RIFF 컨테이너(offset 0) + WEBP(offset 8)
            case "webp" -> startsWith(content, 0x52, 0x49, 0x46, 0x46)
                    && content.length >= 12
                    && startsWith(Arrays.copyOfRange(content, 8, 12), 0x57, 0x45, 0x42, 0x50);
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

    private static class UncheckedImageReadException extends RuntimeException {
        UncheckedImageReadException(Throwable cause) {
            super(cause);
        }
    }
}
