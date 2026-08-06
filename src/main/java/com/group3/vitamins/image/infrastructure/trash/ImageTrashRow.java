package com.group3.vitamins.image.infrastructure.trash;

import java.time.LocalDateTime;

/** image 테이블에서 조회한 삭제된 이미지 행. imageUrl은 실제로는 S3 저장 키(원시값)다. */
public record ImageTrashRow(Long imgId, String originalName, String imageUrl,
                             String caption, LocalDateTime deletedAt) {
}
