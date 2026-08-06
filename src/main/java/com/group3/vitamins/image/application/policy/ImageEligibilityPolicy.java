package com.group3.vitamins.image.application.policy;

import com.group3.vitamins.image.application.port.ImageStepLookupPort;
import com.group3.vitamins.image.domain.exception.ImageErrorCode;
import com.group3.vitamins.image.domain.model.ImageItem;
import com.group3.vitamins.image.domain.repository.ImageBlockRepository;
import com.group3.vitamins.image.domain.repository.ImageRepository;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
import com.group3.vitamins.text.application.port.BlockCatalogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ImageEligibilityPolicy {

    private static final String BLOCK_TYPE = "IMAGE";

    // 명세에 화이트리스트가 없어 구현 시 임의 결정 (.ai/api/image.md §S3 저장 정책 참고).
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final BlockCatalogPort blockCatalogPort;
    private final ImageBlockRepository imageBlockRepository;
    private final ImageRepository imageRepository;
    private final ImageStepLookupPort imageStepLookupPort;
    private final StepAccessUseCase stepAccessUseCase;

    public void assertBlockActiveOrThrow(Long imgBlockId) {
        if (!imageBlockRepository.existsActive(imgBlockId)) {
            log.warn("이미지 블록 존재하지 않음 - imgBlockId={}", imgBlockId);
            throw new NotFoundException(ImageErrorCode.BLOCK_NOT_FOUND);
        }
    }

    /**
     * 복구 API 전용 — {@link #assertBlockActiveOrThrow} 와 같은 조회(image_block 자체 테이블, 공유
     * Block 도메인과 무관)를 쓰지만, "블록이 아예 없음"(IMG-003, 생성·수정 API 용)이 아니라
     * "있었는데 삭제되어 되돌릴 자리가 없음"이라는 걸 명확히 알려주기 위해 별도 코드로 던진다.
     */
    public void assertBlockActiveForRestoreOrThrow(Long imgBlockId) {
        if (!imageBlockRepository.existsActive(imgBlockId)) {
            log.warn("복구 대상 이미지의 블록이 삭제됨 - imgBlockId={}", imgBlockId);
            throw new NotFoundException(ImageErrorCode.BLOCK_DELETED_CANNOT_RESTORE);
        }
    }

    /**
     * 조회(GET)용 — {@link #assertBlockActiveOrThrow} 는 내부적으로 PESSIMISTIC_WRITE 락 조회를 써서
     * 읽기 전용 트랜잭션에서 부르면 DB가 거부한다. 락이 필요 없는 단순 조회 화면(이미지 항목 조회 등)은 이걸 쓴다.
     */
    public void assertBlockActiveOrThrowReadOnly(Long imgBlockId) {
        if (!imageBlockRepository.existsActiveReadOnly(imgBlockId)) {
            log.warn("이미지 블록 존재하지 않음 - imgBlockId={}", imgBlockId);
            throw new NotFoundException(ImageErrorCode.BLOCK_NOT_FOUND);
        }
    }

    public ImageItem getActiveItemOrThrow(Long imgId) {
        return imageRepository.findActiveByImgId(imgId)
                .orElseThrow(() -> {
                    log.warn("이미지 항목 존재하지 않음 - imgId={}", imgId);
                    return new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
                });
    }

    public void assertEditPermission(Long imgBlockId, String userId, String role) {
        if (!blockCatalogPort.hasEditPermission(BLOCK_TYPE, imgBlockId, userId, role)) {
            log.warn("편집 권한 없음 - blockType={}, imgBlockId={}, userId={}", BLOCK_TYPE, imgBlockId, userId);
            throw new ForbiddenException(ImageErrorCode.FORBIDDEN);
        }
    }

    /**
     * 복구·완전 삭제 전용 — 블록이 삭제돼 있어도 정확한 편집 권한 판정을 한다. 블록이 살아있으면
     * 평소처럼 {@link #assertEditPermission}(공유 {@code BlockCatalogPort}, 실패 시 IMG-002)을 쓴다.
     * 블록이 삭제돼 있으면 {@code BlockCatalogPort.hasEditPermission}이 대상을 못 찾아 권한 유무와
     * 무관하게 항상 false만 반환하므로(§생성·수정 API와 동일 포트), 그 경로를 안 타고 {@link ImageStepLookupPort}
     * 로 이 블록이 속했던 stepId를 직접 찾아(image_block→block, 삭제 여부 무시) 그대로 존재하는
     * {@code StepAccessUseCase.requireEditable}로 판정한다 — 실제 권한 규칙은 여전히 Step 도메인이
     * 소유한다, 여기선 ID만 찾는다.
     *
     * <p>⚠️ {@code ImageStepLookupPort} 구현체는 이미지 도메인 소유다(동훈님께 요청하는 게 아님) — Block
     * 도메인에 "삭제된 블록도 포함해서 stepId를 찾는" 정식 포트가 생기면 이 포트의 구현체만 교체하면 된다
     * (2026-08-06 결정, `.ai/api/image.md` 참고).
     *
     * <p>{@code StepAccessUseCase}가 던지는 예외(Step 도메인 코드 `STEP_EDIT_DENIED`/`STEP_NOT_FOUND`)는
     * 이미지 API 계약 밖의 코드라 그대로 노출하지 않고 이미지 도메인 코드(IMG-002)로 감싼다.
     */
    public void assertEditPermissionEvenIfBlockDeleted(Long imgBlockId, String userId, String role) {
        if (imageBlockRepository.existsActive(imgBlockId)) {
            assertEditPermission(imgBlockId, userId, role);
            return;
        }

        Long stepId = imageStepLookupPort.findStepIdByImgBlockId(imgBlockId)
                .orElseThrow(() -> new IllegalStateException(
                        "image_block이 가리키는 block 행을 찾을 수 없습니다 — 데이터 정합성 문제: " + imgBlockId));

        log.info("블록 삭제된 이미지 - 스텝 편집 권한으로 대체 판정 - imgBlockId={}, stepId={}", imgBlockId, stepId);
        try {
            stepAccessUseCase.requireEditable(stepId, userId, role);
        } catch (NotFoundException | ForbiddenException e) {
            // 응답 코드는 그대로 IMG-002로 감싸지만, 원인(Step 도메인이 실제로 왜 거부했는지 —
            // 스텝 자체가 없는 건지, 편집 권한이 없는 건지)은 cause로 남겨서 운영 중 추적 가능하게 한다.
            log.warn("블록 삭제된 이미지 - 스텝 편집 권한 없음 - imgBlockId={}, stepId={}", imgBlockId, stepId, e);
            throw new ForbiddenException(ImageErrorCode.FORBIDDEN, e);
        }
    }

    /** 이미지 항목 조회(GET)는 편집 권한이 아니라 접근(VIEWER 이상) 권한만 있으면 된다. */
    public void assertViewPermission(Long imgBlockId, String userId, String role) {
        if (!blockCatalogPort.hasViewPermission(BLOCK_TYPE, imgBlockId, userId, role)) {
            log.warn("접근 권한 없음 - blockType={}, imgBlockId={}, userId={}", BLOCK_TYPE, imgBlockId, userId);
            throw new ForbiddenException(ImageErrorCode.VIEW_FORBIDDEN);
        }
    }

    /** 전체 다운로드(zip) 파일명용 — 활동 로그 Block명 조회와 같은 포트를 재사용한다. */
    public String getBlockTitle(Long imgBlockId) {
        return blockCatalogPort.getBlockTitle(BLOCK_TYPE, imgBlockId);
    }

    /** @return 검증을 통과한 확장자(소문자, 점 없음) */
    public String assertSupportedExtensionOrThrow(String originalFilename) {
        String extension = extractExtension(originalFilename);
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("지원하지 않는 파일 형식 - originalFilename={}", originalFilename);
            throw new ValidationException(ImageErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        return extension;
    }

    /**
     * 확장자(파일명 기준, 사용자가 임의로 붙일 수 있음)가 아니라 실제 바이트 내용이 그 확장자가 맞는
     * 이미지 포맷인지 매직 바이트로 확인한다. 확장자만 검사하면 HTML·스크립트 파일도 이름만
     * `.png`/`.gif`로 바꿔서 그대로 통과·저장될 수 있다(위장 업로드).
     *
     * <p>{@link #assertSupportedExtensionOrThrow}와 같은 이유로 업로드를 시작하기 *전에* 파일 전부를
     * 검증해야 한다 — 처음엔 이 검증을 업로드 어댑터(S3ImageStorageAdapter) 안에 파일마다 두어서,
     * 뒤쪽 파일이 위장 파일로 걸리면 앞서 이미 올라간 파일들이 S3에 고아 객체로 남는 문제가 있었다
     * (2026-08-06, 사용자가 직접 테스트로 발견 — 원래 설계 원칙을 어긴 것이었다).
     *
     * <p>매직 바이트는 파일 맨 앞 몇 바이트만 봐서, "헤더는 진짜인데 본문이 잘렸거나 깨진" 파일은
     * 못 잡는다. jpg/png/gif는 JDK 표준 {@code ImageIO}로 실제 디코딩까지 한 번 더 확인한다(2026-08-06,
     * 코드 리뷰로 발견). webp는 JDK에 내장 디코더가 없어서(외부 라이브러리 추가가 필요) 매직 바이트
     * 검사만 유지한다 — 알려진 제한사항, `.ai/local/STATE.md` 백로그 참고.
     */
    public void assertActualImageContentOrThrow(MultipartFile file, String extension) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (!matchesSignature(content, extension)) {
            log.warn("파일 내용이 확장자와 일치하지 않음(위장 업로드 의심) - originalFilename={}, extension={}",
                    file.getOriginalFilename(), extension);
            throw new ValidationException(ImageErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (!"webp".equals(extension) && !isDecodableImage(content)) {
            log.warn("이미지 헤더는 맞지만 실제 디코딩 실패(손상/위장 의심) - originalFilename={}, extension={}",
                    file.getOriginalFilename(), extension);
            throw new ValidationException(ImageErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private boolean isDecodableImage(byte[] content) {
        try {
            return ImageIO.read(new ByteArrayInputStream(content)) != null;
        } catch (IOException e) {
            return false;
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
