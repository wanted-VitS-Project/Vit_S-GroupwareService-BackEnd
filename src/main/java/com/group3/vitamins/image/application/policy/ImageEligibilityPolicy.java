package com.group3.vitamins.image.application.policy;

import com.group3.vitamins.image.domain.exception.ImageErrorCode;
import com.group3.vitamins.image.domain.model.ImageItem;
import com.group3.vitamins.image.domain.repository.ImageBlockRepository;
import com.group3.vitamins.image.domain.repository.ImageRepository;
import com.group3.vitamins.image.infrastructure.trash.ImageTrashMapper;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.step.application.usecase.StepAccessUseCase;
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
    private final ImageTrashMapper imageTrashMapper;
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
     * 무관하게 항상 false만 반환하므로(§생성·수정 API와 동일 포트), 그 경로를 안 타고 이 블록이
     * 속했던 stepId를 직접 찾아(image_block→block, 삭제 여부 무시) 그대로 존재하는
     * {@code StepAccessUseCase.requireEditable}로 판정한다 — 실제 권한 규칙은 여전히 Step 도메인이
     * 소유한다, 여기선 ID만 찾는다.
     *
     * <p>⚠️ Block 도메인(동훈님)에 "삭제된 블록도 포함해서 stepId를 찾는" 정식 포트가 생기면 그걸로
     * 교체할 것 — 지금은 이미지 도메인 단독으로 처리하려고 둔 임시 우회다 (2026-08-06 결정,
     * `.ai/api/image.md` 참고). 실패 시 코드는 IMG-002가 아니라 Step 도메인 코드
     * (`STEP_EDIT_DENIED`/`STEP_NOT_FOUND`)로 나간다 — 원 명세와의 차이.
     */
    public void assertEditPermissionEvenIfBlockDeleted(Long imgBlockId, String userId, String role) {
        if (imageBlockRepository.existsActive(imgBlockId)) {
            assertEditPermission(imgBlockId, userId, role);
            return;
        }

        Long stepId = imageTrashMapper.findStepIdByImgBlockId(imgBlockId)
                .orElseThrow(() -> new IllegalStateException(
                        "image_block이 가리키는 block 행을 찾을 수 없습니다 — 데이터 정합성 문제: " + imgBlockId));

        log.info("블록 삭제된 이미지 - 스텝 편집 권한으로 대체 판정 - imgBlockId={}, stepId={}", imgBlockId, stepId);
        stepAccessUseCase.requireEditable(stepId, userId, role);
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
