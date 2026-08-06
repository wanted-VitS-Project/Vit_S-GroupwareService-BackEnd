package com.group3.vitamins.image.application.service;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.image.application.policy.ImageEligibilityPolicy;
import com.group3.vitamins.image.application.port.ImageStoragePort;
import com.group3.vitamins.image.application.query.GetImageDownloadQuery;
import com.group3.vitamins.image.application.query.GetImageItemQuery;
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
 * 이미지 읽기 전용 API 3종 — 전부 편집 권한이 아니라 접근(VIEWER 이상) 권한만 있으면 된다.
 *
 * <p>1) "다음"/"이전" 버튼으로 이미지를 한 장씩 순차 조회. 마지막에서 "다음"을 누르면 첫 이미지로,
 * 처음에서 "이전"을 누르면 마지막 이미지로 순환한다 — 프론트가 이미지 목록을 캐싱하지 않고 매번
 * 이 API를 호출하는 걸 전제로 한다 (`.ai/api/image.md` §다음 이미지 조회 API 참고).
 *
 * <p>2) 이미지 다운로드 — imgId 지정 시 그 이미지 한 장, 없으면 블록에 속한 활성 이미지 전체를
 * zip으로 묶어 응답한다.
 *
 * <p>3) 이미지 휴지통 — 프로젝트에 속한 삭제된 이미지 전체 조회. 앞의 둘과 달리 블록 단위가 아니라
 * 프로젝트 단위 조회라 {@link ImageEligibilityPolicy} 대신 {@link ProjectAccessUseCase}로 권한을 확인한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ImageQueryService implements ImageQueryUseCase {

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

    /** 같은 이름으로 여러 번 업로드된 경우 zip 안에서 항목이 서로 덮어써지는 걸 막는다. */
    private String uniqueEntryName(ImageItem item, Set<String> usedNames) {
        String name = sanitizeEntryName(item.getOriginalName());
        if (usedNames.add(name)) {
            return name;
        }
        String disambiguated = item.getImgId() + "_" + name;
        usedNames.add(disambiguated);
        return disambiguated;
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
     * 이미지 휴지통 — 프로젝트에 속한 삭제된 이미지 전체를 최신순으로 돌려준다. stepId를 하나로
     * 특정할 수 없는 프로젝트 전체 조회라, 스텝 단위가 아니라 {@link ProjectAccessUseCase}로 프로젝트
     * 접근 권한만 확인한다(존재하지 않으면 404, 참여자가 아니면 403 — 둘 다 프로젝트 도메인 공통 코드).
     */
    @Override
    public List<TrashedImageView> getTrash(GetImageTrashQuery query) {
        log.info("이미지 휴지통 조회 요청 - projectId={}, userId={}", query.projectId(), query.userId());

        projectAccessUseCase.requireAccess(query.projectId(), query.userId(), query.role());

        List<TrashedImageView> trashed = imageTrashMapper.findTrashedByProjectId(query.projectId()).stream()
                .map(row -> new TrashedImageView(
                        row.imgId(), row.originalName(),
                        imageStoragePort.presignViewUrl(row.imageUrl()),
                        row.caption(), row.deletedAt()))
                .toList();

        log.info("이미지 휴지통 조회 완료 - projectId={}, count={}", query.projectId(), trashed.size());

        return trashed;
    }

    /**
     * 프로젝트 이미지 모아보기 — 프로젝트에 속한 활성 이미지 전체를 생성일 최신순으로 돌려준다.
     * 휴지통 조회와 동일한 이유로 스텝 단위가 아니라 {@link ProjectAccessUseCase}로 프로젝트 접근
     * 권한만 확인한다. 정렬 기준(최신순)은 명세에 없어 트래시 조회와 동일하게 구현 시 임의 결정.
     */
    @Override
    public List<ProjectImageView> getProjectImages(GetProjectImagesQuery query) {
        log.info("프로젝트 이미지 모아보기 조회 요청 - projectId={}, userId={}", query.projectId(), query.userId());

        projectAccessUseCase.requireAccess(query.projectId(), query.userId(), query.role());

        List<ProjectImageView> images = imageGalleryMapper.findActiveByProjectId(query.projectId()).stream()
                .map(row -> new ProjectImageView(
                        row.imgBlockId(), row.imgId(), row.originalName(),
                        imageStoragePort.presignViewUrl(row.imageUrl()),
                        row.caption(), row.createdAt()))
                .toList();

        log.info("프로젝트 이미지 모아보기 조회 완료 - projectId={}, count={}", query.projectId(), images.size());

        return images;
    }
}
