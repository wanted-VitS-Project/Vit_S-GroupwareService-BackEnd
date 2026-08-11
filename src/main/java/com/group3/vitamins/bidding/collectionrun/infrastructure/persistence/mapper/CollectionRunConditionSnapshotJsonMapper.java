package com.group3.vitamins.bidding.collectionrun.infrastructure.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CollectionRunConditionSnapshotJsonMapper {

    private final ObjectMapper objectMapper;

    // 실행 당시 수집 조건 스냅샷을 DB JSON 값으로 변환합니다.
    public JsonNode toJson(CollectionRunConditionSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalStateException("수집 실행 조건 스냅샷이 존재하지 않습니다.");
        }
        return objectMapper.valueToTree(snapshot);
    }

    // DB JSON 값을 수집 실행 조건 스냅샷으로 복원합니다.
    public CollectionRunConditionSnapshot fromJson(JsonNode snapshot) {
        if (snapshot == null || snapshot.isNull()) {
            throw new IllegalStateException("수집 실행 조건 스냅샷이 존재하지 않습니다.");
        }

        try {
            return objectMapper.treeToValue(
                    snapshot,
                    CollectionRunConditionSnapshot.class
            );
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "수집 실행 조건 스냅샷을 읽을 수 없습니다.",
                    exception
            );
        }
    }
}
