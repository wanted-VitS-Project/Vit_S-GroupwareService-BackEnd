package com.group3.vitamins.settlement.infrastructure.blockdetail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 3중 파생 테이블을 걷어낸 진행률 집계 쿼리를 <b>실제로 돌려서</b> 검증한다.
 *
 * <p>이 쿼리의 위험은 전부 SQL 안에 있다. 집계 범위를 project_id·type 으로 못 좁히면
 * <b>다른 프로젝트의 금액이 진행률에 섞이는데</b>, 컴파일도 통과하고 예외도 안 난다 — 숫자만 틀린다.
 * 또 {@link SettlementDetailRow} 는 record 생성자 <b>위치</b> 매핑이라, SELECT 컬럼 순서가
 * 한 칸 어긋나도 조용히 엉뚱한 값이 들어간다. 둘 다 여기서 잡는다.
 *
 * <p>픽스처에 {@code block}·{@code step} 테이블이 없다 — 의도한 것이다. project_id 비정규화
 * (V20260816170100) 이후 이 쿼리는 두 테이블을 타면 안 되고, 조인이 되살아나면 여기서 즉시 깨진다.
 */
@MybatisTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:settlement-detail-query;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.hikari.connection-init-sql=",
        "spring.flyway.enabled=false",
        "mybatis.mapper-locations=classpath:mapper/settlement/SettlementDetailMapper.xml"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@MapperScan("com.group3.vitamins.settlement.infrastructure.blockdetail")
// ⚠️ encoding 을 명시하지 않으면 스크립트를 플랫폼 기본 charset(윈도우는 MS949)으로 읽어
//    픽스처의 한글이 깨진 채 적재된다.
@Sql(scripts = "/sql/settlement-detail-query.sql", config = @SqlConfig(encoding = "UTF-8"))
@DisplayName("SettlementDetailMapper — 진행률 집계 조회 (실 SQL)")
class SettlementDetailMapperTest {

    @Autowired
    private SettlementDetailMapper mapper;

    private SettlementDetailRow findOne(Long settleId) {
        List<SettlementDetailRow> rows = mapper.findBySettleIds(List.of(settleId));
        return rows.isEmpty() ? null : rows.get(0);
    }

    @Test
    @DisplayName("요청하지 않은 형제 회차의 금액도 합계에 들어간다")
    void 같은_프로젝트_같은_타입_형제가_합계에_포함된다() {
        SettlementDetailRow row = findOne(1L);

        // settle 1(1000) + settle 2(500). settle 5 는 삭제돼 빠진다.
        assertThat(row.actualAmountSum()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("다른 프로젝트의 금액은 섞이지 않는다")
    void 다른_프로젝트는_합계에서_제외된다() {
        SettlementDetailRow row = findOne(1L);

        // project 200 의 7777 이 새어 들어오면 9277 이 된다.
        assertThat(row.actualAmountSum()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("같은 프로젝트라도 타입이 다르면 섞이지 않는다")
    void 다른_타입은_합계에서_제외된다() {
        SettlementDetailRow income = findOne(1L);
        SettlementDetailRow outcome = findOne(3L);

        assertThat(income.actualAmountSum()).isEqualTo(1500L);
        assertThat(outcome.actualAmountSum()).isEqualTo(9999L);
    }

    @Test
    @DisplayName("삭제된 형제 회차는 합계에서 빠진다")
    void 삭제된_형제는_합계에서_제외된다() {
        SettlementDetailRow row = findOne(1L);

        // settle 5(300)가 살아 있으면 1800 이 된다.
        assertThat(row.actualAmountSum()).isEqualTo(1500L);
    }

    @Test
    @DisplayName("타입이 없는 빈 상세 행은 합계가 null 이다 (어댑터가 0으로 취급)")
    void 타입이_null_이면_합계도_null() {
        SettlementDetailRow row = findOne(6L);

        assertThat(row).isNotNull();
        assertThat(row.type()).isNull();
        assertThat(row.actualAmountSum()).isNull();
    }

    @Test
    @DisplayName("삭제된 정산 블록은 결과에 나오지 않는다")
    void 삭제된_정산_블록은_조회되지_않는다() {
        assertThat(mapper.findBySettleIds(List.of(7L))).isEmpty();
    }

    @Test
    @DisplayName("세금계산서 연결 여부는 삭제되지 않은 계산서만 본다")
    void 삭제된_세금계산서는_연결로_보지_않는다() {
        assertThat(findOne(1L).taxInvoiceLinked()).isTrue();
        assertThat(findOne(2L).taxInvoiceLinked()).isFalse();
    }

    @Test
    @DisplayName("여러 건을 한 번에 요청해도 각자 자기 프로젝트 합계를 받는다")
    void 여러_건_조회시_행마다_자기_프로젝트_합계가_붙는다() {
        List<SettlementDetailRow> rows = mapper.findBySettleIds(List.of(1L, 4L));

        assertThat(rows).hasSize(2);
        assertThat(rows).anySatisfy(row -> {
            assertThat(row.settleId()).isEqualTo(1L);
            assertThat(row.actualAmountSum()).isEqualTo(1500L);
        });
        assertThat(rows).anySatisfy(row -> {
            assertThat(row.settleId()).isEqualTo(4L);
            assertThat(row.actualAmountSum()).isEqualTo(7777L);
        });
    }

    /**
     * record 위치 매핑이 어긋나면 타입이 같은 이웃 컬럼끼리 조용히 뒤바뀐다
     * (traderName/bankName/accountNumber/accountHolder, plannedDate/taxInvoiceDueDate).
     * 픽스처가 이들을 전부 다른 값으로 채워둔 이유다.
     */
    @Test
    @DisplayName("SELECT 컬럼 순서와 record 필드 순서가 맞는다")
    void 필드_위치_매핑이_어긋나지_않는다() {
        SettlementDetailRow row = findOne(1L);

        assertThat(row.settleId()).isEqualTo(1L);
        assertThat(row.roundNo()).isEqualTo(1);
        assertThat(row.type()).isEqualTo("INCOME");
        assertThat(row.status()).isEqualTo("PENDING");
        assertThat(row.totalAmount()).isEqualTo(5000L);
        assertThat(row.plannedAmount()).isEqualTo(1200L);
        assertThat(row.plannedTaxAmount()).isEqualTo(120L);
        assertThat(row.plannedDate()).isEqualTo("2026-08-20");
        assertThat(row.taxInvoiceDueDate()).isEqualTo("2026-08-25");
        assertThat(row.traderName()).isEqualTo("거래처가");
        assertThat(row.bankName()).isEqualTo("국민");
        assertThat(row.accountNumber()).isEqualTo("ENC-1");
        assertThat(row.accountHolder()).isEqualTo("예금주가");
        assertThat(row.actualAmount()).isEqualTo(1000L);
        assertThat(row.version()).isEqualTo(3);
    }
}
