package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// 나라장터가 반환한 입찰공고 원본 한 건을 담습니다.
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaraBidNoticeItem(
        String bidNtceNo,
        String bidNtceOrd,
        String bidNtceNm,
        String ntceInsttNm,
        String dminsttNm,
        String ntceKindNm,
        String intrbidYn,
        String bidNtceDt,
        String bidBeginDt,
        String bidClseDt,
        String opengDt,
        String bdgtAmt,
        String asignBdgtAmt,
        String presmptPrce,
        String bidMethdNm,
        String cntrctCnclsMthdNm,
        String bidQlfctRgstCntnts,
        String cmmnSpldmdMethdNm,
        String bidNtceDtlUrl,

        String ntceSpecFileNm1,
        String ntceSpecDocUrl1,
        String ntceSpecFileNm2,
        String ntceSpecDocUrl2,
        String ntceSpecFileNm3,
        String ntceSpecDocUrl3,
        String ntceSpecFileNm4,
        String ntceSpecDocUrl4,
        String ntceSpecFileNm5,
        String ntceSpecDocUrl5,
        String ntceSpecFileNm6,
        String ntceSpecDocUrl6,
        String ntceSpecFileNm7,
        String ntceSpecDocUrl7,
        String ntceSpecFileNm8,
        String ntceSpecDocUrl8,
        String ntceSpecFileNm9,
        String ntceSpecDocUrl9,
        String ntceSpecFileNm10,
        String ntceSpecDocUrl10
) {

    // 공사·용역 API에서 서로 다른 예산 필드를 하나의 값으로 선택합니다.
    public String resolvedBudgetAmount() {
        return hasText(bdgtAmt) ? bdgtAmt : asignBdgtAmt;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}