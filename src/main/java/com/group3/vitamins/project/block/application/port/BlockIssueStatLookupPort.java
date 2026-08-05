package com.group3.vitamins.project.block.application.port;

import java.util.Collection;
import java.util.Map;

/** issue_block 으로 연결된 이슈를 블록 단위로 집계하는 아웃바운드 포트. */
public interface BlockIssueStatLookupPort {

    /** blockId → 집계. 연결이 없는 블록은 키가 없다. */
    Map<Long, BlockIssueStat> countByBlockIds(Collection<Long> blockIds);

    record BlockIssueStat(int totalCount, int doneCount) {

        private static final BlockIssueStat EMPTY = new BlockIssueStat(0, 0);

        public static BlockIssueStat empty() {
            return EMPTY;
        }
    }
}