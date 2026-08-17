package com.group3.vitamins.project.block.application.port;

/**
 * 블록 삭제 시 그 블록에 매달린 파일을 휴지통으로 이동시키는 아웃바운드 포트 (D안 · 2026-08-16 · {@code api/file.md} §블록 생명주기).
 *
 * <p>block 도메인이 선언하고 file 도메인이 구현한다(cross-domain, {@code FileDerivedDataCleanupPort} 선례와 동형).
 * block 도메인은 file 내부 구조(block_file·file_version)를 모른 채 blockId 만 넘긴다.
 *
 * <p>⚠️ 진행 중 결재가 참조하는 파일이 하나라도 있으면 구현체가 {@code FILE_APPROVAL_IN_PROGRESS}(409)를 던져
 * 블록 삭제 트랜잭션을 통째로 롤백한다 — 직접 삭제·스텝/스테이지 cascade 삭제 모두 이 규칙을 따른다
 * ("결재중 파일은 못 지운다" 불변식, 개별 휴지통 이동(§5)의 하드 락과 동일).
 */
public interface BlockFileTrashPort {

    /**
     * blockId 에 매달린 활성 파일을 모두 휴지통으로 이동한다.
     *
     * @param blockId     삭제되는 블록
     * @param actorUserId 삭제 요청자(활동 로그 actor)
     * @return 휴지통으로 이동한 파일 수(이미 휴지통이거나 링크가 없는 파일은 제외)
     */
    int trashByBlockId(Long blockId, String actorUserId);
}
