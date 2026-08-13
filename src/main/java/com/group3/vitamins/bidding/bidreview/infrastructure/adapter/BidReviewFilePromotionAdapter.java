package com.group3.vitamins.bidding.bidreview.infrastructure.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewFilePromotionPort;
import com.group3.vitamins.file.application.command.AttachStagedFileCommand;
import com.group3.vitamins.file.application.result.AttachStagedFileResult;
import com.group3.vitamins.file.application.usecase.AttachStagedFileUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BidReviewFilePromotionAdapter implements BidReviewFilePromotionPort {

    // AttachStagedFileCommand.comment 계약상 관례 값(FILE-V1 §2-G-1).
    private static final String PROMOTION_COMMENT = "AI 검토 첨부";

    private final AttachStagedFileUseCase attachStagedFileUseCase;

    @Override
    public PromotedFile promote(PromotionRequest request) {
        AttachStagedFileResult result = attachStagedFileUseCase.attach(new AttachStagedFileCommand(
                request.companyId(),
                request.projectId(),
                request.requesterUserId(),
                request.temporaryStorageKey(),
                request.fileName(),
                request.fileSizeBytes(),
                null, // checksum - 다운로드 단계에서 계산하지 않음
                null, // name - 생략하면 파일 도메인이 확장자를 뗀 원본 파일명으로 채운다
                PROMOTION_COMMENT,
                true, // allowDuplicateName - 귀속은 항상 새 문서(PROMOTE-009)
                String.valueOf(request.bidReviewDocumentId()) // idempotencyKey - 재시도 중복 귀속 방지(PROMOTE-007)
        ));

        return new PromotedFile(result.fileId(), result.fileVersionId());
    }
}
