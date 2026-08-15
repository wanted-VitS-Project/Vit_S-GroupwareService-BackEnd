package com.group3.vitamins.bidding.bidsummary.presentation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidsummary.application.command.AbandonBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.command.ConfirmBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.command.UpdateBidNoticeSummaryCommand;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryQuery;
import com.group3.vitamins.bidding.bidsummary.application.query.GetBidNoticeSummaryHistoryQuery;
import com.group3.vitamins.bidding.bidsummary.application.result.AbandonBidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryItemResult;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryHistoryResult;
import com.group3.vitamins.bidding.bidsummary.application.result.BidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.result.ConfirmBidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.result.CreateBidNoticeSummaryResult;
import com.group3.vitamins.bidding.bidsummary.application.usecase.AbandonBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.ConfirmBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.CreateBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.GetBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.GetBidNoticeSummaryHistoryUseCase;
import com.group3.vitamins.bidding.bidsummary.application.usecase.UpdateBidNoticeSummaryUseCase;
import com.group3.vitamins.bidding.bidsummary.presentation.api.request.CreateBidNoticeSummaryRequest;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("BidNoticeSummaryController 공개 API")
class BidNoticeSummaryControllerTest {

    private static final Long SUMMARY_ID = 31L;
    private static final String USER_ID = "vitas-USER001";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 9, 0);

    private GetBidNoticeSummaryUseCase getUseCase;
    private CreateBidNoticeSummaryUseCase createUseCase;
    private GetBidNoticeSummaryHistoryUseCase historyUseCase;
    private UpdateBidNoticeSummaryUseCase updateUseCase;
    private ConfirmBidNoticeSummaryUseCase confirmUseCase;
    private AbandonBidNoticeSummaryUseCase abandonUseCase;
    private BidNoticeSummaryController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        getUseCase = mock(GetBidNoticeSummaryUseCase.class);
        createUseCase = mock(CreateBidNoticeSummaryUseCase.class);
        historyUseCase = mock(GetBidNoticeSummaryHistoryUseCase.class);
        updateUseCase = mock(UpdateBidNoticeSummaryUseCase.class);
        confirmUseCase = mock(ConfirmBidNoticeSummaryUseCase.class);
        abandonUseCase = mock(AbandonBidNoticeSummaryUseCase.class);
        controller = new BidNoticeSummaryController(
                createUseCase,
                historyUseCase,
                getUseCase, updateUseCase, confirmUseCase, abandonUseCase
        );
        authentication = new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    @DisplayName("AI 요약 요청은 202와 생성된 요약 ID를 반환한다")
    void createsSummary() {
        when(createUseCase.create(any())).thenReturn(
                new CreateBidNoticeSummaryResult(41L, "PENDING", NOW)
        );

        var response = controller.create(
                317L,
                new CreateBidNoticeSummaryRequest("위험 요소를 정리해줘", null),
                authentication
        );

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().summaryId()).isEqualTo(41L);
        verify(createUseCase).create(any());
    }

    @Test
    @DisplayName("공고별 요약 이력은 최신 내 요약 ID와 개정 계보를 반환한다")
    void getsSummaryHistory() {
        var item = new BidNoticeSummaryHistoryItemResult(
                32L, 31L, 2, "COMPLETED", "위험을 보강해줘",
                false, true, null, NOW, null
        );
        when(historyUseCase.get(new GetBidNoticeSummaryHistoryQuery(
                317L, 0, 20, USER_ID, "ADMIN"
        ))).thenReturn(new BidNoticeSummaryHistoryResult(
                32L, List.of(item), 1, 1, 0, 20
        ));

        var response = controller.getHistory(317L, 0, 20, authentication);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().latestMySummaryId()).isEqualTo(32L);
        assertThat(response.getBody().data().content().get(0).parentSummaryId())
                .isEqualTo(31L);
        assertThat(response.getBody().data().content().get(0).revisionNo())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("요약 조회 응답은 공개 계약 필드를 반환한다")
    void getsSummary() {
        when(getUseCase.get(new GetBidNoticeSummaryQuery(SUMMARY_ID, USER_ID, "ADMIN")))
                .thenReturn(summaryResult(false));

        var response = controller.get(SUMMARY_ID, authentication);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("입찰 공고 AI 요약 조회 성공");
        assertThat(response.getBody().data().summaryId()).isEqualTo(SUMMARY_ID);
    }

    @Test
    @DisplayName("요약 수정은 JSON의 전달 필드만 command에 표시한다")
    void updatesSummary() throws Exception {
        when(updateUseCase.update(any())).thenReturn(summaryResult(false));

        controller.update(
                SUMMARY_ID,
                new ObjectMapper().readTree("{\"riskSummary\":\"수정 위험\"}"),
                authentication
        );

        ArgumentCaptor<UpdateBidNoticeSummaryCommand> captor =
                ArgumentCaptor.forClass(UpdateBidNoticeSummaryCommand.class);
        verify(updateUseCase).update(captor.capture());
        assertThat(captor.getValue().overviewSummary().present()).isFalse();
        assertThat(captor.getValue().riskSummary().value()).isEqualTo("수정 위험");
        assertThat(captor.getValue().role()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("명세에 없는 요약 수정 필드는 거부한다")
    void rejectsUnknownUpdateField() throws Exception {
        assertInvalidUpdate("{\"unknownSummary\":\"값\"}");
    }

    @Test
    @DisplayName("문자열이 아닌 요약 수정 값은 거부한다")
    void rejectsNonTextUpdateValues() throws Exception {
        for (String json : List.of(
                "{\"riskSummary\":123}",
                "{\"riskSummary\":null}",
                "{\"riskSummary\":{\"value\":\"위험\"}}"
        )) {
            assertInvalidUpdate(json);
        }
    }

    @Test
    @DisplayName("이력 조회의 잘못된 page와 size 오류를 그대로 전달한다")
    void propagatesHistoryPageBoundaries() {
        when(historyUseCase.get(any())).thenThrow(
                new ValidationException(BiddingErrorCode.BIDDING_INVALID_SUMMARY_REQUEST)
        );

        assertThatThrownBy(() -> controller.getHistory(317L, -1, 51, authentication))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("입찰 접근 권한 오류를 공개 API 경계에서 전달한다")
    void propagatesPermissionFailure() {
        when(getUseCase.get(any())).thenThrow(
                new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED)
        );

        assertThatThrownBy(() -> controller.get(SUMMARY_ID, authentication))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("요약 확정은 요청자와 역할을 command로 전달한다")
    void confirmsSummary() {
        when(confirmUseCase.confirm(any())).thenReturn(
                new ConfirmBidNoticeSummaryResult(SUMMARY_ID, true, USER_ID, NOW, true)
        );

        var response = controller.confirm(SUMMARY_ID, authentication);

        verify(confirmUseCase).confirm(
                new ConfirmBidNoticeSummaryCommand(SUMMARY_ID, USER_ID, "ADMIN")
        );
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().projectCreationAllowed()).isTrue();
    }

    @Test
    @DisplayName("요약 중단은 요청자와 역할을 command로 전달한다")
    void abandonsSummary() {
        when(abandonUseCase.abandon(any())).thenReturn(
                new AbandonBidNoticeSummaryResult(SUMMARY_ID, "ABANDONED", NOW)
        );

        var response = controller.abandon(SUMMARY_ID, authentication);

        verify(abandonUseCase).abandon(
                new AbandonBidNoticeSummaryCommand(SUMMARY_ID, USER_ID, "ADMIN")
        );
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().summaryStatus()).isEqualTo("ABANDONED");
    }

    private BidNoticeSummaryResult summaryResult(boolean confirmed) {
        return new BidNoticeSummaryResult(
                SUMMARY_ID, 317L, null, 1, "검토해줘", "COMPLETED",
                "개요", "금액", "일정", "자격", "과업", "위험",
                confirmed, confirmed ? USER_ID : null,
                confirmed ? NOW : null, null, null, 0, NOW, NOW, NOW
        );
    }

    private void assertInvalidUpdate(String json) throws Exception {
        assertThatThrownBy(() -> controller.update(
                SUMMARY_ID,
                new ObjectMapper().readTree(json),
                authentication
        )).isInstanceOf(ValidationException.class)
                .satisfies(exception -> assertThat(
                        ((ValidationException) exception).getErrorCode()
                ).isEqualTo(BiddingErrorCode.BIDDING_INVALID_SUMMARY_UPDATE));

        verifyNoInteractions(updateUseCase);
    }
}
