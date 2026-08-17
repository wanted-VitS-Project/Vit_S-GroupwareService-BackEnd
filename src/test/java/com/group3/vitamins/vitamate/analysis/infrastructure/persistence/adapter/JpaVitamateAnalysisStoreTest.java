package com.group3.vitamins.vitamate.analysis.infrastructure.persistence.adapter;

import com.group3.vitamins.vitamate.analysis.application.port.VitamateAnalysisStorePort;
import com.group3.vitamins.vitamate.analysis.application.result.CreateVitamateAnalysisResult;
import com.group3.vitamins.vitamate.analysis.domain.model.AnalysisDocumentRole;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.DocumentChunkEntity;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.entity.VitamateAnalysisDocumentEntity;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.DocumentChunkJpaRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.VitamateAnalysisDocumentJpaRepository;
import com.group3.vitamins.vitamate.analysis.infrastructure.persistence.repository.VitamateAnalysisJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JpaVitamateAnalysisStore가 실제 JPA 영속화 계층을 통해 REFERENCE/TARGET 문서 역할과
 * 최종 프롬프트를 정확히 저장·조회하는지 검증한다. 기존 mock 기반 테스트들은 이 계약을
 * 실제로 확인하지 않는다(CodeRabbit 지적 반영).
 */
@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:vitamate-analysis-store;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaVitamateAnalysisStore.class)
@DisplayName("JpaVitamateAnalysisStore 저장 계약")
class JpaVitamateAnalysisStoreTest {

    private static final Long VITAMATE_BLOCK_ID = 1L;
    private static final String REQUESTED_BY = "EMP001";
    private static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 8, 7, 10, 0);

    @Autowired
    private JpaVitamateAnalysisStore store;

    @Autowired
    private VitamateAnalysisJpaRepository analysisRepository;

    @Autowired
    private VitamateAnalysisDocumentJpaRepository documentRepository;

    @Autowired
    private DocumentChunkJpaRepository chunkRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        analysisRepository.deleteAll();
        chunkRepository.deleteAll();
    }

    @Test
    @DisplayName("savePendingAnalysis는 최종 프롬프트를 그대로 저장한다")
    void savePendingAnalysisPersistsPrompt() {
        VitamateAnalysisStorePort.NewAnalysis analysis = new VitamateAnalysisStorePort.NewAnalysis(
                VITAMATE_BLOCK_ID,
                REQUESTED_BY,
                "idem-key-1",
                "0".repeat(64),
                "기준 문서와 다른 금액을 정리해줘.",
                "COST_REPORT",
                "COST_RESULT,COST_OVERVIEW",
                REQUESTED_AT
        );

        CreateVitamateAnalysisResult result = store.savePendingAnalysis(analysis);

        String savedPrompt = analysisRepository.findById(result.analysisId())
                .orElseThrow()
                .getPrompt();
        assertThat(savedPrompt).isEqualTo("기준 문서와 다른 금액을 정리해줘.");
    }

    @Test
    @DisplayName("saveAnalysisDocuments는 REFERENCE와 TARGET 역할을 각각 정확히 저장한다")
    void saveAnalysisDocumentsPersistsBothRoles() {
        Long analysisId = savePendingAnalysis("idem-key-2");

        store.saveAnalysisDocuments(analysisId, List.of(
                new VitamateAnalysisStorePort.NewAnalysisDocument(101L, AnalysisDocumentRole.REFERENCE.name()),
                new VitamateAnalysisStorePort.NewAnalysisDocument(201L, AnalysisDocumentRole.TARGET.name())
        ));

        List<VitamateAnalysisDocumentEntity> saved = documentRepository
                .findByAnalysisIdAndFileVersionIdInAndDeletedAtIsNull(analysisId, List.of(101L, 201L));

        assertThat(saved).hasSize(2);
        assertThat(saved)
                .filteredOn(document -> document.getFileVersionId().equals(101L))
                .singleElement()
                .satisfies(document -> assertThat(document.getDocumentRole()).isEqualTo("REFERENCE"));
        assertThat(saved)
                .filteredOn(document -> document.getFileVersionId().equals(201L))
                .singleElement()
                .satisfies(document -> assertThat(document.getDocumentRole()).isEqualTo("TARGET"));
    }

    @Test
    @DisplayName("existsAllCitationTargets는 선택 문서에 속한 활성 청크를 정확히 참조하면 true를 반환한다")
    void existsAllCitationTargetsReturnsTrueForValidCitations() {
        Long analysisId = savePendingAnalysis("idem-key-citation-1");
        store.saveAnalysisDocuments(analysisId, List.of(
                new VitamateAnalysisStorePort.NewAnalysisDocument(101L, AnalysisDocumentRole.TARGET.name())
        ));
        Long chunkA = saveChunk(101L, 0);
        Long chunkB = saveChunk(101L, 1);

        boolean result = store.existsAllCitationTargets(analysisId, List.of(
                citation(chunkA, 101L),
                citation(chunkB, 101L)
        ));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsAllCitationTargets는 소프트삭제된 청크를 참조하면 false를 반환한다")
    void existsAllCitationTargetsReturnsFalseForDeletedChunk() {
        Long analysisId = savePendingAnalysis("idem-key-citation-2");
        store.saveAnalysisDocuments(analysisId, List.of(
                new VitamateAnalysisStorePort.NewAnalysisDocument(102L, AnalysisDocumentRole.TARGET.name())
        ));
        Long chunkId = saveChunk(102L, 0);
        jdbcTemplate.update(
                "UPDATE document_chunk SET deleted_at = ? WHERE document_chunk_id = ?",
                LocalDateTime.now(), chunkId
        );

        boolean result = store.existsAllCitationTargets(analysisId, List.of(
                citation(chunkId, 102L)
        ));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsAllCitationTargets는 청크가 실제로는 다른 파일버전 소속이면 false를 반환한다")
    void existsAllCitationTargetsReturnsFalseWhenChunkBelongsToDifferentFileVersion() {
        Long analysisId = savePendingAnalysis("idem-key-citation-3");
        store.saveAnalysisDocuments(analysisId, List.of(
                new VitamateAnalysisStorePort.NewAnalysisDocument(103L, AnalysisDocumentRole.TARGET.name()),
                new VitamateAnalysisStorePort.NewAnalysisDocument(104L, AnalysisDocumentRole.TARGET.name())
        ));
        // 청크는 104번 파일버전 소속인데, citation은 103번 소속이라고 주장한다.
        Long chunkId = saveChunk(104L, 0);

        boolean result = store.existsAllCitationTargets(analysisId, List.of(
                citation(chunkId, 103L)
        ));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsAllCitationTargets는 선택하지 않은 파일버전을 참조하면 false를 반환한다")
    void existsAllCitationTargetsReturnsFalseWhenFileVersionNotSelected() {
        Long analysisId = savePendingAnalysis("idem-key-citation-4");
        store.saveAnalysisDocuments(analysisId, List.of(
                new VitamateAnalysisStorePort.NewAnalysisDocument(105L, AnalysisDocumentRole.TARGET.name())
        ));
        Long chunkId = saveChunk(999L, 0);

        boolean result = store.existsAllCitationTargets(analysisId, List.of(
                citation(chunkId, 999L)
        ));

        assertThat(result).isFalse();
    }

    private Long saveChunk(Long fileVersionId, int chunkIndex) {
        DocumentChunkEntity chunk = new DocumentChunkEntity(
                fileVersionId, chunkIndex, 1, "섹션", 0, 80, 30, "테스트 청크", REQUESTED_AT
        );
        return chunkRepository.save(chunk).getId();
    }

    private VitamateAnalysisStorePort.NewCitation citation(Long documentChunkId, Long fileVersionId) {
        return new VitamateAnalysisStorePort.NewCitation(
                documentChunkId, fileVersionId, 1, null, "발췌"
        );
    }

    private Long savePendingAnalysis(String idempotencyKey) {
        VitamateAnalysisStorePort.NewAnalysis analysis = new VitamateAnalysisStorePort.NewAnalysis(
                VITAMATE_BLOCK_ID,
                REQUESTED_BY,
                idempotencyKey,
                "0".repeat(64),
                "검토 요청",
                "COST_REPORT",
                "COST_RESULT",
                REQUESTED_AT
        );
        return store.savePendingAnalysis(analysis).analysisId();
    }
}
