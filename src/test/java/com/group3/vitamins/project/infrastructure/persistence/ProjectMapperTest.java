package com.group3.vitamins.project.infrastructure.persistence;

import com.group3.vitamins.project.domain.model.Project;
import com.group3.vitamins.project.domain.model.ProjectStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * projectId · companyId · bidNoticeId 는 셋 다 {@code Long} 이라 위치 기반 생성자에서 뒤바뀌어도
 * 컴파일이 통과한다. 서로 다른 값을 넣어 왕복시켜 순서 어긋남을 잡는다.
 */
class ProjectMapperTest {

    private static final Long PROJECT_ID = 1L;
    private static final Long COMPANY_ID = 2L;
    private static final Long BID_NOTICE_ID = 3L;
    private static final int VERSION = 7;

    @Test
    @DisplayName("도메인 → 엔티티 → 도메인 왕복에서 세 식별자가 자기 자리를 지킨다")
    void 왕복_식별자_순서() {
        ProjectJpaEntity entity = ProjectMapper.toEntity(sample());

        assertThat(entity.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(entity.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(entity.getBidNoticeId()).isEqualTo(BID_NOTICE_ID);
        assertThat(entity.getName()).isEqualTo("하수관로 정비");
        assertThat(entity.getVersion()).isEqualTo(VERSION);

        Project restored = ProjectMapper.toDomain(entity);

        assertThat(restored.getProjectId()).isEqualTo(PROJECT_ID);
        assertThat(restored.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(restored.getBidNoticeId()).isEqualTo(BID_NOTICE_ID);
        assertThat(restored.getName()).isEqualTo("하수관로 정비");
        // version 이 낡거나 밀리면 저장 조건이 어긋나 모든 수정이 409 가 된다 (CONCURRENCY.md §6-3)
        assertThat(restored.getVersion()).isEqualTo(VERSION);
    }

    @Test
    @DisplayName("생성 시 회사가 찍히고 ID 는 비어 있다")
    void 생성_스탬핑() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 10, 9, 0);

        Project created = Project.create(BID_NOTICE_ID, "신규 과업", null, "OO시청",
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31), BigDecimal.TEN,
                "vitas-EMP001", now, COMPANY_ID);

        assertThat(created.getProjectId()).isNull();
        assertThat(created.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(created.getBidNoticeId()).isEqualTo(BID_NOTICE_ID);
        assertThat(created.getStatus()).isEqualTo(ProjectStatus.NOT_STARTED);
        // 신규 행은 1 부터다 — DB 의 DEFAULT 1 과 맞아야 첫 수정이 409 가 안 난다
        assertThat(created.getVersion()).isEqualTo(1);
    }

    private Project sample() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 1, 9, 0);
        return Project.restore(PROJECT_ID, COMPANY_ID, BID_NOTICE_ID, "하수관로 정비", "설명",
                ProjectStatus.IN_PROGRESS, "OO시청", new BigDecimal("100000000"),
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31),
                null, null, null, VERSION, "vitas-EMP001", createdAt, createdAt, null);
    }
}
