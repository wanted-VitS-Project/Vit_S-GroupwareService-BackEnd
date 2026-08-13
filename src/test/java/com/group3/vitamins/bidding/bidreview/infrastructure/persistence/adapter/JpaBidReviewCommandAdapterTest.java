package com.group3.vitamins.bidding.bidreview.infrastructure.persistence.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReview;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewDocument;
import com.group3.vitamins.bidding.bidreview.domain.model.BidReviewStatus;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewDocumentJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.entity.BidReviewOutboxJpaEntity;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewDocumentJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewJpaRepository;
import com.group3.vitamins.bidding.bidreview.infrastructure.persistence.repository.BidReviewOutboxJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bid-review;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        JpaBidReviewCommandAdapter.class,
        JpaBidReviewCommandAdapterTest.JacksonConfig.class
})
@DisplayName("JpaBidReviewCommandAdapter 검토·문서·Outbox 저장 및 활성 검토 판정")
class JpaBidReviewCommandAdapterTest {

    private static final Long COMPANY_ID = 10L;
    private static final Long OTHER_COMPANY_ID = 20L;
    private static final Long NOTICE_ID = 30L;
    private static final String USER_ID = "EMP001";
    private static final String PROMPT = "기준 문서와 비교해서 금액·일정 리스크를 짚어줘.";
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 12, 10, 0);

    @Autowired
    private JpaBidReviewCommandAdapter commandAdapter;

    @Autowired
    private BidReviewJpaRepository reviewRepository;

    @Autowired
    private BidReviewDocumentJpaRepository documentRepository;

    @Autowired
    private BidReviewOutboxJpaRepository outboxRepository;

    @Test
    @DisplayName("검토, 선택 문서, Worker 발행용 Outbox를 한 트랜잭션으로 저장한다")
    void savesReviewDocumentsAndOutboxTogether() {
        String attemptId = UUID.randomUUID().toString();
        BidReview review = pendingReview(COMPANY_ID, attemptId);
        List<BidReviewDocument> documents = List.of(
                BidReviewDocument.createBidAttachment(100L, "제안요청서.pdf", NOW),
                BidReviewDocument.createInternalReference(200L, "회사소개서.pdf", NOW)
        );

        BidReview saved = commandAdapter.savePendingWithDocumentsAndOutbox(review, documents);

        assertThat(saved.reviewId()).isNotNull();
        assertThat(saved.reviewStatus()).isEqualTo(BidReviewStatus.PENDING);

        List<BidReviewDocumentJpaEntity> savedDocuments = documentRepository
                .findAllByReviewIdOrderByDocumentRoleAscReviewDocumentIdAsc(saved.reviewId());
        assertThat(savedDocuments).hasSize(2);
        assertThat(savedDocuments)
                .extracting(BidReviewDocumentJpaEntity::getFileName)
                .containsExactlyInAnyOrder("제안요청서.pdf", "회사소개서.pdf");
        assertThat(savedDocuments)
                .allSatisfy(document ->
                        assertThat(document.getReviewId()).isEqualTo(saved.reviewId()));

        List<BidReviewOutboxJpaEntity> outboxEntries = outboxRepository.findAll();
        assertThat(outboxEntries).hasSize(1);
        assertThat(outboxEntries.get(0).getReviewId()).isEqualTo(saved.reviewId());
        assertThat(outboxEntries.get(0).getAttemptId()).isEqualTo(attemptId);
        assertThat(outboxEntries.get(0).getPublishStatus()).isEqualTo("PENDING");
        assertThat(outboxEntries.get(0).getEventType()).isEqualTo("BID_REVIEW_REQUESTED");
    }

    @Test
    @DisplayName("같은 회사·공고·요청자의 PENDING/PROCESSING 검토만 활성으로 판정한다")
    void detectsActiveProcessingScopedByCompany() {
        commandAdapter.savePendingWithDocumentsAndOutbox(
                pendingReview(COMPANY_ID, UUID.randomUUID().toString()),
                List.of(BidReviewDocument.createBidAttachment(100L, "제안요청서.pdf", NOW))
        );

        assertThat(commandAdapter.existsProcessing(COMPANY_ID, NOTICE_ID, USER_ID)).isTrue();
        assertThat(commandAdapter.existsProcessing(OTHER_COMPANY_ID, NOTICE_ID, USER_ID)).isFalse();
    }

    @Test
    @DisplayName("완료된 검토는 활성 검토로 판정하지 않는다")
    void ignoresCompletedReviewWhenDetectingActiveProcessing() {
        BidReviewJpaEntity completed = BidReviewJpaEntity.from(
                pendingReview(COMPANY_ID, UUID.randomUUID().toString())
                        .complete("검토 결과 요약", NOW)
        );
        reviewRepository.saveAndFlush(completed);

        assertThat(commandAdapter.existsProcessing(COMPANY_ID, NOTICE_ID, USER_ID)).isFalse();
    }

    private BidReview pendingReview(Long companyId, String attemptId) {
        return BidReview.createPending(
                companyId, NOTICE_ID, USER_ID, PROMPT, attemptId, NOW
        );
    }

    @TestConfiguration
    static class JacksonConfig {

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }
}
