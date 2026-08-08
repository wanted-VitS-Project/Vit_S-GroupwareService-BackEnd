package com.group3.vitamins.file.application.result;

/** 영구 삭제 결과(§7). 지운 버전 수와 저장소에서 실제 삭제된 객체 수를 담는다. */
public record FilePermanentDeleteResult(
        Long fileId,
        int deletedVersionCount,
        int storageDeletedCount
) {
}
