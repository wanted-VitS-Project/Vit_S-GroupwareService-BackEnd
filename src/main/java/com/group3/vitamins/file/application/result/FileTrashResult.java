package com.group3.vitamins.file.application.result;

import java.time.LocalDateTime;

/** 휴지통 이동 결과(§5). */
public record FileTrashResult(
        Long fileId,
        LocalDateTime deletedAt
) {
}
