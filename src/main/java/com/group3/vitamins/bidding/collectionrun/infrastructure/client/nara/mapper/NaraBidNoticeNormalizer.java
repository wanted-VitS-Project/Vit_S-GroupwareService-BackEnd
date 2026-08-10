package com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.mapper;

import com.group3.vitamins.bidding.collectioncondition.domain.model.BidNoticeType;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.dto.NaraBidNoticeItem;
import com.group3.vitamins.bidding.collectionrun.infrastructure.client.nara.exception.NaraBidNoticeNormalizationException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
public class NaraBidNoticeNormalizer {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm[:ss]");

    // 나라장터 공고 원문 한 건을 내부 수집 공고로 변환합니다.
    public CollectedBidNotice normalize(
            NaraBidNoticeItem item,
            BidNoticeType noticeType
    ) {
        if (item == null || noticeType == null) {
            throw new NaraBidNoticeNormalizationException(
                    "나라장터 공고 또는 공고 유형이 없습니다."
            );
        }

        String externalId = requireText(item.bidNtceNo(), "입찰공고번호");
        String noticeOrder = requireText(item.bidNtceOrd(), "공고차수");
        String noticeName = requireText(item.bidNtceNm(), "공고명");

        return new CollectedBidNotice(
                externalId,
                noticeOrder,
                noticeType,
                noticeName,
                trimToNull(item.ntceInsttNm()),
                trimToNull(item.dminsttNm()),
                trimToNull(item.ntceKindNm()),
                normalizeInternationalType(item.intrbidYn()),
                parseDate(item.bidNtceDt(), externalId),
                parseDate(item.bidBeginDt(), externalId),
                parseDate(item.bidClseDt(), externalId),
                parseDate(item.opengDt(), externalId),
                parseAmount(item.resolvedBudgetAmount(), externalId),
                parseAmount(item.presmptPrce(), externalId),
                trimToNull(item.bidMethdNm()),
                trimToNull(item.cntrctCnclsMthdNm()),
                trimToNull(item.bidQlfctRgstCntnts()),
                resolveJointContractAllowed(item.cmmnSpldmdMethdNm()),
                trimToNull(item.cmmnSpldmdMethdNm()),
                trimToNull(item.bidNtceDtlUrl()),
                collectAttachments(item)
        );
    }

    private LocalDateTime parseDate(String value, String externalId) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        try {
            return LocalDateTime.parse(normalized, DATE_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            throw new NaraBidNoticeNormalizationException(
                    "나라장터 공고 날짜 형식이 올바르지 않습니다. externalId=" + externalId,
                    exception
            );
        }
    }

    private BigDecimal parseAmount(String value, String externalId) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }

        try {
            return new BigDecimal(normalized.replace(",", ""));
        } catch (NumberFormatException exception) {
            throw new NaraBidNoticeNormalizationException(
                    "나라장터 공고 금액 형식이 올바르지 않습니다. externalId=" + externalId,
                    exception
            );
        }
    }

    private String normalizeInternationalType(String value) {
        return switch (value == null ? "" : value.trim()) {
            case "Y" -> "INTERNATIONAL";
            case "N" -> "DOMESTIC";
            default -> null;
        };
    }

    private Boolean resolveJointContractAllowed(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        return !normalized.contains("불허");
    }

    private List<CollectedBidNotice.Attachment> collectAttachments(
            NaraBidNoticeItem item
    ) {
        String[] names = {
                item.ntceSpecFileNm1(), item.ntceSpecFileNm2(),
                item.ntceSpecFileNm3(), item.ntceSpecFileNm4(),
                item.ntceSpecFileNm5(), item.ntceSpecFileNm6(),
                item.ntceSpecFileNm7(), item.ntceSpecFileNm8(),
                item.ntceSpecFileNm9(), item.ntceSpecFileNm10()
        };
        String[] urls = {
                item.ntceSpecDocUrl1(), item.ntceSpecDocUrl2(),
                item.ntceSpecDocUrl3(), item.ntceSpecDocUrl4(),
                item.ntceSpecDocUrl5(), item.ntceSpecDocUrl6(),
                item.ntceSpecDocUrl7(), item.ntceSpecDocUrl8(),
                item.ntceSpecDocUrl9(), item.ntceSpecDocUrl10()
        };

        List<CollectedBidNotice.Attachment> attachments = new ArrayList<>();
        for (int index = 0; index < names.length; index++) {
            String name = trimToNull(names[index]);
            String url = trimToNull(urls[index]);

            if (name != null && url != null) {
                attachments.add(
                        new CollectedBidNotice.Attachment(index + 1, name, url)
                );
            }
        }
        return attachments;
    }

    private String requireText(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new NaraBidNoticeNormalizationException(
                    "나라장터 공고의 필수값이 없습니다. field=" + fieldName
            );
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.replace('\u00A0', ' ').trim();
        return normalized.isEmpty() ? null : normalized;
    }
}