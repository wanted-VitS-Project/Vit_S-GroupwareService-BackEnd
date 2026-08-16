package com.group3.vitamins.approval.infrastructure.persistence.mapper;

import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalBlockPermissionRow;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalBlockSummaryRow;
import com.group3.vitamins.project.domain.model.MemberPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 결재 블록 판정 조회를 <b>실제 SQL 실행</b>으로 검증한다.
 *
 * <p>이 매퍼는 리포지토리 다중 호출(블록 조회 2발 + 프로젝트/권한 조회)을 조인으로 접은 것이라,
 * 조인 조건 하나만 잘못 놓여도 <b>컴파일도 되고 예외도 안 나면서 판정만 뒤집힌다.</b>
 * 특히 아래 두 가지는 단위테스트로는 절대 안 잡힌다:
 *
 * <ul>
 *   <li>{@code project_member}/{@code step_permission} 조건을 {@code ON} 이 아니라 {@code WHERE} 로
 *       내리면, 미참여자에게 행이 통째로 사라져 <b>"블록 없음"과 "권한 없음"이 구분되지 않는다</b></li>
 *   <li>{@code deleted_at} 필터를 빠뜨리면 삭제된 블록·스텝·프로젝트가 살아난다</li>
 * </ul>
 *
 * <p>⚠️ <b>{@code @Nested} 로 묶지 마라.</b> 중첩 클래스에는 {@code @MybatisTest(properties = ...)} 의
 * 인라인 프로퍼티가 상속되지 않아 H2 URL·{@code flyway.enabled=false} 가 통째로 날아간다. 그러면
 * 운영 {@code application.yml} 설정으로 컨텍스트가 떠서 Flyway 검증 실패로 죽는다 — 원인이
 * 테스트 코드와 무관해 보여서 찾는 데 오래 걸린다(2026-08-16 확인).
 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:approval-block-access;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/approval/ApprovalBlockAccessMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.group3.vitamins.approval.infrastructure.persistence.mapper")
// ⚠️ encoding 을 명시하지 않으면 스크립트를 플랫폼 기본 charset(윈도우는 MS949)으로 읽어
//    픽스처의 한글이 깨진 채 적재된다. 개발자 OS 에 따라 결과가 갈리므로 못 박는다.
@Sql(scripts = "/sql/approval-block-access.sql", config = @SqlConfig(encoding = "UTF-8"))
@DisplayName("ApprovalBlockAccessMapper — 블록 소속·회사 경계·유효 권한 (실 SQL)")
class ApprovalBlockAccessMapperTest {

    private static final Long COMPANY_1 = 1L;
    private static final Long COMPANY_2 = 2L;

    private static final Long LIVE_BLOCK = 11L;
    private static final Long DELETED_BLOCK = 12L;
    private static final Long BLOCK_ON_DELETED_STEP = 13L;
    private static final Long COMPANY_2_BLOCK = 21L;
    private static final Long BLOCK_ON_DELETED_PROJECT = 31L;
    private static final Long ABSENT_BLOCK = 999L;

    @Autowired
    private ApprovalBlockAccessMapper mapper;

    // ── findBlockSummary — block → step 한 번에 ─────────────────────────

    @Test
    @DisplayName("살아있는 블록은 스텝을 거쳐 projectId 까지 채워서 돌려준다")
    void resolvesProjectThroughStep() {
        ApprovalBlockSummaryRow row = mapper.findBlockSummary(LIVE_BLOCK).orElseThrow();

        assertThat(row.blockId()).isEqualTo(LIVE_BLOCK);
        assertThat(row.stepId()).isEqualTo(1L);
        assertThat(row.projectId()).isEqualTo(1L);
        assertThat(row.blockType()).isEqualTo("APPROVAL");
        assertThat(row.createdBy()).isEqualTo("vitas-1111111");
    }

    @Test
    @DisplayName("삭제된 블록은 없는 것으로 본다")
    void skipsDeletedBlock() {
        assertThat(mapper.findBlockSummary(DELETED_BLOCK)).isEmpty();
    }

    @Test
    @DisplayName("블록이 살아있어도 스텝이 삭제됐으면 없는 것으로 본다")
    void skipsBlockOnDeletedStep() {
        assertThat(mapper.findBlockSummary(BLOCK_ON_DELETED_STEP)).isEmpty();
    }

    @Test
    @DisplayName("없는 블록은 빈 값이다")
    void returnsEmptyForAbsentBlock() {
        assertThat(mapper.findBlockSummary(ABSENT_BLOCK)).isEmpty();
    }

    // ── existsBlockInCompany — 회사(테넌트) 경계 ────────────────────────

    @Test
    @DisplayName("자기 회사 블록은 통과한다")
    void passesOwnCompany() {
        assertThat(mapper.existsBlockInCompany(LIVE_BLOCK, COMPANY_1)).isPresent();
    }

    @Test
    @DisplayName("타 회사 블록은 blockId 를 알아도 빈 값이다")
    void rejectsOtherCompany() {
        assertThat(mapper.existsBlockInCompany(LIVE_BLOCK, COMPANY_2)).isEmpty();
        assertThat(mapper.existsBlockInCompany(COMPANY_2_BLOCK, COMPANY_1)).isEmpty();
    }

    @Test
    @DisplayName("프로젝트가 삭제됐으면 회사가 같아도 빈 값이다")
    void rejectsDeletedProject() {
        assertThat(mapper.existsBlockInCompany(BLOCK_ON_DELETED_PROJECT, COMPANY_1)).isEmpty();
    }

    @Test
    @DisplayName("삭제된 블록·스텝도 빈 값이다")
    void rejectsDeletedBlockOrStep() {
        assertThat(mapper.existsBlockInCompany(DELETED_BLOCK, COMPANY_1)).isEmpty();
        assertThat(mapper.existsBlockInCompany(BLOCK_ON_DELETED_STEP, COMPANY_1)).isEmpty();
    }

    // ── findBlockPermission — 유효 권한 재료 ────────────────────────────

    @Test
    @DisplayName("스텝 오버라이드가 없으면 프로젝트 권한만 나오고 스텝은 null 이다 — 상속 판정용")
    void inheritsProjectPermission() {
        ApprovalBlockPermissionRow row = permissionOf("vitas-editor01").orElseThrow();

        assertThat(row.projectPermission()).isEqualTo(MemberPermission.EDITOR);
        assertThat(row.stepPermission()).isNull();
    }

    @Test
    @DisplayName("스텝 오버라이드가 있으면 두 값이 모두 나온다 — 우선순위 판정은 어댑터 몫")
    void reportsBothWhenOverridden() {
        ApprovalBlockPermissionRow row = permissionOf("vitas-overrid1").orElseThrow();

        assertThat(row.projectPermission()).isEqualTo(MemberPermission.VIEWER);
        assertThat(row.stepPermission()).isEqualTo(MemberPermission.EDITOR);
    }

    @Test
    @DisplayName("하향 오버라이드(NONE)도 그대로 실어 나른다")
    void reportsDowngrade() {
        ApprovalBlockPermissionRow row = permissionOf("vitas-downgrd1").orElseThrow();

        assertThat(row.projectPermission()).isEqualTo(MemberPermission.EDITOR);
        assertThat(row.stepPermission()).isEqualTo(MemberPermission.NONE);
    }

    @Test
    @DisplayName("⭐ 프로젝트 미참여자도 행은 나온다 — 프로젝트 권한이 null 이라 '블록 없음'과 구분된다")
    void returnsRowForNonMember() {
        // 이 테스트가 ON → WHERE 회귀를 잡는 지점이다. WHERE 로 내리면 여기서 빈 값이 나온다.
        ApprovalBlockPermissionRow row = permissionOf("vitas-outside1").orElseThrow();

        assertThat(row.projectPermission()).isNull();
        // 스텝 오버라이드 행은 있지만 프로젝트에 없으므로 어댑터가 NONE 으로 접는다
        assertThat(row.stepPermission()).isEqualTo(MemberPermission.EDITOR);
    }

    @Test
    @DisplayName("프로젝트 권한이 NONE 이면 오버라이드가 EDITOR 여도 두 값이 그대로 나온다")
    void reportsProjectNoneWithOverride() {
        ApprovalBlockPermissionRow row = permissionOf("vitas-prjnone1").orElseThrow();

        assertThat(row.projectPermission()).isEqualTo(MemberPermission.NONE);
        assertThat(row.stepPermission()).isEqualTo(MemberPermission.EDITOR);
    }

    @Test
    @DisplayName("블록이 없거나 삭제됐으면 행 자체가 없다 — 권한 없음과 구분되는 유일한 신호다")
    void returnsEmptyWhenBlockGone() {
        assertThat(mapper.findBlockPermission(ABSENT_BLOCK, "vitas-editor01")).isEmpty();
        assertThat(mapper.findBlockPermission(DELETED_BLOCK, "vitas-editor01")).isEmpty();
        assertThat(mapper.findBlockPermission(BLOCK_ON_DELETED_STEP, "vitas-editor01")).isEmpty();
    }

    private Optional<ApprovalBlockPermissionRow> permissionOf(String userId) {
        return mapper.findBlockPermission(LIVE_BLOCK, userId);
    }
}
