package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePayload;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeItem;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception.NaraBidNoticeNormalizationException;
import com.group3.vitamins.global.application.support.hash.Sha256HashGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NaraBidNoticePayloadMapper {

    private final ObjectMapper objectMapper;
    private final NaraBidNoticeNormalizer normalizer;
    private final Sha256HashGenerator hashGenerator;

    // 나라장터 원문을 정규화하고 원문 해시를 함께 생성합니다.
    public CollectedBidNoticePayload map(
            JsonNode rawItem,
            BidNoticeType noticeType
    ) {
        try {
            String rawPayload = objectMapper.writeValueAsString(rawItem);
            NaraBidNoticeItem item = objectMapper.treeToValue(
                    rawItem,
                    NaraBidNoticeItem.class
            );
            CollectedBidNotice notice =
                    normalizer.normalize(item, noticeType);

            return new CollectedBidNoticePayload(
                    notice,
                    rawPayload,
                    hashGenerator.generate(rawPayload)
            );
        } catch (JsonProcessingException exception) {
            throw new NaraBidNoticeNormalizationException(
                    "나라장터 공고 원문을 변환할 수 없습니다.",
                    exception
            );
        }
    }
}