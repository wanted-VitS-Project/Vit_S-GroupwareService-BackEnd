package com.group3.vitamins.bidding.collectioncondition.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CollectionConditionParamsJsonMapper {

    private final ObjectMapper objectMapper;

    // 공고 종류와 필터를 DB params 컬럼에 저장할 JSON으로 변환합니다.
    public JsonNode toJson(
            List<BidNoticeType> noticeTypes,
            CollectionConditionFilter filters
    ) {
        ParamsPayload payload = new ParamsPayload(
                List.copyOf(noticeTypes),
                filters
        );

        return objectMapper.valueToTree(payload);
    }

    // DB params JSON을 도메인에서 사용할 공고 종류와 필터로 복원합니다.
    public ParsedParams fromJson(JsonNode params) {
        if (params == null || params.isNull()) {
            throw new IllegalStateException(
                    "수집 조건 params가 존재하지 않습니다."
            );
        }

        try {
            ParamsPayload payload = objectMapper.treeToValue(
                    params,
                    ParamsPayload.class
            );

            if (payload.noticeTypes() == null
                    || payload.filters() == null) {
                throw new IllegalStateException(
                        "수집 조건 params 구조가 올바르지 않습니다."
                );
            }

            return new ParsedParams(
                    List.copyOf(payload.noticeTypes()),
                    payload.filters()
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "수집 조건 params를 읽을 수 없습니다.",
                    exception
            );
        }
    }

    // DB에 저장되는 params JSON 구조입니다.
    private record ParamsPayload(
            List<BidNoticeType> noticeTypes,
            CollectionConditionFilter filters
    ) {
    }

    // JSON에서 복원한 도메인 검색 조건입니다.
    public record ParsedParams(
            List<BidNoticeType> noticeTypes,
            CollectionConditionFilter filters
    ) {
    }
}