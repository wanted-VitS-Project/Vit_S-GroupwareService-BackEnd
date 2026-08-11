package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectioncondition.domain.model.CollectionConditionFilter;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePage;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNoticePayload;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRequestCombination;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectionRunFailureType;
import com.group3.vitamins.bidding.collectionrun.application.port.CollectionSourceCollectorPort;
import com.group3.vitamins.bidding.collectionrun.domain.model.CollectionRunConditionSnapshot;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeApiResponse;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeSearchRequest;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception.NaraBidNoticeClientException;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception.NaraBidNoticeNormalizationException;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.mapper.NaraBidNoticePayloadMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class NaraCollectionSourceAdapter implements CollectionSourceCollectorPort {

    private static final String SOURCE_CODE = "NARA";

    private final NaraBidNoticeClient client;
    private final NaraBidNoticePayloadMapper payloadMapper;

    public NaraCollectionSourceAdapter(
            NaraBidNoticeClient client,
            NaraBidNoticePayloadMapper payloadMapper
    ) {
        this.client = client;
        this.payloadMapper = payloadMapper;
    }

    // 나라장터 수집처 코드를 반환합니다.
    @Override
    public String supportedSourceCode() {
        return SOURCE_CODE;
    }

    // task가 지정한 외부 요청 조합 한 건의 공고를 수집합니다.
    @Override
    public CollectedBidNoticePage collect(
            CollectionRunConditionSnapshot condition,
            CollectionRequestCombination target,
            int pageSize
    ) {
        validateRequest(condition, target, pageSize);

        try {
            NaraBidNoticeApiResponse response = search(
                    target.noticeType(),
                    createRequest(condition, target, pageSize)
            );
            return successPage(response, target);
        } catch (NaraBidNoticeNormalizationException exception) {
            return failurePage(
                    target,
                    CollectionRunFailureType.RESPONSE_PARSING_FAILURE,
                    false
            );
        } catch (NaraBidNoticeClientException exception) {
            return failurePage(
                    target,
                    CollectionRunFailureType.CONNECTION_FAILURE,
                    exception.isRetryable()
            );
        }
    }

    private CollectedBidNoticePage successPage(
            NaraBidNoticeApiResponse response,
            CollectionRequestCombination target
    ) {
        NaraBidNoticeApiResponse.Body body = response.response().body();
        if (body == null) {
            return new CollectedBidNoticePage(
                    List.of(), List.of(), target.pageNumber(), 0, false
            );
        }

        Map<String, CollectedBidNoticePayload> uniquePayloads = new LinkedHashMap<>();
        body.safeItems().stream()
                .map(item -> payloadMapper.map(item, target.noticeType()))
                .forEach(payload -> uniquePayloads.put(noticeKey(payload), payload));

        return new CollectedBidNoticePage(
                List.copyOf(uniquePayloads.values()),
                List.of(),
                target.pageNumber(),
                safeTotalCount(body.totalCount()),
                hasNext(body)
        );
    }

    private CollectedBidNoticePage failurePage(
            CollectionRequestCombination target,
            CollectionRunFailureType failureType,
            boolean retryable
    ) {
        return new CollectedBidNoticePage(
                List.of(),
                List.of(new CollectedBidNoticePage.CollectionFailure(
                        target.noticeType(),
                        target.keyword(),
                        target.regionCode(),
                        target.industryCode(),
                        target.pageNumber(),
                        failureType,
                        retryable
                )),
                target.pageNumber(),
                0,
                false
        );
    }

    private NaraBidNoticeApiResponse search(
            BidNoticeType noticeType,
            NaraBidNoticeSearchRequest request
    ) {
        return switch (noticeType) {
            case CONSTRUCTION -> client.searchConstructionNotices(request);
            case SERVICE -> client.searchServiceNotices(request);
        };
    }

    private NaraBidNoticeSearchRequest createRequest(
            CollectionRunConditionSnapshot condition,
            CollectionRequestCombination target,
            int pageSize
    ) {
        CollectionConditionFilter filters = condition.filters();
        return new NaraBidNoticeSearchRequest(
                condition.collectionStartedAt(),
                condition.collectionEndedAt(),
                target.keyword(),
                target.regionCode(),
                target.industryCode(),
                filters.minimumEstimatedPrice(),
                filters.maximumEstimatedPrice(),
                filters.excludeClosed(),
                filters.internationalBidType() == null
                        ? null
                        : filters.internationalBidType().name(),
                target.pageNumber(),
                pageSize
        );
    }

    private String noticeKey(CollectedBidNoticePayload payload) {
        return payload.notice().externalId() + "|" + payload.notice().noticeOrder();
    }

    private boolean hasNext(NaraBidNoticeApiResponse.Body body) {
        int currentPage = body.pageNo() == null ? 1 : body.pageNo();
        int rows = body.numOfRows() == null ? 0 : body.numOfRows();
        int totalCount = safeTotalCount(body.totalCount());
        return rows > 0 && (long) currentPage * rows < totalCount;
    }

    private int safeTotalCount(Integer totalCount) {
        return totalCount == null ? 0 : Math.max(totalCount, 0);
    }

    private void validateRequest(
            CollectionRunConditionSnapshot condition,
            CollectionRequestCombination target,
            int pageSize
    ) {
        if (condition == null) {
            throw new IllegalArgumentException("수집 조건 스냅샷은 필수입니다.");
        }
        if (!SOURCE_CODE.equals(condition.sourceCode())) {
            throw new IllegalArgumentException("나라장터 수집 조건이 아닙니다.");
        }
        if (target == null || !condition.noticeTypes().contains(target.noticeType())) {
            throw new IllegalArgumentException("허용되지 않은 공고 유형입니다.");
        }
        if (target.pageNumber() < 1 || pageSize < 1) {
            throw new IllegalArgumentException("페이지 번호와 크기는 1 이상이어야 합니다.");
        }
    }
}
