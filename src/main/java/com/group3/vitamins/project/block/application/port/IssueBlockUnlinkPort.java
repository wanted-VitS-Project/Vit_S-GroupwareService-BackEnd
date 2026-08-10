package com.group3.vitamins.project.block.application.port;

/**
 * 블록 기준으로 {@code issue_block} 연결을 끊는 아웃바운드 포트 (BLK-014).
 *
 * <p>블록을 다른 스텝으로 옮기면 연결된 이슈가 다른 스텝이 되어 BLK-009 · INV-06 이 깨진다 —
 * 그래서 이동과 함께 연결을 끊는다.
 *
 * <p>⚠️ <b>하드 삭제다.</b> {@code issue_block} 에는 {@code deleted_at} 이 없다 —
 * soft 로 두면 {@code uk_ib} 를 시체가 점유해 재연결이 1062 로 죽는다 (BLOCK.md §8-1).
 */
public interface IssueBlockUnlinkPort {

    /** @return 끊긴 연결 수 */
    int unlinkByBlockId(Long blockId);
}
