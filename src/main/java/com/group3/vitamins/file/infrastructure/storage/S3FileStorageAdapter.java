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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

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
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .responseContentDisposition("attachment; filename=\"" + originalFileName + "\"")
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
        }
    }
}
