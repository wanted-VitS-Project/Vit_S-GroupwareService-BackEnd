package com.group3.vitamins.image.application.usecase;

import com.group3.vitamins.image.application.query.GetImageDownloadQuery;
import com.group3.vitamins.image.application.query.GetImageItemQuery;
import com.group3.vitamins.image.application.query.GetImageTrashQuery;

import java.time.LocalDateTime;
import java.util.List;

public interface ImageQueryUseCase {

    //이미지 항목 조회(다음/이전)
    ImageItemView getItem(GetImageItemQuery query);

    record ImageItemView(
            Long imgId,
            String originalName,
            String imageUrl,
            String caption,
            int orderIndex,
            int totalCount
    ) {
    }

    //이미지 다운로드(단건 또는 블록 전체 zip)
    ImageDownloadView getDownload(GetImageDownloadQuery query);

    record ImageDownloadView(
            String fileName,
            String contentType,
            byte[] content
    ) {
    }

    //이미지 휴지통 조회 — 프로젝트에 속한 삭제된 이미지 전체
    List<TrashedImageView> getTrash(GetImageTrashQuery query);

    record TrashedImageView(
            Long imgId,
            String originalName,
            String imageUrl,
            String caption,
            LocalDateTime deletedAt
    ) {
    }
}
