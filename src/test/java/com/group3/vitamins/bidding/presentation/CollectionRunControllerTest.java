package com.group3.vitamins.bidding.presentation;

import com.group3.vitamins.bidding.collectionrun.application.command.StartCollectionRunCommand;
import com.group3.vitamins.bidding.collectionrun.application.query.GetCollectionRunQuery;
import com.group3.vitamins.bidding.collectionrun.application.result.CollectionRunResult;
import com.group3.vitamins.bidding.collectionrun.application.usecase.CollectionRunUseCase;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunStatus;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunTriggerType;
import com.group3.vitamins.bidding.collectionrun.presentation.api.response.CollectionRunResponse;
import com.group3.vitamins.bidding.collectionrun.presentation.api.response.StartCollectionRunResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CollectionRunController")
class CollectionRunControllerTest {

    private static final Long RUN_ID = 1L;
    private static final Long CONDITION_ID = 10L;
    private static final String USER_ID = "EMP001";
    private static final LocalDateTime STARTED_AT =
            LocalDateTime.of(2026, 8, 10, 11, 30);
    private static final LocalDateTime FINISHED_AT =
            LocalDateTime.of(2026, 8, 10, 11, 31);

    @Mock
    private CollectionRunUseCase collectionRunUseCase;

    @InjectMocks
    private CollectionRunController controller;

    @Nested
    @DisplayName("수동 수집 실행")
    class StartCollectionRun {

        @Test
        @DisplayName("수집 요청을 접수하고 202 응답을 반환한다")
        void returnsAcceptedResponse() {
            CollectionRunResult result = pendingResult();

            when(collectionRunUseCase.start(
                    new StartCollectionRunCommand(CONDITION_ID, USER_ID)
            )).thenReturn(result);

            ResponseEntity<ApiResponse<StartCollectionRunResponse>> response =
                    controller.start(USER_ID, CONDITION_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().httpStatus()).isEqualTo(202);
            assertThat(response.getBody().message())
                    .isEqualTo("입찰 공고 수집 요청이 접수되었습니다.");
            assertThat(response.getBody().data().runId()).isEqualTo(RUN_ID);
            assertThat(response.getBody().data().runStatus())
                    .isEqualTo(CollectionRunStatus.PENDING);
            assertThat(response.getBody().data().requestedAt())
                    .isEqualTo(STARTED_AT);

            ArgumentCaptor<StartCollectionRunCommand> commandCaptor =
                    ArgumentCaptor.forClass(StartCollectionRunCommand.class);

            verify(collectionRunUseCase).start(commandCaptor.capture());
            assertThat(commandCaptor.getValue().conditionId())
                    .isEqualTo(CONDITION_ID);
            assertThat(commandCaptor.getValue().userId()).isEqualTo(USER_ID);
        }
    }

    @Nested
    @DisplayName("수집 실행 결과 조회")
    class GetCollectionRun {

        @Test
        @DisplayName("수집 상태와 처리 건수를 반환한다")
        void returnsCollectionRunResult() {
            CollectionRunResult result = completedResult();

            when(collectionRunUseCase.get(new GetCollectionRunQuery(RUN_ID)))
                    .thenReturn(result);

            ResponseEntity<ApiResponse<CollectionRunResponse>> response =
                    controller.get(RUN_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().httpStatus()).isEqualTo(200);
            assertThat(response.getBody().message())
                    .isEqualTo("입찰 공고 수집 결과 조회 성공");

            CollectionRunResponse data = response.getBody().data();

            assertThat(data.runId()).isEqualTo(RUN_ID);
            assertThat(data.conditionId()).isEqualTo(CONDITION_ID);
            assertThat(data.triggerType())
                    .isEqualTo(CollectionRunTriggerType.MANUAL);
            assertThat(data.runStatus())
                    .isEqualTo(CollectionRunStatus.COMPLETED);
            assertThat(data.collectedCount()).isEqualTo(40);
            assertThat(data.insertedCount()).isEqualTo(12);
            assertThat(data.updatedCount()).isEqualTo(5);
            assertThat(data.skippedCount()).isEqualTo(23);
            assertThat(data.errorMessage()).isNull();
            assertThat(data.startedAt()).isEqualTo(STARTED_AT);
            assertThat(data.finishedAt()).isEqualTo(FINISHED_AT);

            verify(collectionRunUseCase).get(
                    new GetCollectionRunQuery(RUN_ID)
            );
        }
    }

    // 처리 대기 상태의 수집 실행 결과를 만듭니다.
    private CollectionRunResult pendingResult() {
        return new CollectionRunResult(
                RUN_ID,
                CONDITION_ID,
                CollectionRunTriggerType.MANUAL,
                CollectionRunStatus.PENDING,
                0,
                0,
                0,
                0,
                null,
                STARTED_AT,
                null
        );
    }

    // 처리가 완료된 수집 실행 결과를 만듭니다.
    private CollectionRunResult completedResult() {
        return new CollectionRunResult(
                RUN_ID,
                CONDITION_ID,
                CollectionRunTriggerType.MANUAL,
                CollectionRunStatus.COMPLETED,
                40,
                12,
                5,
                23,
                null,
                STARTED_AT,
                FINISHED_AT
        );
    }
}