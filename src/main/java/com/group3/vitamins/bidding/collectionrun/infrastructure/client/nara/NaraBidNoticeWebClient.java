package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara;

import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeApiResponse;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeSearchRequest;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception.NaraBidNoticeClientException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.format.DateTimeFormatter;

@Component
public class NaraBidNoticeWebClient implements NaraBidNoticeClient {

    private static final String CONSTRUCTION_OPERATION =
            "/getBidPblancListInfoCnstwkPPSSrch";
    private static final String SERVICE_OPERATION =
            "/getBidPblancListInfoServcPPSSrch";
    private static final DateTimeFormatter REQUEST_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");


    private final WebClient webClient;
    private final NaraBidNoticeClientProperties properties;


    public NaraBidNoticeWebClient(
            @Qualifier("naraWebClient") WebClient webClient,
            NaraBidNoticeClientProperties properties
    ) {
        this.webClient = webClient;
        this.properties = properties;
    }

    // 나라장터 공사 공고 검색 API를 호출합니다.
    @Override
    public NaraBidNoticeApiResponse searchConstructionNotices(
            NaraBidNoticeSearchRequest request
    ) {
        return search(CONSTRUCTION_OPERATION, request);
    }

    // 나라장터 용역 공고 검색 API를 호출합니다.
    @Override
    public NaraBidNoticeApiResponse searchServiceNotices(
            NaraBidNoticeSearchRequest request
    ) {
        return search(SERVICE_OPERATION, request);
    }

    private NaraBidNoticeApiResponse search(
            String operation,
            NaraBidNoticeSearchRequest request
    ) {
        validateConfiguration();

        try {
            NaraBidNoticeApiResponse response = webClient.get()
                    .uri(builder -> buildUri(builder, operation, request))
                    .retrieve()
                    .bodyToMono(NaraBidNoticeApiResponse.class)
                    .block(properties.readTimeout());

            validateResponse(response);
            return response;
        } catch (NaraBidNoticeClientException exception) {
            throw exception;
        } catch (WebClientResponseException exception) {
            throw new NaraBidNoticeClientException(
                    "나라장터 입찰공고 API가 HTTP 오류를 반환했습니다.",
                    exception.getStatusCode().is5xxServerError(),
                    exception
            );
        } catch (WebClientRequestException exception) {
            throw new NaraBidNoticeClientException(
                    "나라장터 입찰공고 API에 연결하지 못했습니다.",
                    true,
                    exception
            );
        } catch (Exception exception) {
            // 인증키가 포함된 실제 요청 URL을 예외 메시지에 남기지 않습니다.
            throw new NaraBidNoticeClientException(
                    "나라장터 입찰공고 API 호출에 실패했습니다.",
                    true,
                    exception
            );
        }
    }

    private URI buildUri(
            UriBuilder builder,
            String operation,
            NaraBidNoticeSearchRequest request
    ) {
        builder.path(operation)
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("type", "json")
                .queryParam("inqryDiv", "1")
                .queryParam("inqryBgnDt", request.startedAt().format(REQUEST_TIME_FORMAT))
                .queryParam("inqryEndDt", request.endedAt().format(REQUEST_TIME_FORMAT))
                .queryParam("pageNo", request.pageNumber())
                .queryParam("numOfRows", request.pageSize())
                .queryParam("bidClseExcpYn", Boolean.TRUE.equals(request.excludeClosed()) ? "Y" : "N");

        addOptionalParameter(builder, "bidNtceNm", request.keyword());
        addOptionalParameter(builder, "prtcptLmtRgnCd", request.regionCode());
        addOptionalParameter(builder, "indstrytyCd", request.industryCode());
        addOptionalParameter(builder, "presmptPrceBgn", request.minimumEstimatedPrice());
        addOptionalParameter(builder, "presmptPrceEnd", request.maximumEstimatedPrice());
        addOptionalParameter(builder, "intrntnlDivCd",
                convertInternationalBidType(request.internationalBidType()));

        return builder.build();
    }

    private void addOptionalParameter(UriBuilder builder, String name, Object value) {
        if (value != null && (!(value instanceof String text) || !text.isBlank())) {
            builder.queryParam(name, value);
        }
    }

    private String convertInternationalBidType(String type) {
        return switch (type == null ? "" : type) {
            case "DOMESTIC" -> "1";
            case "INTERNATIONAL" -> "2";
            default -> null;
        };
    }

    private void validateConfiguration() {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new NaraBidNoticeClientException(
                    "나라장터 API 기본 주소가 설정되지 않았습니다."
            );
        }
        if (properties.serviceKey() == null || properties.serviceKey().isBlank()) {
            throw new NaraBidNoticeClientException(
                    "나라장터 API 인증키가 설정되지 않았습니다."
            );
        }
    }

    private void validateResponse(NaraBidNoticeApiResponse response) {
        if (response == null || response.response() == null
                || response.response().header() == null) {
            throw new NaraBidNoticeClientException(
                    "나라장터 API 응답 형식이 올바르지 않습니다."
            );
        }
        if (!response.response().header().isSuccess()) {
            throw new NaraBidNoticeClientException(
                    "나라장터 API가 요청 처리를 거부했습니다."
            );
        }
    }
}
