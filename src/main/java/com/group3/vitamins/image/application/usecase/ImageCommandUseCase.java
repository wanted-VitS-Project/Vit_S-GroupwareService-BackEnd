package com.group3.vitamins.image.application.usecase;

import com.group3.vitamins.image.application.command.CreateImageItemsCommand;
import com.group3.vitamins.image.application.command.DeleteImageItemCommand;
import com.group3.vitamins.image.application.command.UpdateImageItemsCommand;

import java.time.LocalDateTime;
import java.util.List;

public interface ImageCommandUseCase {

    //이미지 항목 생성
    //입력 모델로 Command를 받고 출력 모델로 내부 주머니(View)를 반환.
    CreateImageItemsView create(CreateImageItemsCommand command);

    //컨트롤러에 전달할 결과
    record CreateImageItemsView(
            Long imgBlockId,
            List<CreatedImageView> images
    ) {
    }

    record CreatedImageView(
            Long imgId,
            String originalName,
            String imageUrl,
            String caption,
            int orderIndex,
            LocalDateTime createdAt
    ) {
    }

    //이미지 항목 수정(순서·캡션)
    UpdateImageItemsView updateItems(UpdateImageItemsCommand command);

    record UpdateImageItemsView(
            List<UpdatedImageOrderView> images
    ) {
    }

    record UpdatedImageOrderView(
            Long imgId,
            int orderIndex,
            String caption
    ) {
    }

    //이미지 항목 삭제
    void delete(DeleteImageItemCommand command);
}
