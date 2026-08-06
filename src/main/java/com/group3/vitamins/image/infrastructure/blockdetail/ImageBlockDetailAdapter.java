package com.group3.vitamins.image.infrastructure.blockdetail;

import com.group3.vitamins.image.application.port.ImageStoragePort;
import com.group3.vitamins.image.application.service.ImageHandlerService;
import com.group3.vitamins.project.block.application.port.BlockDetailPort;
import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.application.result.ImageDetail;
import com.group3.vitamins.project.block.application.result.ImageDetail.ImagePreview;
import com.group3.vitamins.project.block.domain.model.BlockType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ImageBlockDetailAdapter implements BlockDetailPort {

    private final ImageDetailMapper imageDetailMapper;
    private final ImageHandlerService imageHandlerService;
    private final ImageStoragePort imageStoragePort;

    @Override
    public BlockType supportedType() {
        return BlockType.IMAGE;
    }

    /** 항목이 0개인 블록 행만 만든다. INSERT 는 이미지 도메인이 JPA 로 처리하고 PK 를 돌려준다. */
    @Override
    public Long createDetail(Long blockId) {
        return imageHandlerService.create(blockId);
    }

    /** 이미지 도메인의 삭제 처리를 재사용한다 — 항목 일괄 정리가 그쪽에 있다. */
    @Override
    public void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt) {
        imageHandlerService.deleteByBlock(typeId, userId, blockTitle, deletedAt);
    }

    /**
     * 블록 카드 미리보기용 — order_index 가 가장 작은 이미지 1장과 활성 이미지 총 개수를 담는다.
     * 이미지가 없는 블록도 요청받은 PK 전체를 기준으로 응답에 포함한다(firstImage=null, totalCount=0).
     */
    @Override
    public Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, ImageDetailRow> firstByBlock = imageDetailMapper.findFirstImagesByImgBlockIds(typeIds).stream()
                .collect(Collectors.toMap(ImageDetailRow::imgBlockId, row -> row));

        Map<Long, BlockDetail> details = new HashMap<>();
        for (Long imgBlockId : typeIds) {
            ImageDetailRow row = firstByBlock.get(imgBlockId);
            if (row == null) {
                details.put(imgBlockId, new ImageDetail(imgBlockId, 0, null));
                continue;
            }
            ImagePreview preview = new ImagePreview(
                    row.imgId(), row.originalName(), imageStoragePort.presignViewUrl(row.imageUrl()),
                    row.caption(), row.orderIndex(), row.createdAt());
            details.put(imgBlockId, new ImageDetail(imgBlockId, row.totalCount(), preview));
        }
        return details;
    }
}
