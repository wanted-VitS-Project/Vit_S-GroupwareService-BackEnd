package com.group3.vitamins.employee.infrastructure.storage;

import com.group3.vitamins.employee.application.port.ProfileImageStoragePort;
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
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

/**
 * 프로필 사진 저장소 구현체. S3 키는 {@code profile-images/{userId}/{uuid}.{ext}} 로 둔다.
 * S3Client·S3Presigner 빈과 버킷 설정({@code cloud.aws.s3.bucket})은 file 도메인의 {@code S3Config} 가
 * 제공하는 공용 빈을 그대로 쓴다.
 *
 * <p>아바타는 여러 화면에서 작게 반복 노출되므로 원본을 그대로 둘 이유가 없어, 임계값을 넘는 이미지는
 * 최대 512px 로 축소해 저장한다(원본 훼손 없이 저장 용량·전송량 절약). {@code gif}·{@code webp} 는
 * 리사이즈에서 제외한다 — gif 는 애니메이션이 깨지고, webp 는 JDK 에 인코더가 없다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class S3ProfileImageStorageAdapter implements ProfileImageStoragePort {

    // gif 는 리사이즈하면 애니메이션이 깨진다(Thumbnailator 가 첫 프레임만 남김) — 원본 그대로 올린다.
    private static final Set<String> NON_RESIZABLE_EXTENSIONS = Set.of("gif");
    private static final int MAX_DIMENSION_PX = 512;
    private static final long RESIZE_THRESHOLD_BYTES = 512L * 1024;
    private static final Duration VIEW_URL_DURATION = Duration.ofHours(1);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public String upload(String userId, MultipartFile file, String extension) {
        // 콘텐츠가 진짜 이미지인지(위장 업로드 방지)는 호출자(ProfileImageService)가 업로드 전에
        // ProfileImageValidator 로 이미 검증했다 — 여기서는 다시 확인하지 않는다.
        byte[] original = readAllBytes(file);
        byte[] body = prepareBody(extension, original);
        String key = "profile-images/" + userId + "/" + UUID.randomUUID() + "." + extension;

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentTypeOf(extension))
                            .contentLength((long) body.length)
                            .build(),
                    RequestBody.fromInputStream(new ByteArrayInputStream(body), body.length)
            );
        } catch (RuntimeException e) {
            // putObject 응답만 유실되고 객체는 실제로 만들어진 경우(네트워크 타임아웃 등) 고아 객체가 남을 수
            // 있어 최선 노력으로 지운다(이미지 도메인 어댑터와 동일 방어). 삭제가 실패해도 원래 예외를 던진다.
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            } catch (RuntimeException deleteFailure) {
                log.error("프로필 사진 업로드 실패 후 정리 삭제도 실패 - key={}", key, deleteFailure);
            }
            throw e;
        }

        log.info("프로필 사진 업로드 완료 - userId={}, key={}, size={}", userId, key, body.length);
        return key;
    }

    @Override
    public void delete(String storageKey) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(storageKey).build());
        log.info("프로필 사진 S3 객체 삭제 - key={}", storageKey);
    }

    @Override
    public String presignViewUrl(String storageKey) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(VIEW_URL_DURATION)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(storageKey).build())
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toExternalForm();
    }

    private String contentTypeOf(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private byte[] prepareBody(String extension, byte[] original) {
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
            // 리사이즈 실패(손상 이미지 등)는 원본 그대로 올린다 — 이미 콘텐츠 검증을 통과한 진짜 이미지다.
            log.warn("프로필 사진 리사이즈 실패, 원본으로 업로드 - size={}", original.length, e);
            return original;
        }
    }

    private byte[] readAllBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
