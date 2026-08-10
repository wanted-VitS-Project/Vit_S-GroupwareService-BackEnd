package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara;

import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeApiResponse;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeSearchRequest;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception.NaraBidNoticeClientException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaraBidNoticeWebClientTest {

    @Test
    void searchesServiceNoticesWithExpectedParameters() {
        AtomicReference<ClientRequest> captured = new AtomicReference<>();
        NaraBidNoticeWebClient client = createClient(captured, successJson());

        NaraBidNoticeApiResponse response =
                client.searchServiceNotices(searchRequest());

        String uri = captured.get().url().toString();

        assertThat(uri)
                .contains("/getBidPblancListInfoServcPPSSrch")
                .contains("inqryBgnDt=202608010900")
                .contains("inqryEndDt=202608101800")
                .contains("bidNtceNm=smart-city")
                .contains("prtcptLmtRgnCd=11")
                .contains("indstrytyCd=6202")
                .contains("intrntnlDivCd=1");

        assertThat(response.response().body().safeItems()).hasSize(1);
        assertThat(
                response.response()
                        .body()
                        .safeItems()
                        .get(0)
                        .path("bidNtceNo")
                        .asText()
        ).isEqualTo("R26BK00000001");
    }

    @Test
    void rejectsFailedNaraResponse() {
        NaraBidNoticeWebClient client =
                createClient(new AtomicReference<>(), failureJson());

        assertThatThrownBy(() -> client.searchServiceNotices(searchRequest()))
                .isInstanceOf(NaraBidNoticeClientException.class)
                .hasMessage("나라장터 API가 요청 처리를 거부했습니다.");
    }

    private NaraBidNoticeWebClient createClient(
            AtomicReference<ClientRequest> captured,
            String responseBody
    ) {
        ExchangeFunction exchangeFunction = request -> {
            captured.set(request);
            return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body(responseBody)
                    .build());
        };

        WebClient webClient = WebClient.builder()
                .baseUrl("https://example.test")
                .exchangeFunction(exchangeFunction)
                .build();

        NaraBidNoticeClientProperties properties =
                new NaraBidNoticeClientProperties(
                        "https://example.test",
                        "test-key",
                        100,
                        Duration.ofSeconds(5),
                        Duration.ofSeconds(15),
                        2_097_152
                );

        return new NaraBidNoticeWebClient(webClient, properties);
    }

    private NaraBidNoticeSearchRequest searchRequest() {
        return new NaraBidNoticeSearchRequest(
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 10, 18, 0),
                "smart-city",
                "11",
                "6202",
                100_000_000L,
                1_000_000_000L,
                true,
                "DOMESTIC",
                1,
                100
        );
    }

    private String successJson() {
        return """
                {
                  "response": {
                    "header": {"resultCode": "00", "resultMsg": "정상"},
                    "body": {
                      "items": [{
                        "bidNtceNo": "R26BK00000001",
                        "bidNtceOrd": "000",
                        "bidNtceNm": "스마트시티 통합관제 용역",
                        "asignBdgtAmt": "200000000"
                      }],
                      "numOfRows": 1,
                      "pageNo": 1,
                      "totalCount": 1
                    }
                  }
                }
                """;
    }

    private String failureJson() {
        return """
                {
                  "response": {
                    "header": {
                      "resultCode": "30",
                      "resultMsg": "SERVICE KEY IS NOT REGISTERED"
                    },
                    "body": null
                  }
                }
                """;
    }
}
