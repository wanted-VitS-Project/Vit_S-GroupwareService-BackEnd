package com.group3.vitamins.checklist.application.usecase;

import com.group3.vitamins.checklist.application.command.CreateChecklistItemCommand;
import com.group3.vitamins.checklist.application.command.DeleteChecklistItemCommand;
import com.group3.vitamins.checklist.application.command.UpdateChecklistItemCommand;

import java.time.LocalDateTime;

public interface ChecklistCommandUseCase {

    //체크리스트 항목 생성
    //입력 모델로 Command를 받고 출력 모델로 내부 주머니(View)를 반환.
    CreateChecklistItemView create(CreateChecklistItemCommand command);

    //컨트롤러에 전달할 결과
    record CreateChecklistItemView(
            Long chkBlockId,
            Long chkId,
            String content,
            int completedCount,
            int totalCount,
            LocalDateTime createdAt
    ) {
    }

    //체크리스트 항목 수정
    UpdateChecklistItemView update(UpdateChecklistItemCommand command);

    record UpdateChecklistItemView(
            Long chkId,
            String content,
            boolean isCompleted,
            int completedCount,
            int totalCount,
            LocalDateTime updatedAt
    ) {
    }

    //체크리스트 항목 삭제
    DeleteChecklistItemView delete(DeleteChecklistItemCommand command);

    record DeleteChecklistItemView(
            int completedCount,
            int totalCount
    ) {
    }
}
