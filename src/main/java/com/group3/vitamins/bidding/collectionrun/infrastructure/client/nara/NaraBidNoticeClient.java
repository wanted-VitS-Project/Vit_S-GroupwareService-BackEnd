package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara;

import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeApiResponse;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeSearchRequest;

public interface NaraBidNoticeClient {

    // 나라장터 공사 공고를 검색합니다.
    NaraBidNoticeApiResponse searchConstructionNotices(
            NaraBidNoticeSearchRequest request
    );

    // 나라장터 용역 공고를 검색합니다.
    NaraBidNoticeApiResponse searchServiceNotices(
            NaraBidNoticeSearchRequest request
    );

}