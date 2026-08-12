package com.group3.vitamins.image.infrastructure.trash;

import java.time.LocalDateTime;

/**
 * image 테이블에서 조회한 삭제된 이미지 행. imageUrl은 실제로는 S3 저장 키(원시값)다.
 *
 * <p>blockDeleted — 상위 image_block(을 담은 block)까지 삭제됐는지(2026-08-11 추가). block이
 * 삭제되면 IMG-009로 복구가 막히는데, 이 값이 없으면 프론트가 미리 알 방법이 없어 사용자가 복구를
 * 시도한 뒤에야(404/409류 실패로) 알게 된다. 삭제 정책 Pattern D — JOIN에서 b.deleted_at을
 * 필터링하지 않고 그대로 노출만 한다.
 */
public record ImageTrashRow(Long imgId, String originalName, String imageUrl,
                             String caption, LocalDateTime deletedAt, boolean blockDeleted) {
}
