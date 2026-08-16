package com.group3.vitamins.approval.application;

import com.group3.vitamins.approval.application.command.ApproveApprovalLineCommand;
import com.group3.vitamins.approval.application.command.ResubmitApprovalCommand;
import com.group3.vitamins.approval.application.policy.ApprovalDocumentEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineEligibilityPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalLineProcessingPolicy;
import com.group3.vitamins.approval.application.policy.ApprovalRevisionEligibilityPolicy;
import com.group3.vitamins.approval.application.port.BlockCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeCatalogPort;
import com.group3.vitamins.approval.application.port.EmployeeSummary;
import com.group3.vitamins.approval.application.port.FileCatalogPort;
import com.group3.vitamins.approval.application.result.ApprovalResubmissionResult;
import com.group3.vitamins.approval.application.service.ApprovalCommandService;
import com.group3.vitamins.approval.domain.exception.ApprovalErrorCode;
import com.group3.vitamins.approval.domain.model.ApprovalLineStatus;
import com.group3.vitamins.approval.domain.model.ApprovalStatus;
import com.group3.vitamins.approval.infrastructure.catalog.CatalogApprovalAdapter;
import com.group3.vitamins.approval.infrastructure.persistence.ApprovalJpaEntity;
import com.group3.vitamins.approval.infrastructure.persistence.ApprovalLineJpaEntity;
import com.group3.vitamins.approval.infrastructure.persistence.ApprovalRevisionJpaEntity;
import com.group3.vitamins.approval.infrastructure.persistence.SpringDataApprovalLineRepository;
import com.group3.vitamins.approval.infrastructure.persistence.SpringDataApprovalRepository;
import com.group3.vitamins.approval.infrastructure.persistence.SpringDataApprovalRevisionRepository;
import com.group3.vitamins.approval.infrastructure.persistence.mapper.ApprovalQueryMapper;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.notification.domain.event.NotificationRequestedEvent;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * 결재 동시 처리를 <b>실제 동시 요청</b>으로 검증한다 (INV-07 · DEL-006).
 *
 * <p>검증 대상은 두 가지다.
 * <ul>
 *   <li><b>동일 결재선 동시 승인</b> — 한 요청만 성공하고 나머지는 409, 다음 {@code ACTIVE} 는 정확히 1개</li>
 *   <li><b>동일 결재 동시 재상신</b> — 두 응답이 같은 회차를 가리키고, 새 회차 행은 정확히 1개</li>
 * </ul>
 *
 * <p>🚨 <b>H2 로는 이 테스트를 쓸 수 없다.</b> {@code SELECT ... FOR UPDATE} 의 대기·타임아웃 동작이
 * InnoDB 와 달라서, 락이 없어도 통과하거나 있어도 실패할 수 있다. 그래서 이 클래스만 실제 MySQL
 * 컨테이너를 띄운다. <b>Docker 가 없으면 이 테스트는 컨테이너 기동에서 실패한다</b> — CI 에 도커가
 * 없다면 태그로 제외할 것.
 *
 * <p>스키마는 Flyway 가 아니라 엔티티에서 만든다({@code ddl-auto=create}). 여기서 보는 것은
 * 스키마 정합성이 아니라 <b>락의 직렬화 효과</b>이고, 운영 스키마를 쓰면 {@code employee} 등 다른
 * 도메인 픽스처가 통째로 필요해진다.
 *
 * <p>⚠️ 클래스 레벨 {@code NOT_SUPPORTED} 로 테스트 트랜잭션을 끈다. 안 끄면 두 스레드가 각자
 * 트랜잭션을 못 열어 경합 자체가 재현되지 않는다.
 *
 * <p>⚠️ 커넥션 풀이 <b>동시 스레드 수보다 커야 한다.</b> 기본값이 작으면 두 번째 스레드가 락이
 * 아니라 <b>커넥션</b>을 기다려서, 락이 없어도 테스트가 통과해 버린다.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        // ⚠️ create-drop 이 아니라 create 다. 컨테이너가 통째로 폐기되므로 종료 시 DROP 은 불필요한데,
        //    Hibernate 가 이미 끊긴 커넥션으로 DROP 을 시도해 CI 로그가 연결 오류로 도배된다.
        //    실패로 집계되지는 않지만 진짜 장애를 가린다.
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.datasource.hikari.maximum-pool-size=10",
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ApprovalCommandService.class, CatalogApprovalAdapter.class,
        ApprovalRevisionEligibilityPolicy.class, ApprovalLineEligibilityPolicy.class,
        ApprovalLineProcessingPolicy.class, ApprovalDocumentEligibilityPolicy.class,
        ApprovalConcurrencyTest.TestBeans.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("결재 동시 처리")
class ApprovalConcurrencyTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36");

    private static final String DRAFTER = "EMP001";
    private static final String APPROVER_1 = "EMP002";
    private static final String APPROVER_2 = "EMP003";

    /**
     * 락을 <b>실제로 쥔 뒤</b> 첫 요청을 멈춰 세우기 위한 스파이 2종.
     *
     * <p>잠금 조회({@code SELECT ... FOR UPDATE})를 감싸서, 진짜 메서드를 호출해 락을 잡은 다음
     * 테스트가 풀어줄 때까지 그 자리에 세운다. 트랜잭션이 아직 열려 있으므로 <b>행 잠금이 유지된 채로</b>
     * 두 번째 요청을 들여보낼 수 있다.
     *
     * <p>운영 코드에 테스트용 훅을 심지 않으려고 스파이를 쓴다 — 잠금 지점이 이미 정책 클래스의
     * 공개 메서드라 그 경계에서 가로채면 충분하다.
     */
    @MockitoSpyBean private ApprovalLineProcessingPolicy lineProcessingPolicy;
    @MockitoSpyBean private ApprovalRevisionEligibilityPolicy revisionEligibilityPolicy;

    @Autowired private ApprovalCommandService approvalCommandService;
    @Autowired private SpringDataApprovalRepository approvalRepository;
    @Autowired private SpringDataApprovalRevisionRepository revisionRepository;
    @Autowired private SpringDataApprovalLineRepository lineRepository;
    @Autowired private EmployeeCatalogPort employeeCatalogPort;
    @Autowired private BlockCatalogPort blockCatalogPort;
    @Autowired private DomainEventPublisher domainEventPublisher;
    @Autowired private PlatformTransactionManager transactionManager;

    /**
     * 픽스처 전용 트랜잭션.
     *
     * <p>⚠️ 클래스 레벨 {@code NOT_SUPPORTED} 때문에 테스트 메서드엔 트랜잭션이 없다. 그런데
     * {@code markInProgress} 같은 {@code @Modifying} 쿼리는 트랜잭션을 요구하므로
     * ({@code save()} 와 달리 리포지토리 기본 {@code @Transactional} 이 안 붙는다) 여기서 열어준다.
     */
    private TransactionTemplate fixtureTx;

    @BeforeEach
    void stubEmployees() {
        fixtureTx = new TransactionTemplate(transactionManager);
        Mockito.reset(employeeCatalogPort, blockCatalogPort, domainEventPublisher);
        Mockito.when(employeeCatalogPort.findEmployee(anyString()))
                .thenAnswer(invocation -> Optional.of(available(invocation.getArgument(0))));
        // ⚠️ 재상신은 대행 기안자 판정에서 회사(테넌트) 경계를 확인한다. 기본값 false 로 두면
        //    락이 아니라 403 으로 떨어져 "동시성 테스트인데 권한 때문에 실패"하게 된다.
        Mockito.when(blockCatalogPort.isBlockInCompany(Mockito.anyLong(), Mockito.anyLong()))
                .thenReturn(true);
    }

    /** 참여 가능한 사원 — 퇴사·삭제 없고 계정 ACTIVE, ADMIN 아님 */
    private static EmployeeSummary available(String userId) {
        return new EmployeeSummary(userId, userId + "님", "대리", "개발팀", "USER",
                1L, "ACTIVE", null, null);
    }

    @Test
    @DisplayName("같은 결재선에 승인 2건이 동시에 들어와도 한 건만 처리되고 다음 결재선은 1개만 활성화된다")
    void concurrentApproveProcessesOnlyOnce() throws Exception {
        Fixture fixture = submittedApprovalWithTwoLines();

        LockGate gate = new LockGate();
        Mockito.doAnswer(gate::pauseFirstHolder)
                .when(lineProcessingPolicy).getApprovalForLineForUpdateOrThrow(Mockito.anyLong());

        List<Outcome> outcomes = runContending(2, gate, () ->
                approvalCommandService.approve(
                        new ApproveApprovalLineCommand(fixture.firstLineId(), "확인했습니다", APPROVER_1)));

        assertThat(outcomes).hasSize(2);
        assertThat(succeeded(outcomes)).as("성공한 요청 수").isEqualTo(1);

        List<DomainException> conflicts = domainFailures(outcomes);
        assertThat(conflicts).as("실패한 요청").hasSize(1);
        assertThat(conflicts.get(0).getErrorCode())
                .as("중복 처리는 409 ALREADY_PROCESSED 여야 한다")
                .isEqualTo(ApprovalErrorCode.APPROVAL_LINE_ALREADY_PROCESSED);

        List<ApprovalLineJpaEntity> lines = lineRepository
                .findByApprovalRevisionIdAndDeletedAtIsNullOrderBySequenceNo(fixture.revisionId());
        assertThat(lines).extracting(ApprovalLineJpaEntity::getStatus)
                .as("1번 승인 완료, 2번 활성화")
                .containsExactly(ApprovalLineStatus.APPROVED, ApprovalLineStatus.ACTIVE);

        // ⚠️ 최종 결재선 상태만으로는 중복 처리를 못 잡는다 — 두 요청이 모두 성공해도 둘 다
        //    "1번 APPROVED · 2번 ACTIVE" 로 같은 결과를 쓰기 때문이다. 실제 피해는 상태가 아니라
        //    **부수효과 중복**이므로(다음 결재자에게 요청 알림이 두 번), 발행 횟수로 확인한다.
        Mockito.verify(domainEventPublisher, Mockito.times(1))
                .publish(Mockito.any(NotificationRequestedEvent.class));
    }

    @Test
    @DisplayName("같은 결재에 재상신 2건이 동시에 들어와도 새 회차는 1개만 생긴다")
    void concurrentResubmitCreatesSingleRevision() throws Exception {
        Fixture fixture = rejectedApproval();

        LockGate gate = new LockGate();
        Mockito.doAnswer(gate::pauseFirstHolder)
                .when(revisionEligibilityPolicy).getApprovalForUpdateOrThrow(Mockito.anyLong());

        List<Outcome> outcomes = runContending(2, gate, () ->
                approvalCommandService.resubmit(new ResubmitApprovalCommand(fixture.approvalId(), DRAFTER)));

        assertThat(succeeded(outcomes))
                .as("재상신은 멱등이라 둘 다 성공해야 한다 — 실패 내역: %s", describeFailures(outcomes))
                .isEqualTo(2);

        List<Long> revisionIds = outcomes.stream()
                .map(Outcome::result)
                .map(ApprovalResubmissionResult.class::cast)
                .map(result -> result.revision().getRevisionId())
                .distinct()
                .toList();
        assertThat(revisionIds).as("두 응답이 같은 회차를 가리켜야 한다").hasSize(1);

        List<ApprovalRevisionJpaEntity> revisions = revisionRepository
                .findByApprovalIdAndDeletedAtIsNullOrderByRevisionNoAsc(fixture.approvalId());
        assertThat(revisions).as("1차(반려) + 2차(새 DRAFT) 만 있어야 한다").hasSize(2);
        assertThat(revisions.get(1).getStatus()).isEqualTo(ApprovalStatus.DRAFT);
        assertThat(revisions.get(1).getRevisionNo()).isEqualTo(2);
    }

    // ---------------------------------------------------------------- 픽스처

    private record Fixture(Long approvalId, Long revisionId, Long firstLineId) {
    }

    /** 상신 완료 상태 — 1번 ACTIVE, 2번 WAITING */
    private Fixture submittedApprovalWithTwoLines() {
        return fixtureTx.execute(status -> {
            ApprovalJpaEntity approval = approvalRepository.saveAndFlush(
                    ApprovalJpaEntity.createDraft(nextBlockId(), DRAFTER));
            ApprovalRevisionJpaEntity revision = revisionRepository.saveAndFlush(
                    ApprovalRevisionJpaEntity.createDraft(approval.getApprovalId(), 1, "동시 승인 품의", "내용"));
            List<ApprovalLineJpaEntity> lines = lineRepository.saveAllAndFlush(List.of(
                    ApprovalLineJpaEntity.createDraft(revision.getApprovalRevisionId(), APPROVER_1, 1),
                    ApprovalLineJpaEntity.createDraft(revision.getApprovalRevisionId(), APPROVER_2, 2)));

            approvalRepository.markInProgress(approval.getApprovalId(), 1, ApprovalStatus.IN_PROGRESS);
            revisionRepository.markSubmitted(revision.getApprovalRevisionId(), ApprovalStatus.IN_PROGRESS);
            lineRepository.activateFirstAndWaitRest(
                    revision.getApprovalRevisionId(), ApprovalLineStatus.ACTIVE, ApprovalLineStatus.WAITING);

            return new Fixture(approval.getApprovalId(), revision.getApprovalRevisionId(),
                    lines.get(0).getApprovalLineId());
        });
    }

    /** 1번 결재자가 반려한 상태 — 재상신 대상 */
    private Fixture rejectedApproval() {
        Fixture submitted = submittedApprovalWithTwoLines();
        return fixtureTx.execute(status -> {
            lineRepository.markProcessed(submitted.firstLineId(), ApprovalLineStatus.REJECTED, "보완 필요");
            revisionRepository.finalizeRevision(submitted.revisionId(), ApprovalStatus.REJECTED);
            approvalRepository.finalizeApproval(submitted.approvalId(), ApprovalStatus.REJECTED);
            return submitted;
        });
    }

    private long nextBlockId() {
        return System.nanoTime() % 1_000_000L;
    }

    // ---------------------------------------------------------------- 동시 실행

    /** 락을 쥔 요청을 멈춰 둔 채, 나머지가 정말 대기하는지 지켜보는 시간 */
    private static final long CONTENTION_WINDOW_MS = 2_000L;

    /**
     * {@code count} 개 요청을 동시에 넣되, <b>락을 잡은 첫 요청을 그 자리에 세워 두고</b>
     * 나머지가 대기하는지 확인한 뒤 풀어준다.
     *
     * <p><b>왜 단순 동시 출발로는 부족한가</b> — 출발만 맞추면 첫 요청이 먼저 커밋하고 두 번째가
     * 그 뒤에 읽는 순서가 얼마든지 나올 수 있다. 그러면 {@code PESSIMISTIC_WRITE} 를 <b>제거해도</b>
     * 최종 상태가 같아 테스트가 통과한다 — 락 회귀가 CI 를 그대로 지나간다.
     *
     * <p><b>이 방식이 결정적인 이유</b> — {@link LockGate} 가 잠금 조회 <b>직후</b>(트랜잭션이 열려
     * 있고 행 잠금이 유지되는 지점)에서 첫 요청을 멈춘다. 그 상태로 두 번째 요청을 들여보내면
     * <ul>
     *   <li>락이 <b>있으면</b> 두 번째가 잠금 조회에서 블로킹돼 아무 요청도 끝나지 않는다</li>
     *   <li>락이 <b>없으면</b> 두 번째가 그대로 통과해 <b>끝나 버린다</b> → 아래 어설션이 잡는다</li>
     * </ul>
     *
     * <p>⚠️ 예전 주석은 "결정적으로 만들면 서로를 기다리다 멈춘다"며 이 방식을 포기했는데, 그건
     * <b>첫 요청이 두 번째의 도착을 기다리는</b> 설계에만 해당한다. 여기서는 첫 요청을 풀어주는 주체가
     * <b>테스트 스레드</b>라 상호 대기가 성립하지 않는다.
     */
    private List<Outcome> runContending(int count, LockGate gate, Callable<Object> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);

        try {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                futures.add(pool.submit(() -> {
                    try {
                        return new Outcome(task.call(), null);
                    } catch (Exception e) {
                        return new Outcome(null, e);
                    }
                }));
            }

            assertThat(gate.awaitAcquired(30, TimeUnit.SECONDS))
                    .as("첫 요청이 잠금 조회를 통과할 때까지 기다린다").isTrue();

            // ⭐ 락 회귀를 잡는 지점이다. 첫 요청이 락을 쥔 채 멈춰 있는 동안 다른 요청이 완료됐다면
            //    그건 잠금 조회가 아무도 막지 않았다는 뜻이다.
            assertThat(anyCompletedWithin(futures, CONTENTION_WINDOW_MS))
                    .as("락을 쥔 요청이 커밋하기 전에 다른 요청이 끝나면 안 된다 — 끝났다면 잠금이 사라진 것이다")
                    .isFalse();

            gate.release();

            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        } finally {
            // 어설션이 실패해 빠져나가도 붙잡힌 스레드를 풀어준다(이미 열려 있으면 무해).
            gate.release();
            pool.shutdownNow();
        }
    }

    /** 주어진 시간 안에 완료된 요청이 하나라도 있으면 {@code true}. 없으면 끝까지 기다린 뒤 {@code false}. */
    private static boolean anyCompletedWithin(List<Future<Outcome>> futures, long millis)
            throws InterruptedException {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (futures.stream().anyMatch(Future::isDone)) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    /**
     * 잠금 조회를 통과한 <b>첫</b> 요청 하나만 붙잡아 두는 관문.
     *
     * <p>⚠️ 붙잡을 요청을 {@code compareAndSet} 으로 고른다. 락이 사라진 회귀 상황에서는 두 요청이
     * <b>동시에</b> 잠금 조회를 통과하는데, 그때 둘 다 멈춰 세우면 아무도 완료되지 않아
     * <b>회귀인데도 어설션이 통과</b>해 버린다. 한 쪽만 붙잡아야 나머지가 끝나는 것이 신호가 된다.
     */
    private static final class LockGate {

        private final CountDownLatch acquired = new CountDownLatch(1);
        private final CountDownLatch hold = new CountDownLatch(1);
        private final AtomicBoolean holderChosen = new AtomicBoolean();

        /** 진짜 메서드로 락을 잡은 뒤, 첫 요청이면 테스트가 풀어줄 때까지 그 자리에 선다. */
        Object pauseFirstHolder(InvocationOnMock invocation) throws Throwable {
            Object result = invocation.callRealMethod();
            if (holderChosen.compareAndSet(false, true)) {
                acquired.countDown();
                // 여기서 멈춰 있는 동안 트랜잭션이 열려 있으므로 행 잠금이 유지된다.
                hold.await(60, TimeUnit.SECONDS);
            }
            return result;
        }

        boolean awaitAcquired(long timeout, TimeUnit unit) throws InterruptedException {
            return acquired.await(timeout, unit);
        }

        void release() {
            hold.countDown();
        }
    }

    private record Outcome(Object result, Exception failure) {
    }

    private static long succeeded(List<Outcome> outcomes) {
        return outcomes.stream().filter(outcome -> outcome.failure() == null).count();
    }

    /** 실패 원인을 어설션 메시지에 실어 준다 — 없으면 "0건 성공"만 보이고 왜인지는 안 보인다. */
    private static String describeFailures(List<Outcome> outcomes) {
        return outcomes.stream()
                .map(Outcome::failure)
                .filter(java.util.Objects::nonNull)
                .map(failure -> {
                    Throwable root = failure;
                    while (root.getCause() != null && root.getCause() != root) {
                        root = root.getCause();
                    }
                    return failure.getClass().getSimpleName() + "(" + failure.getMessage() + ")"
                            + " root=" + root.getClass().getSimpleName() + "(" + root.getMessage() + ")";
                })
                .toList()
                .toString();
    }

    private static List<DomainException> domainFailures(List<Outcome> outcomes) {
        return outcomes.stream()
                .map(Outcome::failure)
                .filter(DomainException.class::isInstance)
                .map(DomainException.class::cast)
                .toList();
    }

    @TestConfiguration
    static class TestBeans {

        /** 이 테스트가 보는 것은 락이지 사원·블록·파일 정책이 아니다 — 전부 대역으로 둔다. */
        @Bean EmployeeCatalogPort employeeCatalogPort() {
            return Mockito.mock(EmployeeCatalogPort.class);
        }

        @Bean BlockCatalogPort blockCatalogPort() {
            return Mockito.mock(BlockCatalogPort.class);
        }

        @Bean FileCatalogPort fileCatalogPort() {
            return Mockito.mock(FileCatalogPort.class);
        }

        @Bean CurrentCompanyIdProvider currentCompanyIdProvider() {
            return Mockito.mock(CurrentCompanyIdProvider.class);
        }

        @Bean ApprovalQueryMapper approvalQueryMapper() {
            return Mockito.mock(ApprovalQueryMapper.class);
        }

        /**
         * 실제 이벤트 리스너의 동작(알림 저장·SSE 전송)은 이 테스트 범위 밖이라 대역으로 둔다.
         * 다만 <b>발행 횟수는 검증 대상</b>이다 — 승인 테스트가 알림 이벤트가 1회만 나가는지 본다.
         */
        @Bean DomainEventPublisher domainEventPublisher() {
            return Mockito.mock(DomainEventPublisher.class);
        }
    }
}
