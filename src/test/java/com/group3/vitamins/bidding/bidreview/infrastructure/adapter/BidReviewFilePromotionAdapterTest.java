package com.group3.vitamins.bidding.bidreview.infrastructure.adapter;

import com.group3.vitamins.bidding.bidreview.application.port.BidReviewFilePromotionPort;
import com.group3.vitamins.file.application.command.AttachStagedFileCommand;
import com.group3.vitamins.file.application.result.AttachStagedFileResult;
import com.group3.vitamins.file.application.usecase.AttachStagedFileUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("BidReviewFilePromotionAdapter - AttachStagedFileUseCase 위임")
class BidReviewFilePromotionAdapterTest {

    private AttachStagedFileUseCase attachStagedFileUseCase;
    private BidReviewFilePromotionAdapter adapter;

    @BeforeEach
    void setUp() {
        attachStagedFileUseCase = mock(AttachStagedFileUseCase.class);
        adapter = new BidReviewFilePromotionAdapter(attachStagedFileUseCase);
    }

    @Test
    @DisplayName("귀속 결과를 그대로 fileId·fileVersionId로 반환한다")
    void promotesAndMapsResult() {
        when(attachStagedFileUseCase.attach(any()))
                .thenReturn(new AttachStagedFileResult(501L, 9001L, 1, AttachStagedFileResult.INDEX_PENDING));

        BidReviewFilePromotionPort.PromotedFile promoted = adapter.promote(
                new BidReviewFilePromotionPort.PromotionRequest(
                        10L, 700L, "U0001", 31L,
                        "companies/10/bidding/reviews/71/attachments/31/abc", "제안요청서.pdf", 1024L
                )
        );

        assertThat(promoted.fileId()).isEqualTo(501L);
        assertThat(promoted.fileVersionId()).isEqualTo(9001L);
    }

    @Test
    @DisplayName("companyId·projectId·requesterUserId·temporaryStorageKey를 그대로 전달하고, " +
            "bidReviewDocumentId를 멱등키로, 항상 새 문서 생성으로 요청한다")
    void mapsCommandFieldsCorrectly() {
        when(attachStagedFileUseCase.attach(any()))
                .thenReturn(new AttachStagedFileResult(501L, 9001L, 1, AttachStagedFileResult.INDEX_PENDING));

        adapter.promote(new BidReviewFilePromotionPort.PromotionRequest(
                10L, 700L, "U0001", 31L,
                "companies/10/bidding/reviews/71/attachments/31/abc", "제안요청서.pdf", 1024L
        ));

        verify(attachStagedFileUseCase).attach(new AttachStagedFileCommand(
                10L, 700L, "U0001",
                "companies/10/bidding/reviews/71/attachments/31/abc",
                "제안요청서.pdf", 1024L,
                null, null, "AI 검토 첨부", true, "31"
        ));
    }
}
