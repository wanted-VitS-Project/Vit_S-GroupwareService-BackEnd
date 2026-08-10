package com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.adapter;

import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.entity.BidNoticeAttachmentJpaEntity;
import com.group3.vitamins.bidding.bidnotice.infrastructure.persistence.repository.SpringDataBidNoticeAttachmentRepository;
import com.group3.vitamins.bidding.collectionrun.application.model.CollectedBidNotice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BidNoticeAttachmentSynchronizerTest {

    private SpringDataBidNoticeAttachmentRepository repository;
    private BidNoticeAttachmentSynchronizer synchronizer;

    @BeforeEach
    void setUp() {
        repository = mock(SpringDataBidNoticeAttachmentRepository.class);
        synchronizer = new BidNoticeAttachmentSynchronizer(repository);
    }

    @Test
    @DisplayName("논리 삭제된 같은 순번의 첨부파일을 새 행 없이 복구한다")
    void reactivatesSoftDeletedAttachment() {
        BidNoticeAttachmentJpaEntity existing = mock(BidNoticeAttachmentJpaEntity.class);
        when(existing.getBidNoticeId()).thenReturn(10L);
        when(existing.getAttachmentOrder()).thenReturn((short) 1);
        when(repository.findAllByBidNoticeIdIn(anyCollection()))
                .thenReturn(List.of(existing));
        CollectedBidNotice.Attachment incoming =
                new CollectedBidNotice.Attachment(1, "공고문.pdf", "https://example.test/file");

        synchronizer.synchronize(
                Map.of(10L, List.of(incoming)),
                LocalDateTime.of(2026, 8, 10, 22, 0)
        );

        verify(existing).updateFrom(eq(incoming), any(LocalDateTime.class));
        ArgumentCaptor<List<BidNoticeAttachmentJpaEntity>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(existing);
    }

    @Test
    @DisplayName("외부 응답이 빈 첨부 목록이면 기존 첨부파일을 보존한다")
    void preservesAttachmentsWhenExternalResponseOmitsThem() {
        BidNoticeAttachmentJpaEntity existing = mock(BidNoticeAttachmentJpaEntity.class);
        when(existing.getBidNoticeId()).thenReturn(10L);
        when(existing.getAttachmentOrder()).thenReturn((short) 1);
        when(repository.findAllByBidNoticeIdIn(anyCollection()))
                .thenReturn(List.of(existing));

        synchronizer.synchronize(
                Map.of(10L, List.of()),
                LocalDateTime.of(2026, 8, 10, 22, 0)
        );

        verify(existing, never()).softDelete(any(LocalDateTime.class));
        verify(repository).saveAll(List.of());
    }
}
