package com.group3.vitamins.employee.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * S3ProfileImageStorageAdapter 를 실제 S3 호환 서버(MinIO)에 대고 검증하는 통합 테스트.
 *
 * <p>업로드(우리 키 스킴·리사이즈)와 presign 왕복이 진짜 S3 프로토콜에서 동작하는지 확인한다 —
 * upload → presignViewUrl → 그 URL 로 HTTP GET → 바이트가 실제로 서빙되는지까지.
 *
 * <p>CI 처럼 MinIO 가 없는 환경에서는 자동으로 건너뛴다({@code MINIO_ENDPOINT} 환경변수가 있을 때만 실행).
 * 엔드포인트·자격증명은 <b>전부 환경변수로만</b> 주입한다 — PUBLIC 레포라 실제 값·기본 자격증명을 코드/문서에 두지 않는다(§6).
 * 실행 예:
 * <pre>
 *   MINIO_ENDPOINT=&lt;minio-url&gt; MINIO_ACCESS_KEY=&lt;key&gt; MINIO_SECRET_KEY=&lt;secret&gt; S3_BUCKET_NAME=&lt;bucket&gt; \
 *   ./gradlew test --tests '*S3ProfileImageStorageAdapterIT' -x jacocoTestReport
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "MINIO_ENDPOINT", matches = ".+")
@DisplayName("S3ProfileImageStorageAdapter × MinIO 통합")
class S3ProfileImageStorageAdapterIT {

    private static final String USER_ID = "vitas-EMP001";

    private S3Client s3Client;
    private S3ProfileImageStorageAdapter adapter;
    private String bucket;

    @BeforeEach
    void setUp() {
        // 엔드포인트·자격증명은 환경변수 필수 — 기본값(특히 자격증명)을 코드에 두지 않는다(PUBLIC 레포 §6).
        String endpoint = requireEnv("MINIO_ENDPOINT");
        String accessKey = requireEnv("MINIO_ACCESS_KEY");
        String secretKey = requireEnv("MINIO_SECRET_KEY");
        bucket = envOr("S3_BUCKET_NAME", "vitamins-it");

        StaticCredentialsProvider creds = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey));

        s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(creds)
                // MinIO 는 기본이 path-style(endpoint/bucket/key) 이라 virtual-host style 을 끈다.
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(creds)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();

        createBucketIfAbsent(bucket);

        adapter = new S3ProfileImageStorageAdapter(s3Client, presigner);
        ReflectionTestUtils.setField(adapter, "bucket", bucket);
    }

    @Test
    @DisplayName("업로드 → presign → HTTP GET 으로 실제 이미지가 서빙된다")
    void uploadThenPresignServesImage() throws Exception {
        byte[] png = pngBytes(64, 64, false);
        MockMultipartFile file = new MockMultipartFile("file", "me.png", "image/png", png);

        String key = adapter.upload(USER_ID, file, "png");
        assertThat(key).startsWith("profile-images/" + USER_ID + "/").endsWith(".png");

        String url = adapter.presignViewUrl(key);
        HttpResponse<byte[]> resp = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(ImageIO.read(new ByteArrayInputStream(resp.body())))
                .as("presigned URL 로 받은 바이트가 디코딩 가능한 이미지여야 한다")
                .isNotNull();
    }

    @Test
    @DisplayName("임계값(512KB)을 넘는 이미지는 512px 이하로 축소되어 저장된다")
    void largeImageIsResized() {
        // 랜덤 픽셀로 채워 압축이 잘 안 되게 → 512KB 초과 유도
        byte[] big = pngBytes(1200, 1200, true);
        assertThat(big.length).isGreaterThan(512 * 1024);

        MockMultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);
        String key = adapter.upload(USER_ID, file, "png");

        byte[] stored = s3Client.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(key).build())
                .asByteArray();
        BufferedImage image = read(stored);
        assertThat(image.getWidth()).isLessThanOrEqualTo(512);
        assertThat(image.getHeight()).isLessThanOrEqualTo(512);
    }

    // ===== 도구 =====

    private void createBucketIfAbsent(String bucket) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (RuntimeException alreadyExists) {
            // 이미 있으면 무시 (BucketAlreadyOwnedByYou/BucketAlreadyExists)
        }
    }

    private byte[] pngBytes(int w, int h, boolean randomPixels) {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        if (randomPixels) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    image.setRGB(x, y, ThreadLocalRandom.current().nextInt(0xFFFFFF));
                }
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", out);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }

    private BufferedImage read(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    private static String requireEnv(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("이 통합 테스트는 환경변수 " + key + " 가 필요합니다 (PUBLIC 레포라 기본값을 두지 않음).");
        }
        return v;
    }
}
