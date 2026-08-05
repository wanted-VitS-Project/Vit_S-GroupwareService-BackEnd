package com.group3.vitamins.project.block.infrastructure.adapter;

import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.application.result.TextDetail;
import com.group3.vitamins.project.block.domain.model.BlockType;
import com.group3.vitamins.text.application.service.TextHandlerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TextBlockDetailAdapter implements BlockDetailPort {

    private final TextDetailMapper textDetailMapper;
    private final TextHandlerService textHandlerService;

    @Override
    public BlockType supportedType() {
        return BlockType.TEXT;
    }

    /** 본문이 빈 행을 만든다. 내용은 PATCH /blocks/texts/{txtId} 가 채운다. */
    @Override
    public Long createDetail(Long blockId) {
        textDetailMapper.insertEmpty(blockId);
        return textDetailMapper.findTxtIdByBlockId(blockId);
    }

    /** 텍스트 도메인의 삭제 처리를 재사용한다 — 멱등 판정이 그쪽에 있다. */
    @Override
    public void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt) {
        textHandlerService.delete(typeId, userId, blockTitle, deletedAt);
    }

    @Override
    public Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        return textDetailMapper.findByTxtIds(typeIds).stream()
                .collect(Collectors.toMap(TextDetailRow::txtId,
                        row -> new TextDetail(row.txtId(), row.content())));
    }
}