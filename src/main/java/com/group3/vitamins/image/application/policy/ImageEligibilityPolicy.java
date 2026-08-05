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

    public ImageItem getActiveItemOrThrow(Long imgId) {
        return imageRepository.findActiveByImgId(imgId)
                .orElseThrow(() -> {
                    log.warn("이미지 항목 존재하지 않음 - imgId={}", imgId);
                    return new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
                });
    }

    public void assertEditPermission(Long imgBlockId, String userId) {
        if (!blockCatalogPort.hasEditPermission(BLOCK_TYPE, imgBlockId, userId)) {
            log.warn("편집 권한 없음 - blockType={}, imgBlockId={}, userId={}", BLOCK_TYPE, imgBlockId, userId);
            throw new ForbiddenException(ImageErrorCode.FORBIDDEN);
        }
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
