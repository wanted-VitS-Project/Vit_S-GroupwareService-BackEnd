package com.group3.vitamins.text.application.usecase;

import com.group3.vitamins.text.application.command.UpdateTextContentCommand;

import java.time.LocalDateTime;

public interface TextCommandUseCase {

    //텍스트 본문 수정
    //입력 모델로 Command를 받고 출력 모델로 내부 주머니(View)를 반환.
    UpdateTextContentView updateContent(UpdateTextContentCommand command);

    //컨트롤러에 전달할 결과
    record UpdateTextContentView(
            Long txtId,
            String content,
            LocalDateTime updatedAt
    ) {
    }
}
