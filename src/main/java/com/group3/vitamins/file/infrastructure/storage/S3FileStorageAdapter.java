package com.group3.vitamins.file.infrastructure.storage;

import com.group3.vitamins.file.application.port.FileStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class S3FileStorageAdapter implements FileStoragePort {

    /** presigned 만료 (2026-08-06 확정). */
    private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Override
    public PresignedUrl presignUpload(String storageKey, String contentType, long sizeBytes) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .contentType(contentType)
                .build();

        var presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                .signatureDuration(UPLOAD_TTL)
                .putObjectRequest(put)
                .build());

        return new PresignedUrl(presigned.url().toString(), presigned.expiration());
    }

    @Override
    public PresignedUrl presignDownload(String storageKey, String originalFileName) {
        // RFC 5987 — 한글 등 비ASCII 파일명이 깨지지 않도록 filename*=UTF-8'' 로 인코딩한다.
        String encoded = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8).replace("+", "%20");
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .responseContentDisposition("attachment; filename*=UTF-8''" + encoded)
                .build();

        var presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(DOWNLOAD_TTL)
                .getObjectRequest(get)
                .build());

        return new PresignedUrl(presigned.url().toString(), presigned.expiration());
    }

    @Override
    public Optional<StoredObject> head(String storageKey) {
        try {
            HeadObjectResponse res = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build());
            return Optional.of(new StoredObject(res.contentLength()));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            // HEAD 는 본문이 없어 NoSuchKey 대신 404 S3Exception 으로 오는 경우가 있다.
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    @Override
    public byte[] getObject(String storageKey) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build()).asByteArray();
    }
}
