package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeItem;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception.NaraBidNoticeNormalizationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaraBidNoticeNormalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NaraBidNoticeNormalizer normalizer =
            new NaraBidNoticeNormalizer();

    @Test
    void normalizesServiceNotice() throws Exception {
        CollectedBidNotice notice = normalizer.normalize(
                readItem(validJson()),
                BidNoticeType.SERVICE
        );

        assertThat(notice.externalId()).isEqualTo("R26BK01663793");
        assertThat(notice.noticeOrder()).isEqualTo("001");
        assertThat(notice.noticeType()).isEqualTo(BidNoticeType.SERVICE);
        assertThat(notice.internationalBidType()).isEqualTo("DOMESTIC");
        assertThat(notice.announcedAt())
                .isEqualTo(LocalDateTime.of(2026, 8, 3, 17, 50, 46));
        assertThat(notice.baseAmount())
                .isEqualByComparingTo(new BigDecimal("182160000"));
        assertThat(notice.estimatedAmount())
                .isEqualByComparingTo(new BigDecimal("165600000"));
        assertThat(notice.jointContractAllowed()).isFalse();

        assertThat(notice.attachments()).hasSize(1);
        assertThat(notice.attachments().get(0).order()).isEqualTo(1);
        assertThat(notice.attachments().get(0).fileName())
                .isEqualTo("입찰공고.pdf");
    }

    @Test
    void rejectsInvalidDate() throws Exception {
        String json = validJson().replace(
                "2026-08-03 17:50:46",
                "invalid-date"
        );

        assertThatThrownBy(() -> normalizer.normalize(
                readItem(json),
                BidNoticeType.SERVICE
        ))
                .isInstanceOf(NaraBidNoticeNormalizationException.class)
                .hasMessageContaining("날짜 형식");
    }

    @Test
    void rejectsInvalidAmount() throws Exception {
        String json = validJson().replace(
                "\"182160000\"",
                "\"not-a-number\""
        );

        assertThatThrownBy(() -> normalizer.normalize(
                readItem(json),
                BidNoticeType.SERVICE
        ))
                .isInstanceOf(NaraBidNoticeNormalizationException.class)
                .hasMessageContaining("금액 형식");
    }

    @Test
    void rejectsMissingRequiredNoticeName() throws Exception {
        String json = validJson().replace(
                "\"스마트시티 통합관제 용역\"",
                "\" \""
        );

        assertThatThrownBy(() -> normalizer.normalize(
                readItem(json),
                BidNoticeType.SERVICE
        ))
                .isInstanceOf(NaraBidNoticeNormalizationException.class)
                .hasMessageContaining("공고명");
    }

    @Test
    void ignoresAttachmentWhenUrlIsMissing() throws Exception {
        String json = validJson().replace(
                "\"https://example.test/notice.pdf\"",
                "\"\""
        );

        CollectedBidNotice notice = normalizer.normalize(
                readItem(json),
                BidNoticeType.SERVICE
        );

        assertThat(notice.attachments()).isEmpty();
        assertThat(notice.hasAttachments()).isFalse();
    }

    private NaraBidNoticeItem readItem(String json) throws Exception {
        return objectMapper.readValue(json, NaraBidNoticeItem.class);
    }

    private String validJson() {
        return """
                {
                  "bidNtceNo": "R26BK01663793",
                  "bidNtceOrd": "001",
                  "bidNtceNm": "스마트시티 통합관제 용역",
                  "ntceInsttNm": "제주특별자치도",
                  "dminsttNm": "제주특별자치도",
                  "ntceKindNm": "변경공고",
                  "intrbidYn": "N",
                  "bidNtceDt": "2026-08-03 17:50:46",
                  "bidBeginDt": "2026-08-04 10:00:00",
                  "bidClseDt": "2026-08-11 10:00:00",
                  "opengDt": "2026-08-11 11:00:00",
                  "asignBdgtAmt": "182160000",
                  "presmptPrce": "165600000",
                  "bidMethdNm": "전자입찰",
                  "cntrctCnclsMthdNm": "일반경쟁",
                  "cmmnSpldmdMethdNm": "(없음)공동수급불허",
                  "bidNtceDtlUrl": "https://example.test/notices/1",
                  "ntceSpecFileNm1": "입찰공고.pdf",
                  "ntceSpecDocUrl1": "https://example.test/notice.pdf"
                }
                """;
    }
}