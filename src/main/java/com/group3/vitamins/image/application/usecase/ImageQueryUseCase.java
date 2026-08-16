package com.group3.vitamins.image.application.usecase;

import com.group3.vitamins.image.application.query.GetImageDownloadQuery;
import com.group3.vitamins.image.application.query.GetImageItemQuery;
import com.group3.vitamins.image.application.query.GetImageItemsQuery;
import com.group3.vitamins.image.application.query.GetImageTrashQuery;
import com.group3.vitamins.image.application.query.GetProjectImagesQuery;

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

    //이미지 항목 전체 조회 — 한 블록의 활성 이미지 전부(수정 화면에서 목록을 통째로 그리는 용도)
    ImageItemsView getItems(GetImageItemsQuery query);

    record ImageItemsView(
            int totalCount,
            List<BlockImageView> images
    ) {
    }

    record BlockImageView(
            Long imgId,
            String originalName,
            String imageUrl,
            String caption,
            int orderIndex,
            int version
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

    //이미지 휴지통 조회 — 프로젝트에 속한 삭제된 이미지(페이지네이션)
    ImageTrashView getTrash(GetImageTrashQuery query);

    record ImageTrashView(
            List<TrashedImageView> images,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    record TrashedImageView(
            Long imgId,
            String originalName,
            String imageUrl,
            String caption,
            LocalDateTime deletedAt,
            boolean blockDeleted
    ) {
    }

    //프로젝트 이미지 모아보기 — 프로젝트에 속한 활성 이미지(여러 스텝·블록에 걸침, 페이지네이션)
    ProjectImagesView getProjectImages(GetProjectImagesQuery query);

    record ProjectImagesView(
            List<ProjectImageView> images,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    record ProjectImageView(
            Long imgBlockId,
            String blockTitle,
            Long stepId,
            String stepName,
            Long imgId,
            String originalName,
            String imageUrl,
            String caption,
            LocalDateTime createdAt
    ) {
    }
}
