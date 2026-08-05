package com.group3.vitamins.vitamate.application.support;

import com.group3.vitamins.vitamate.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.application.result.CreateVitamateAnalysisResult;
import com.group3.vitamins.vitamate.application.result.StartVitamateAnalysisResult;
import com.group3.vitamins.vitamate.domain.model.AnalysisStatus;
import com.group3.vitamins.vitamate.infrastructure.persistence.entity.VitamateAnalysisEntity;
import com.group3.vitamins.vitamate.infrastructure.persistence.repository.VitamateAnalysisJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * VitamateAnalysisStateManager가 Spring 프록시를 통해 호출될 때 REQUIRES_NEW 트랜잭션이 실제로 커밋되는지 검증한다.
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vitamate-state-manager-tx;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
        VitamateAnalysisStateManager.class,
        VitamateAnalysisStateManagerTransactionTest.TestBeans.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("VitamateAnalysisStateManager 트랜잭션 경계")
class VitamateAnalysisStateManagerTransactionTest {

    private static final Long VITAMATE_BLOCK_ID = 1L;
    private static final String REQUESTED_BY = "EMP001";
    private static final String REQUEST_HASH = "0".repeat(64);
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 8, 5, 10, 0);

    @Autowired
    private VitamateAnalysisStateManager stateManager;

    @Autowired
    private VitamateAnalysisJpaRepository analysisRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate rollbackOnlyTransaction;

    @BeforeEach
    void setUp() {
        analysisRepository.deleteAll();
        rollbackOnlyTransaction = new TransactionTemplate(transactionManager);
        rollbackOnlyTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    }

    @Test
    @DisplayName("바깥 트랜잭션이 롤백되어도 PROCESSING 선점은 커밋된다")
    void startProcessingSurvivesOuterRollback() {
        Long analysisId = savePendingAnalysis("state-start-key");

        rollbackOnlyTransaction.executeWithoutResult(status -> {
            Optional<StartVitamateAnalysisResult> result = stateManager.startProcessing(analysisId);

            assertThat(result).isPresent();
            status.setRollbackOnly();
        });

        VitamateAnalysisEntity reloaded = reload(analysisId);
        assertThat(reloaded.getAnalysisStatus()).isEqualTo(AnalysisStatus.PROCESSING);
        assertThat(reloaded.getProcessingAttemptId()).isNotBlank();
        assertThat(reloaded.getProcessingStartedAt()).isNotNull();
        assertThat(reloaded.getLeaseExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("바깥 트랜잭션이 롤백되어도 FAILED 마감은 커밋된다")
    void failProcessingSurvivesOuterRollback() {
        Long analysisId = savePendingAnalysis("state-fail-key");
        StartVitamateAnalysisResult started = stateManager.startProcessing(analysisId).orElseThrow();

        rollbackOnlyTransaction.executeWithoutResult(status -> {
            boolean failed = stateManager.failProcessing(analysisId, started.attemptId(), "python error");

            assertThat(failed).isTrue();
            status.setRollbackOnly();
        });

        VitamateAnalysisEntity reloaded = reload(analysisId);
        assertThat(reloaded.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(reloaded.getErrorMessage()).isEqualTo("python error");
        assertThat(reloaded.getCompletedAt()).isNotNull();
        assertThat(reloaded.getLeaseExpiresAt()).isNull();
    }

    private Long savePendingAnalysis(String idempotencyKey) {
        VitamateAnalysisEntity saved = analysisRepository.saveAndFlush(VitamateAnalysisEntity.pending(
                VITAMATE_BLOCK_ID,
                REQUESTED_BY,
                idempotencyKey,
                REQUEST_HASH,
                "analysis prompt",
                REQUESTED_AT
        ));
        return saved.getId();
    }

    private VitamateAnalysisEntity reload(Long analysisId) {
        return analysisRepository.findById(analysisId).orElseThrow();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBeans {

        @Bean
        VitamateAnalysisStorePort vitamateAnalysisStorePort(VitamateAnalysisJpaRepository repository) {
            return new TestVitamateAnalysisStore(repository);
        }
    }

    private static class TestVitamateAnalysisStore implements VitamateAnalysisStorePort {

        private final VitamateAnalysisJpaRepository repository;

        private TestVitamateAnalysisStore(VitamateAnalysisJpaRepository repository) {
            this.repository = repository;
        }

        // PENDING 행을 실제 JPQL UPDATE로 PROCESSING 상태로 바꾼다.
        @Override
        public boolean markProcessing(
                Long analysisId,
                String attemptId,
                LocalDateTime startedAt,
                LocalDateTime leaseExpiresAt
        ) {
            return repository.markProcessing(
                    analysisId,
                    AnalysisStatus.PENDING,
                    AnalysisStatus.PROCESSING,
                    attemptId,
                    startedAt,
                    leaseExpiresAt
            ) == 1;
        }

        // PROCESSING 행을 실제 JPQL UPDATE로 FAILED 상태로 마감한다.
        @Override
        public boolean markFailedFromProcessing(
                Long analysisId,
                String attemptId,
                String errorMessage,
                LocalDateTime failedAt
        ) {
            return repository.markFailedFromProcessing(
                    analysisId,
                    AnalysisStatus.PROCESSING,
                    AnalysisStatus.FAILED,
                    attemptId,
                    errorMessage,
                    failedAt
            ) == 1;
        }

        @Override
        public Optional<ExistingAnalysis> findExistingAnalysis(
                Long vitamateBlockId,
                String requestedBy,
                String idempotencyKey
        ) {
            throw unused();
        }

        @Override
        public CreateVitamateAnalysisResult savePendingAnalysis(NewAnalysis analysis) {
            throw unused();
        }

        @Override
        public void saveAnalysisDocuments(Long analysisId, List<Long> fileVersionIds) {
            throw unused();
        }

        @Override
        public boolean markCompleted(Long analysisId, String attemptId, String result, LocalDateTime completedAt) {
            throw unused();
        }

        @Override
        public boolean markFailedFromPending(Long analysisId, String errorMessage, LocalDateTime failedAt) {
            throw unused();
        }

        @Override
        public Optional<String> findAnalysisStatus(Long analysisId) {
            throw unused();
        }

        @Override
        public boolean existsAllCitationTargets(Long analysisId, List<NewCitation> citations) {
            throw unused();
        }

        @Override
        public void saveAnalysisCitations(Long analysisId, List<NewCitation> citations) {
            throw unused();
        }

        private UnsupportedOperationException unused() {
            return new UnsupportedOperationException("이 테스트에서 사용하지 않는 포트 메서드입니다.");
        }
    }
}
