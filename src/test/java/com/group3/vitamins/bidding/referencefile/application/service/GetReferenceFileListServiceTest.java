package com.group3.vitamins.bidding.referencefile.application.service;

import com.group3.vitamins.bidding.collectioncondition.application.policy.BiddingAccessPolicy;
import com.group3.vitamins.bidding.collectioncondition.domain.exception.BiddingErrorCode;
import com.group3.vitamins.bidding.referencefile.application.query.GetReferenceFileListQuery;
import com.group3.vitamins.bidding.referencefile.application.result.ReferenceFileListResult;
import com.group3.vitamins.bidding.referencefile.domain.model.BidReferenceFile;
import com.group3.vitamins.bidding.referencefile.domain.model.ReferenceFileIndexStatus;
import com.group3.vitamins.bidding.referencefile.domain.model.ReferenceFileUploadStatus;
import com.group3.vitamins.bidding.referencefile.domain.repository.BidReferenceFileRepository;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("GetReferenceFileListService 입찰 기준자료 파일함 조회")
class GetReferenceFileListServiceTest {

    private static final Long COMPANY_ID = 10L;
    private static final String USER_ID = "EMP001";
    private static final String ROLE = "MEMBER";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    private BidReferenceFileRepository referenceFileRepository;
    private BiddingAccessPolicy biddingAccessPolicy;
    private GetReferenceFileListService service;

    @BeforeEach
    void setUp() {
        referenceFileRepository = mock(BidReferenceFileRepository.class);
        biddingAccessPolicy = mock(BiddingAccessPolicy.class);
        CurrentCompanyIdProvider companyIdProvider = mock(CurrentCompanyIdProvider.class);

        service = new GetReferenceFileListService(
                referenceFileRepository, biddingAccessPolicy, companyIdProvider
        );

        when(companyIdProvider.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    @DisplayName("업로드·인덱싱이 모두 완료된 파일만 selectable=true로 내려준다")
    void mapsSelectableCorrectly() {
        when(referenceFileRepository.findAllActiveByCompanyId(COMPANY_ID))
                .thenReturn(List.of(readyFile(), uploadingFile()));

        ReferenceFileListResult result = service.get(
                new GetReferenceFileListQuery(USER_ID, ROLE)
        );

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).selectable()).isTrue();
        assertThat(result.content().get(0).uploadStatus()).isEqualTo("COMPLETED");
        assertThat(result.content().get(0).indexStatus()).isEqualTo("COMPLETED");
        assertThat(result.content().get(1).selectable()).isFalse();
        assertThat(result.content().get(1).uploadStatus()).isEqualTo("UPLOADING");
    }

    @Test
    @DisplayName("등록된 기준자료가 없으면 빈 목록을 반환한다")
    void returnsEmptyListWhenNoneRegistered() {
        when(referenceFileRepository.findAllActiveByCompanyId(COMPANY_ID))
                .thenReturn(List.of());

        ReferenceFileListResult result = service.get(
                new GetReferenceFileListQuery(USER_ID, ROLE)
        );

        assertThat(result.content()).isEmpty();
    }

    @Test
    @DisplayName("입찰 관리 권한이 없으면 저장소를 건드리기 전에 403으로 막힌다")
    void shortCircuitsOnAccessDenied() {
        doThrow(new ForbiddenException(BiddingErrorCode.BIDDING_ACCESS_PERMISSION_REQUIRED))
                .when(biddingAccessPolicy).assertAccess(USER_ID, ROLE);

        assertThatThrownBy(() -> service.get(new GetReferenceFileListQuery(USER_ID, ROLE)))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(referenceFileRepository);
    }

    private BidReferenceFile readyFile() {
        return new BidReferenceFile(
                1L, COMPANY_ID, "원가계산_기준.pdf", "pdf", "application/pdf", 204800L,
                "companies/10/bidding/reference-files/a.pdf",
                ReferenceFileUploadStatus.COMPLETED, ReferenceFileIndexStatus.COMPLETED,
                "attempt-1", 0, null, null, NOW, NOW, USER_ID, NOW, NOW, null
        );
    }

    private BidReferenceFile uploadingFile() {
        return new BidReferenceFile(
                2L, COMPANY_ID, "제안서.pdf", "pdf", "application/pdf", 10240L,
                "companies/10/bidding/reference-files/b.pdf",
                ReferenceFileUploadStatus.UPLOADING, ReferenceFileIndexStatus.PENDING,
                null, 0, null, NOW.plusMinutes(10), null, null, USER_ID, NOW, NOW, null
        );
    }
}
