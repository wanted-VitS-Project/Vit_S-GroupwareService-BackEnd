package com.group3.vitamins.image.application.policy;

import com.group3.vitamins.image.domain.exception.ImageErrorCode;
import com.group3.vitamins.image.domain.model.ImageItem;
import com.group3.vitamins.image.domain.repository.ImageBlockRepository;
import com.group3.vitamins.image.domain.repository.ImageRepository;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.text.application.port.BlockCatalogPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
