package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaraBidNoticeApiResponse(
        Response response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
            Header header,
            Body body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String resultCode,
            String resultMsg
    ) {

        // 나라장터 OpenAPI의 정상 응답 코드를 확인합니다.
        public boolean isSuccess() {
            return "00".equals(resultCode);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            List<JsonNode> items,
            Integer numOfRows,
            Integer pageNo,
            Integer totalCount
    ) {

        // 공고가 없는 응답도 안전하게 순회할 수 있도록 빈 목록을 반환합니다.
        public List<JsonNode> safeItems() {
            return items == null ? List.of() : List.copyOf(items);
        }
    }
}