package com.group3.vitamins.image.application.port;

import java.util.Optional;

/**
 * 복구·완전 삭제 전용 — 블록이 삭제돼 있어도 그 블록이 속했던 stepId를 찾는 아웃바운드 포트.
 * 공유 {@code BlockCatalogPort}는 삭제된 블록을 못 찾아 이 용도로 쓸 수 없다(§ImageEligibilityPolicy 참고).
 *
 * <p>구현체는 이미지 도메인 소유다(동훈님 Block 도메인에 요청하는 게 아님) — 임시 우회임을
 * 명시하려고 별도 포트로 분리했을 뿐, 실제 조회는 여전히 이미지 도메인 안에서 끝난다.
 */
public interface ImageStepLookupPort {

    Optional<Long> findStepIdByImgBlockId(Long imgBlockId);
}
