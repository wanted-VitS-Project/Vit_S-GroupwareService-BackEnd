package com.group3.vitamins.image.infrastructure.storage;

import com.group3.vitamins.image.application.port.ImageStoragePort;
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
        byte[] body = prepareBody(file, extension);
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

    private byte[] prepareBody(MultipartFile file, String extension) {
        if (NON_RESIZABLE_EXTENSIONS.contains(extension) || file.getSize() <= RESIZE_THRESHOLD_BYTES) {
            return readAllBytes(file);
        }

        try (InputStream input = file.getInputStream()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Thumbnails.of(input)
                    .size(MAX_DIMENSION_PX, MAX_DIMENSION_PX)
                    .keepAspectRatio(true)
                    .outputFormat(extension)
                    .toOutputStream(output);
            return output.toByteArray();
        } catch (IOException e) {
            // 리사이즈 실패(손상된 이미지 등)는 원본 그대로 올린다 — 업로드 자체를 막을 이유는 아니다.
            log.warn("이미지 리사이즈 실패, 원본으로 업로드 - originalFilename={}", file.getOriginalFilename(), e);
            return readAllBytes(file);
        }
    }

    private byte[] readAllBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedImageReadException(e);
        }
    }

    private static class UncheckedImageReadException extends RuntimeException {
        UncheckedImageReadException(Throwable cause) {
            super(cause);
        }
    }
}
