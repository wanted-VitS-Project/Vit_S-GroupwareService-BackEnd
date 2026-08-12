package com.group3.vitamins.image.domain.model;

import java.time.LocalDateTime;

/**
 * 이미지 항목 도메인 모델 — 영속성 프레임워크에 의존하지 않는다.
 *
 * <p>이미지 블록({@code image_block}) 생성·삭제는 Block 도메인이 처리한다. {@code imgBlockId} 는
 * 그 블록 상세 행을 참조하는 값만 저장할 뿐 FK 는 아니며, 이 도메인은 그 값을 읽기 전용으로 쓴다.
 *
 * <p>{@code imageUrl} 은 DB 컬럼명과 달리 실제로는 <b>S3 저장 키</b>를 담는다 — 버킷이 퍼블릭
 * 액세스를 전부 차단하고 있어 영구 URL을 저장해도 못 쓴다. 실제 조회 URL은 API 응답을 만드는
 * 시점에 {@link com.group3.vitamins.image.application.port.ImageStoragePort#presignViewUrl} 로
 * 그때그때 서명해서 만든다.
 */
public class ImageItem {

    // 신규 항목의 시작 버전 (CONCURRENCY.md §3-1 — 기존 행·신규 행 모두 1로 시작해야 프론트가 받은
    // 값과 맞물린다). 도메인은 이후 이 값을 절대 올리지 않는다 — +1은 저장 시 WHERE와 같은 문장 안에서
    // DB가 한다.
    private static final int INITIAL_VERSION = 1;

    private final Long imgId;
    private final Long imgBlockId;
    private final String originalName;
    private final String imageUrl;
    private final String extension;
    private final long size;
    private final String caption;
    private final int orderIndex;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final LocalDateTime deletedAt;
    private final int version;

    private ImageItem(Long imgId, Long imgBlockId, String originalName, String imageUrl, String extension,
                       long size, String caption, int orderIndex,
                       LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt, int version) {
        this.imgId = imgId;
        this.imgBlockId = imgBlockId;
        this.originalName = originalName;
        this.imageUrl = imageUrl;
        this.extension = extension;
        this.size = size;
        this.caption = caption;
        this.orderIndex = orderIndex;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
    }

    /** 업로드 완료 후 저장 직전의 새 항목(아직 imgId·createdAt 없음)을 만든다. */
    public static ImageItem newItem(Long imgBlockId, String originalName, String imageUrl, String extension,
                                     long size, String caption, int orderIndex) {
        return new ImageItem(null, imgBlockId, originalName, imageUrl, extension, size,
                caption, orderIndex, null, null, null, INITIAL_VERSION);
    }

    public static ImageItem reconstruct(Long imgId, Long imgBlockId, String originalName, String imageUrl,
                                         String extension, long size, String caption, int orderIndex,
                                         LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt,
                                         int version) {
        return new ImageItem(imgId, imgBlockId, originalName, imageUrl, extension, size,
                caption, orderIndex, createdAt, updatedAt, deletedAt, version);
    }

    public Long getImgId() {
        return imgId;
    }

    public Long getImgBlockId() {
        return imgBlockId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getExtension() {
        return extension;
    }

    public long getSize() {
        return size;
    }

    public String getCaption() {
        return caption;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public int getVersion() {
        return version;
    }
}
