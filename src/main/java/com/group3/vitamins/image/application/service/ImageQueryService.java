package com.group3.vitamins.image.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.image.application.policy.ImageEligibilityPolicy;
import com.group3.vitamins.image.application.port.ImageStoragePort;
import com.group3.vitamins.image.application.query.GetImageDownloadQuery;
import com.group3.vitamins.image.application.query.GetImageItemQuery;
import com.group3.vitamins.image.application.query.GetImageItemsQuery;
import com.group3.vitamins.image.application.query.GetImageTrashQuery;
import com.group3.vitamins.image.application.query.GetProjectImagesQuery;
import com.group3.vitamins.image.application.usecase.ImageQueryUseCase;
import com.group3.vitamins.image.domain.exception.ImageErrorCode;
import com.group3.vitamins.image.domain.model.ImageItem;
import com.group3.vitamins.image.domain.repository.ImageRepository;
import com.group3.vitamins.image.infrastructure.gallery.ImageGalleryMapper;
import com.group3.vitamins.image.infrastructure.trash.ImageTrashMapper;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 이미지 읽기 전용 API — 권한 기준이 종류마다 다르니 주의할 것.
 *
 * <p>1) "다음"/"이전" 버튼으로 이미지를 한 장씩 순차 조회. 마지막에서 "다음"을 누르면 첫 이미지로,
 * 처음에서 "이전"을 누르면 마지막 이미지로 순환한다 — 프론트가 이미지 목록을 캐싱하지 않고 매번
 * 이 API를 호출하는 걸 전제로 한다 (`.ai/api/image.md` §다음 이미지 조회 API 참고). 접근(VIEWER 이상) 권한.
 *
 * <p>2) 이미지 항목 전체 조회 — 한 블록의 활성 이미지 전부를 한 번에 돌려준다. 수정 화면이 목록을
 * 통째로 그리기 위한 조회라, 다른 조회들과 달리 **편집 권한**을 요구한다(2026-08-07 담당자 결정).
 *
 * <p>3) 이미지 다운로드 — imgId 지정 시 그 이미지 한 장, 없으면 블록에 속한 활성 이미지 전체를
 * zip으로 묶어 응답한다. 접근 권한.
 *
 * <p>4) 이미지 휴지통 · 프로젝트 이미지 모아보기 — 블록 단위가 아니라 프로젝트 단위 조회라
 * {@link ImageEligibilityPolicy} 대신 {@link ProjectAccessUseCase}로 권한을 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ImageQueryService implements ImageQueryUseCase {

    private static final int MAX_PAGE_SIZE = 100;

    private final ImageEligibilityPolicy eligibilityPolicy;
    private final ImageRepository imageRepository;
    private final ImageStoragePort imageStoragePort;
    private final ImageTrashMapper imageTrashMapper;
    private final ImageGalleryMapper imageGalleryMapper;
    private final ProjectAccessUseCase projectAccessUseCase;

    @Override
    public ImageItemView getItem(GetImageItemQuery query) {
        log.info("이미지 항목 조회 요청 - imgBlockId={}, currentOrderIndex={}, direction={}, userId={}",
                query.imgBlockId(), query.currentOrderIndex(), query.direction(), query.userId());

        eligibilityPolicy.assertBlockActiveOrThrowReadOnly(query.imgBlockId());
        eligibilityPolicy.assertViewPermission(query.imgBlockId(), query.userId(), query.role());

        // currentOrderIndex가 그 블록의 실제 활성 이미지를 가리키는지부터 확인한다 — 이게 없으면
        // "다음이 없다"는 신호와 "애초에 없는 값을 보냈다"는 신호를 구분 못 해서, 존재하지 않는
        // orderIndex를 보내도 순환 로직을 타고 첫/마지막 이미지가 조용히 나가버린다.
        if (!imageRepository.existsActiveByOrderIndex(query.imgBlockId(), query.currentOrderIndex())) {
            throw noActiveItem(query.imgBlockId());
        }

        ImageItem item = switch (query.direction()) {
            case NEXT -> findNextOrWrapToFirst(query.imgBlockId(), query.currentOrderIndex());
            case PREV -> findPreviousOrWrapToLast(query.imgBlockId(), query.currentOrderIndex());
        };

        int totalCount = imageRepository.countActive(query.imgBlockId());

        log.info("이미지 항목 조회 완료 - imgBlockId={}, imgId={}", query.imgBlockId(), item.getImgId());

        return new ImageItemView(
                item.getImgId(),
                item.getOriginalName(),
                imageStoragePort.presignViewUrl(item.getImageUrl()),
                item.getCaption(),
                item.getOrderIndex(),
                totalCount
        );
    }

    private ImageItem findNextOrWrapToFirst(Long imgBlockId, int currentOrderIndex) {
        return imageRepository.findNextByOrderIndex(imgBlockId, currentOrderIndex)
                .or(() -> imageRepository.findFirstActive(imgBlockId))
                .orElseThrow(() -> noActiveItem(imgBlockId));
    }

    private ImageItem findPreviousOrWrapToLast(Long imgBlockId, int currentOrderIndex) {
        return imageRepository.findPreviousByOrderIndex(imgBlockId, currentOrderIndex)
                .or(() -> imageRepository.findLastActive(imgBlockId))
                .orElseThrow(() -> noActiveItem(imgBlockId));
    }

    /** 블록은 존재하지만 활성 이미지가 하나도 없을 때(전부 삭제됨 등). */
    private NotFoundException noActiveItem(Long imgBlockId) {
        log.warn("조회할 이미지 항목 없음 - imgBlockId={}", imgBlockId);
        return new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
    }

    /**
     * 이미지 항목 전체 조회 — 블록의 활성 이미지 전부를 orderIndex 오름차순으로 돌려준다.
     *
     * <p>수정 화면이 "현재 목록 전체"를 받아 그린 뒤 그대로 수정 API(PATCH)로 되돌려 보내는 구조라
     * 조회지만 **편집 권한**을 요구한다(2026-08-07 담당자 결정) — 순차 조회(getItem)·다운로드가
     * 접근 권한인 것과 다르다.
     *
     * <p>이미지가 0장이어도 404가 아니라 빈 배열 + totalCount=0으로 응답한다(휴지통·모아보기 조회와
     * 동일한 목록 조회 원칙). 블록 자체가 없을 때만 404다.
     */
    @Override
    public ImageItemsView getItems(GetImageItemsQuery query) {
        log.info("이미지 항목 전체 조회 요청 - imgBlockId={}, userId={}", query.imgBlockId(), query.userId());

        eligibilityPolicy.assertBlockActiveOrThrowReadOnly(query.imgBlockId());
        eligibilityPolicy.assertEditPermission(query.imgBlockId(), query.userId(), query.role());

        List<BlockImageView> images = imageRepository.findAllActiveByImgBlockId(query.imgBlockId()).stream()
                .map(item -> new BlockImageView(
                        item.getImgId(),
                        item.getOriginalName(),
                        // DB에 저장된 건 S3 키다 — presigned URL은 1시간 만료라 응답 시점에 매번 새로 서명한다.
                        imageStoragePort.presignViewUrl(item.getImageUrl()),
                        item.getCaption(),
                        item.getOrderIndex(),
                        item.getVersion()))
                .toList();

        log.info("이미지 항목 전체 조회 완료 - imgBlockId={}, count={}", query.imgBlockId(), images.size());

        return new ImageItemsView(images.size(), images);
    }

    /** 이미지 다운로드 — imgId가 있으면 그 이미지 한 장(원본 파일명), 없으면 블록 전체를 zip으로(블록명). */
    @Override
    public ImageDownloadView getDownload(GetImageDownloadQuery query) {
        log.info("이미지 다운로드 요청 - imgBlockId={}, imgId={}, userId={}",
                query.imgBlockId(), query.imgId(), query.userId());

        eligibilityPolicy.assertBlockActiveOrThrowReadOnly(query.imgBlockId());
        eligibilityPolicy.assertViewPermission(query.imgBlockId(), query.userId(), query.role());

        ImageDownloadView view = query.imgId() != null
                ? downloadSingle(query.imgBlockId(), query.imgId())
                : downloadAll(query.imgBlockId());

        log.info("이미지 다운로드 완료 - imgBlockId={}, imgId={}, fileName={}",
                query.imgBlockId(), query.imgId(), view.fileName());

        return view;
    }

    private ImageDownloadView downloadSingle(Long imgBlockId, Long imgId) {
        ImageItem item = imageRepository.findActiveByImgId(imgId)
                .filter(candidate -> candidate.getImgBlockId().equals(imgBlockId))
                .orElseThrow(() -> {
                    log.warn("다운로드할 이미지 항목 없음 - imgBlockId={}, imgId={}", imgBlockId, imgId);
                    return new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
                });

        byte[] content = imageStoragePort.download(item.getImageUrl());
        String contentType = imageStoragePort.contentTypeOf(item.getExtension());

        return new ImageDownloadView(item.getOriginalName(), contentType, content);
    }

    private ImageDownloadView downloadAll(Long imgBlockId) {
        List<ImageItem> items = imageRepository.findAllActiveByImgBlockId(imgBlockId);
        if (items.isEmpty()) {
            log.warn("다운로드할 이미지가 없음 - imgBlockId={}", imgBlockId);
            throw new NotFoundException(ImageErrorCode.NO_IMAGES_TO_DOWNLOAD);
        }

        String blockTitle = eligibilityPolicy.getBlockTitle(imgBlockId);
        String zipFileName = (blockTitle == null || blockTitle.isBlank() ? "images" : blockTitle) + ".zip";

        return new ImageDownloadView(zipFileName, "application/zip", zipAll(items));
    }

    private byte[] zipAll(List<ImageItem> items) {
        Set<String> usedNames = new HashSet<>();
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            for (ImageItem item : items) {
                zip.putNextEntry(new ZipEntry(uniqueEntryName(item, usedNames)));
                zip.write(imageStoragePort.download(item.getImageUrl()));
                zip.closeEntry();
            }
            zip.finish();
            return buffer.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 같은 이름으로 여러 번 업로드된 경우 zip 안에서 항목이 서로 덮어써지는 걸 막는다. imgId 같은 내부
     * ID를 그대로 노출하면 사용자가 봤을 때 의미를 알 수 없어서, OS 탐색기가 중복 파일에 붙이는 것과
     * 같은 방식(원본파일명-1.jpg, 원본파일명-2.jpg, ...)으로 구분한다. 우연히 그 이름도 이미 쓰여 있으면
     * (예: 실제로 "photo-1.jpg"라는 파일명이 따로 존재) 번호를 계속 올려가며 재시도한다 — 그러지 않으면
     * {@code ZipOutputStream}이 중복 엔트리에서 예외를 던져 전체 다운로드가 500으로 실패한다
     * (2026-08-06, 코드 리뷰로 발견).
     */
    private String uniqueEntryName(ImageItem item, Set<String> usedNames) {
        String name = sanitizeEntryName(item.getOriginalName());
        if (usedNames.add(name)) {
            return name;
        }
        String base = stripExtension(name);
        String extension = extensionOf(name);
        int counter = 1;
        String candidate;
        do {
            candidate = base + "-" + counter + extension;
            counter++;
        } while (!usedNames.add(candidate));
        return candidate;
    }

    private String stripExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }

    private String extensionOf(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(dotIndex) : "";
    }

    /**
     * zip 엔트리명은 압축 해제 시 실제 파일 경로로 쓰인다 — 업로드 파일명(사용자 입력, DB에 원본 그대로
     * 저장됨)에 경로 구분자나 `..`가 들어있으면 취약한 압축 해제 도구에서 대상 경로 밖에 파일을
     * 쓸 수 있다(zip slip / Path Traversal). 경로 조작에 쓰일 수 있는 문자를 전부 제거한다.
     */
    private String sanitizeEntryName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "image";
        }
        String sanitized = originalName
                .replaceAll("[\\\\/]", "_")
                .replace("..", "_")
                .replaceAll("[\\x00-\\x1f]", "");
        return sanitized.isBlank() ? "image" : sanitized;
    }

    /**
     * 이미지 휴지통 — 프로젝트에 속한 삭제된 이미지를 최신순으로 페이지네이션 조회한다. stepId를 하나로
     * 특정할 수 없는 프로젝트 전체 조회라, 스텝 단위가 아니라 {@link ProjectAccessUseCase}로 프로젝트
     * 접근 권한만 확인한다(존재하지 않으면 404, 참여자가 아니면 403 — 둘 다 프로젝트 도메인 공통 코드).
     */
    @Override
    public ImageTrashView getTrash(GetImageTrashQuery query) {
        log.info("이미지 휴지통 조회 요청 - projectId={}, userId={}", query.projectId(), query.userId());

        projectAccessUseCase.requireAccess(query.projectId(), query.userId(), query.role());
        validatePageQuery(query.page(), query.size());

        List<TrashedImageView> trashed = imageTrashMapper
                .findTrashedByProjectId(query.projectId(), query.size(), query.page() * query.size()).stream()
                .map(row -> new TrashedImageView(
                        row.imgId(), row.originalName(),
                        imageStoragePort.presignViewUrl(row.imageUrl()),
                        row.caption(), row.deletedAt(), row.blockDeleted()))
                .toList();
        long totalElements = imageTrashMapper.countTrashedByProjectId(query.projectId());
        int totalPages = (int) Math.ceil((double) totalElements / query.size());

        log.info("이미지 휴지통 조회 완료 - projectId={}, count={}", query.projectId(), trashed.size());

        return new ImageTrashView(trashed, query.page(), query.size(), totalElements, totalPages);
    }

    /**
     * 프로젝트 이미지 모아보기 — 프로젝트에 속한 활성 이미지를 생성일 최신순으로 페이지네이션 조회한다.
     * 휴지통 조회와 동일한 이유로 스텝 단위가 아니라 {@link ProjectAccessUseCase}로 프로젝트 접근
     * 권한만 확인한다. 정렬 기준(최신순)은 명세에 없어 트래시 조회와 동일하게 구현 시 임의 결정.
     */
    @Override
    public ProjectImagesView getProjectImages(GetProjectImagesQuery query) {
        log.info("프로젝트 이미지 모아보기 조회 요청 - projectId={}, userId={}", query.projectId(), query.userId());

        projectAccessUseCase.requireAccess(query.projectId(), query.userId(), query.role());
        validatePageQuery(query.page(), query.size());

        List<ProjectImageView> images = imageGalleryMapper
                .findActiveByProjectId(query.projectId(), query.size(), query.page() * query.size()).stream()
                .map(row -> new ProjectImageView(
                        row.imgBlockId(), row.blockTitle(), row.stepId(), row.stepName(),
                        row.imgId(), row.originalName(),
                        imageStoragePort.presignViewUrl(row.imageUrl()),
                        row.caption(), row.createdAt()))
                .toList();
        long totalElements = imageGalleryMapper.countActiveByProjectId(query.projectId());
        int totalPages = (int) Math.ceil((double) totalElements / query.size());

        log.info("프로젝트 이미지 모아보기 조회 완료 - projectId={}, count={}", query.projectId(), images.size());

        return new ProjectImagesView(images, query.page(), query.size(), totalElements, totalPages);
    }

    // 재무 입출금/세금계산서 조회와 동일 컨벤션 — 잘못된 page/size는 클램프 대신 400으로 던진다.
    private void validatePageQuery(int page, int size) {
        if (page < 0 || size <= 0 || size > MAX_PAGE_SIZE || page > Integer.MAX_VALUE / size) {
            throw new ValidationException(ImageErrorCode.PAGE_QUERY_INVALID);
        }
    }
}
