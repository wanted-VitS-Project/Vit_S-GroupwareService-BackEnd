package com.group3.vitamins.file.infrastructure.storage;

import com.group3.vitamins.file.application.port.FileStoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class S3FileStorageAdapter implements FileStoragePort {

    /** presigned 만료 (2026-08-06 확정). */
    private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(5);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3FileStorageAdapter(S3Client s3Client, S3Presigner s3Presigner,
                                @Value("${cloud.aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

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

    /** S3 DeleteObjects 요청당 최대 객체 수. */
    private static final int DELETE_BATCH_SIZE = 1000;

    @Override
    public int deleteObjects(Collection<String> storageKeys) {
        if (storageKeys == null || storageKeys.isEmpty()) {
            return 0;
        }
        List<String> keys = storageKeys.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        int deleted = 0;
        // DeleteObjects 는 요청당 최대 1000개 — 청크로 나눠 보낸다. 한 배치가 실패해도 다음 배치는 계속 시도한다.
        for (int from = 0; from < keys.size(); from += DELETE_BATCH_SIZE) {
            List<ObjectIdentifier> batch = keys.subList(from, Math.min(from + DELETE_BATCH_SIZE, keys.size())).stream()
                    .map(key -> ObjectIdentifier.builder().key(key).build())
                    .toList();
            try {
                DeleteObjectsResponse res = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucket)
                        .delete(Delete.builder().objects(batch).quiet(false).build())
                        .build());
                // 실제 삭제된 것만 집계한다. 개별 실패(res.errors())·아래 배치 실패의 키는 지금은 재시도하지 않는다 —
                // best-effort(§7). 고아 S3 객체의 내구성 있는 회수는 정기 reconciliation 배치로 분리(STATE 백로그).
                deleted += res.deleted().size();
            } catch (SdkException e) {
                // 네트워크·자격·S3 오류 포함. DB 는 이미 커밋됐고 남은 키는 정리 대상(백로그 reconciliation).
            }
        }
        return deleted;
    }

    @Override
    public void copyObject(String sourceStorageKey, String destStorageKey) {
        // 서버측 복사(입찰 검토 파일 귀속 §2-G). 50MB 상한이라 단순 CopyObject 로 충분(멀티파트 복사 불필요).
        // 원본이 없으면 NoSuchKeyException/404 S3Exception 이 그대로 전파된다 — 호출부가 버전을 FAILED 로 전이한다.
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucket)
                .sourceKey(sourceStorageKey)
                .destinationBucket(bucket)
                .destinationKey(destStorageKey)
                .build());
    }
}
