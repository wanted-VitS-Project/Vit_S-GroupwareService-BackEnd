package com.group3.vitamins.checklist.infrastructure.blockdetail;

import com.group3.vitamins.checklist.application.service.ChecklistHandlerService;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.application.result.ChecklistDetail;
import com.group3.vitamins.project.block.application.result.ChecklistItemView;
import com.group3.vitamins.project.block.domain.model.BlockType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChecklistBlockDetailAdapter implements BlockDetailPort {

    private final ChecklistDetailMapper checklistDetailMapper;
    private final ChecklistHandlerService checklistHandlerService;

    @Override
    public BlockType supportedType() {
        return BlockType.CHECKLIST;
    }

    /** 항목이 0개인 블록 행만 만든다. INSERT 는 체크리스트 도메인이 JPA 로 처리하고 PK 를 돌려준다. */
    @Override
    public Long createDetail(Long blockId, String userId) {
        return checklistHandlerService.create(blockId);
    }

    /** 체크리스트 도메인의 삭제 처리를 재사용한다 — 항목 일괄 정리가 그쪽에 있다. */
    @Override
    public void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt) {
        checklistHandlerService.deleteByBlock(typeId, userId, blockTitle, deletedAt);
    }

    /** 항목이 없는 블록도 0/0 으로 응답해야 하므로 요청받은 PK 전체를 기준으로 만든다. */
    @Override
    public Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<ChecklistItemView>> itemsByBlock =
                checklistDetailMapper.findItemsByChkBlockIds(typeIds).stream()
                        .collect(Collectors.groupingBy(ChecklistItemRow::chkBlockId,
                                LinkedHashMap::new,
                                Collectors.mapping(row -> new ChecklistItemView(
                                        row.chkId(), row.content(), row.isCompleted()),
                                        Collectors.toList())));

        Map<Long, BlockDetail> details = new HashMap<>();
        for (Long chkBlockId : typeIds) {
            List<ChecklistItemView> items = itemsByBlock.getOrDefault(chkBlockId, List.of());
            int completedCount = (int) items.stream()
                    .filter(ChecklistItemView::isCompleted)
                    .count();
            details.put(chkBlockId, new ChecklistDetail(
                    chkBlockId, items.size(), completedCount, items));
        }
        return details;
    }
}
