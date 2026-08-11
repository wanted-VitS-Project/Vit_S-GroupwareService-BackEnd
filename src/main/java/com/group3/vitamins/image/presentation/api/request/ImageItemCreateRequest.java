package com.group3.vitamins.image.presentation.api.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ImageItemCreateRequest(
        @Schema(description = "각 이미지에 대응하는 캡션 (files와 같은 순서, 없으면 빈 문자열)",
                example = "[\"회의실 전경\", \"\", \"화이트보드\"]")
        List<String> captions
) {
}
