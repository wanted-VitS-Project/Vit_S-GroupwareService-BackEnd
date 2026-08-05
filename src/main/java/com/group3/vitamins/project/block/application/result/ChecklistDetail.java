package com.group3.vitamins.project.block.application.result;

import java.util.List;

/** CHECKLIST 블록 상세. 추가 직후에는 항목이 0개이고 카드에 0/0 이 뜬다. */
public record ChecklistDetail(Long chkBlockId, int totalCount, int completedCount,
                              List<ChecklistItemView> items) implements BlockDetail {
}