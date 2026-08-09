# 삭제 데이터 정리 컨벤션 — 하드 딜리트 스케줄러 & CASCADE

**최종 업데이트**: 2026-08-08 (§2 참고 구현·시험 적용 예외 추가 / §3 CASCADE 후보 표 제거(판단 기준만 유지) / §4를 실시간 등록 현황 추적에서 원칙 설명으로 전환 — `issue` 시험 적용 근거)
**담당**: 김용준 (PO)

> ✅ **원칙: 프로젝트 전체에 하드 딜리트 스케줄러는 `global`에 하나만 둔다.**
> 도메인은 스케줄러를 새로 만들지 않고, 이 문서가 정하는 SPI에 자기 몫만 등록한다.
> **그리고 모든 soft-delete 대상이 스케줄러 등록 후보인 건 아니다** — 사용자가 독립적으로 삭제하는 것과, 그 삭제를 그냥 따라가야 하는 종속 데이터는 처리 방식이 다르다. §0 판단 기준부터 본다.

---

## 0. 판단 기준 — 스케줄러냐 CASCADE냐

새 테이블에 `deleted_at`(또는 하드 삭제 대상 데이터)이 생겼을 때, 아래 순서로 판단한다.

| 질문 | Yes | No |
|---|---|---|
| ① 사용자가 이 엔티티를 **독립적으로 삭제하는 API**를 갖고 있고, 자기 `deleted_at`(또는 그에 준하는 삭제 시각)을 스스로 갖는가? | → **§2 하드 딜리트 스케줄러** 등록 후보 (정책적 이슈 없으면) | ②로 |
| ② (①이 No) 이 데이터는 특정 부모가 사라지면 같이 사라져야 하는, **부모 없이는 독자적 의미가 없는** 종속 데이터인가? | → **§3 CASCADE** 후보 | ③으로 |
| ③ (②도 No) 감사·재무·법적 보존 요구가 있거나, 그 참조 자체가 비즈니스 규칙(참조 차단)의 근거인가? | → **CASCADE 절대 금지.** `SET NULL` 또는 애플리케이션이 직접 확인(§2-2 UseCase 경유 패턴) | — |

이 표가 문서 전체의 뼈대다. §2·§3은 각각 "①에서 Yes" / "②에서 Yes"인 경우의 구체 규칙이다.

**왜 나누나** — 사용자가 직접 지우고 스스로 `deleted_at`을 갖는 엔티티(예: 프로젝트가 독립적으로 삭제 가능한 것들)는 "보존기간이 지나면 정리한다"는 정책 판단이 필요해서 **스케줄러**가 맞다. 반면 그 엔티티에만 딸려 있고 자체 삭제 API가 없는 데이터(예: 상세 블록의 1:N 자식)는 정책 판단이 필요 없다 — **부모가 사라지는 순간 같이 정리되면 그만**이라 DB의 `CASCADE`가 스케줄러보다 훨씬 싸고 안전하다. 매일 새벽 3시까지 기다렸다가 지울 이유가 없다.

---

## 1. 지금 있는 것 (`global`)

| 파일 | 역할 |
|---|---|
| `global/application/cleanup/port/HardDeleteTarget.java` | 삭제 대상 포트 — 이름·보존기간·삭제실행 3개 |
| `global/application/cleanup/port/HardDeleteOperation.java` | 실제 삭제 동작 (`@FunctionalInterface`, 메서드 레퍼런스로 주입) |
| `global/application/cleanup/DefaultHardDeleteTarget.java` | 도메인이 이름·보존기간·삭제동작만 넘겨 쓰는 범용 구현체 |
| `global/application/cleanup/HardDeleteExecutor.java` | `List<HardDeleteTarget>`를 순회하며 실행. **개별 대상 실패를 격리**해 나머지 실행에 영향 없음 |
| `global/infrastructure/cleanup/HardDeleteScheduler.java` | `@Scheduled(cron = "${cleanup.hard-delete.cron:0 0 3 * * *}", zone = "Asia/Seoul")`로 매일 새벽 3시 `executeAll()` 호출 |

**도메인은 이 5개 파일을 절대 수정하지 않는다.** 새 도메인이 늘어도 이 파일들은 그대로다 — Spring이 `HardDeleteTarget` 타입 빈을 자동으로 모아 `HardDeleteExecutor`에 주입해주기 때문이다.

---

## 2. 하드 딜리트 스케줄러 (판단 기준 ①이 Yes인 경우)

`{domain}/infrastructure/cleanup/{Domain}CleanupConfig.java`에 `@Configuration` + `@Bean`으로 `HardDeleteTarget`을 등록한다.

### 2-1. 단순 케이스 — 리포지토리 메서드 참조

참조 무결성 검증 없이 "보존기간 지난 행을 그냥 지우면 되는" 도메인.

```java
@Configuration
public class ExampleCleanupConfig {

    @Bean
    public HardDeleteTarget exampleHardDeleteTarget(
            SpringDataExampleRepository repository
    ) {
        return new DefaultHardDeleteTarget(
                "example",
                Period.ofMonths(6),
                repository::hardDeleteByDeletedAtBefore   // HardDeleteOperation 시그니처와 일치
        );
    }
}
```

### 2-2. 복잡한 케이스 — UseCase 경유

삭제 전 참조 검증·연관 데이터 정리·외부 스토리지(S3 등) 처리가 필요한 도메인은 리포지토리를 직접 물리지 말고 **도메인이 소유한 UseCase**를 거친다.

```java
@Configuration
public class ExampleCleanupConfig {

    @Bean
    public HardDeleteTarget exampleHardDeleteTarget(
            HardDeleteExampleUseCase hardDeleteExampleUseCase
    ) {
        return new DefaultHardDeleteTarget(
                "example",
                Period.ofYears(1),
                hardDeleteExampleUseCase::hardDeleteBefore
        );
    }
}
```

`HardDeleteExampleUseCase`는 도메인 쪽 `application/usecase`에 인터페이스로 두고, `application/service`의 `ExampleHardDeleteService`가 구현한다 (일반 유스케이스와 동일한 ARCHITECTURE.md §2 계층 규칙).

**2-1 vs 2-2 판단 기준**: 삭제가 순수 SQL `WHERE deleted_at < ?` 한 줄로 끝나면 2-1. 삭제 전에 참조 확인·차단·외부 I/O가 필요하면 2-2 — 특히 **§3-2의 세 번째 조건(타 어그리게이트가 나를 참조하는 경우)은 무조건 2-2**로 가야 한다(순서로 못 풀고 확인이 필요하기 때문).

### 2-3. 등록 시 반드시 지킬 것

| 규칙 | 이유 |
|---|---|
| **`activity_log`는 절대 이 SPI에 등록하지 않는다** | "삭제 사실은 `activity_log`가 갖고, `activity_log` 자체는 지우지 않는다"는 팀 확정 정책([docs/README.md §3](../README.md)). 감사로그를 하드 딜리트 대상으로 넣으면 이 정책을 정면으로 깬다 |
| **FK로 참조당하는 도메인이 있으면 `@Order`를 명시한다** | `HardDeleteExecutor`는 등록 순서를 보장하지 않는다. 자식 테이블을 부모보다 먼저 지워야 하면 `@Order(작은 값 = 먼저 실행)`으로 순서를 못박는다 |
| **보존기간이 정책값이면 상수로, 운영 튜닝값이면 `application.yml`로** | 팀 합의(수용 기준)로 정해진 값이면 코드 상수로 박아 바뀔 때 PR로 드러나게 한다. 운영 중 조정 가능한 값이면 `@Value`/`application.yml`로 뺀다 |
| **하드 삭제 정책 자체가 "미정" 상태면 등록하지 않는다** | `.ai/api/{도메인}.md`에 보존기간·삭제 정책이 아직 팀 합의로 정해지지 않았다면 `CleanupConfig`를 만들지 않는다. (⚠️ 상태 표기 라벨(`📝 초안`/`✅ 확정`) 자체의 유효성은 `AGENTS.md`와 `.coderabbit.yaml`이 서로 다르게 설명하고 있어 팀 확인 필요 — 이 규칙은 라벨이 아니라 **정책이 실제로 합의됐는지**로 판단한다) |
| **`HardDeleteOperation` 구현 내부의 실패 처리는 도메인이 책임진다** | `HardDeleteExecutor`는 대상 단위로만 실패를 격리한다. 외부 스토리지 삭제 실패 시 DB까지 롤백할지, DB는 지우고 실패한 키만 남길지는 도메인의 비즈니스 판단이다 |
| **등록엔 배선 검증 테스트를 같이 쓴다** | 위임이 깨져도 컴파일은 통과한다. `HardDeleteTarget`이 올바른 이름·보존기간·삭제동작으로 만들어지는지 최소한의 단위 테스트로 잡는다 — 참고: `IssueCleanupConfigTest` |

### 2-4. 정책이 미정이어도 배선만 검증하는 시험 적용은 가능하다

보존기간 정책이 아직 안 정해졌어도, 담당자 본인이 SPI 배선이 실제로 동작하는지 확인하고 싶으면 임시로 등록할 수 있다. 위 표의 "정책 미정이면 등록하지 않는다"와 충돌하는 게 아니라 예외다 — 단, 확정 정책으로 오해되지 않게 아래 3가지를 지킨다.

1. 보존기간 상수에 **"시험 적용, 정책 미정"이라고 주석**을 남긴다.
2. PR 설명에 **"정책 미확정 — 배선 검증 목적"**이라고 명시한다.
3. 실제 운영 정책(보존기간이 몇 개월인지 등)은 이 문서에 쌓지 않는다 — 각 도메인 담당자가 팀에 브리핑하고 결정한다(§3 CASCADE도 동일 원칙, §4 참고).

**참고 구현**: `issue/infrastructure/cleanup/IssueCleanupConfig.java` — `issue`는 아직 보존기간 정책이 없어 위 규칙대로 임시값(`Period.ofMonths(6)`)으로 시험 등록한 사례다. 2-1(단순 리포지토리 참조) 패턴의 실제 동작하는 코드가 필요하면 이걸 본다.

---

## 3. CASCADE — 종속 데이터 자동 정리 (판단 기준 ②가 Yes인 경우)

부모(사용자가 직접 삭제하는 엔티티)가 하드 딜리트될 때, **자체 삭제 API가 없는 종속 데이터**까지 자동으로 정리되게 하는 방법. 스케줄러 등록이 필요 없다 — 부모가 지워지는 순간 DB가 알아서 처리한다.

### 3-1. CASCADE를 걸어도 되는 조건 (전부 만족해야 함)

1. 자식이 **부모 없이 존재할 이유가 없다** — 부모의 콘텐츠 그 자체이거나, 순수 연결 행이다.
2. 자식 자체에 **독립적인 감사·재무·법적 보존 요구가 없다.**
3. 자식이 지워져도 **되돌릴 수 없는 정보 손실이 아니다** — "연결돼 있었다"는 사실 외에 잃을 정보가 없다.

### 3-2. CASCADE를 걸면 안 되는 조건 (하나라도 해당하면 금지)

1. **감사로그류** — 정책상 영구 보존해야 하는 데이터(`activity_log` 등).
2. **재무 기록** — 입금·세금계산서 등, 부모가 사라져도 회계적으로 남아야 하는 데이터.
3. **그 FK의 참조 자체가 비즈니스 규칙의 근거인 경우** — 예: 결재가 파일 버전을 참조 중이면 그 파일을 영구 삭제하지 못하게 막는 로직이, 바로 그 FK가 살아있다는 사실에 기대고 있을 수 있다. 이런 FK에 CASCADE를 걸면 "삭제를 막아야 하는 규칙"을 DB가 조용히 무력화시킨다.

### 3-3. 대안 — `SET NULL`

3-2에 걸려서 CASCADE는 안 되지만, 참조 컬럼이 원래 nullable이고 그 테이블이 스냅샷(이름·내용 등)을 이미 별도 컬럼에 갖고 있다면 `ON DELETE SET NULL`로 "로그·기록은 남기되 죽은 참조만 끊는" 절충이 가능하다. 예: `activity_log.block_id`는 nullable이고 `target_name`에 대상 이름 스냅샷이 이미 있어서, `block_id`만 `NULL`로 바뀌어도 로그 내용은 안 죽는다.

### 3-4. Flyway로 반영하는 법

FK 방향을 바꾸는 건 스키마 변경이라 [FLYWAY.md](FLYWAY.md)를 따른다.

- 이미 적용된 `V202608031739__init_schema.sql`은 **수정하지 않는다** — `ALTER TABLE ... DROP FOREIGN KEY fk_xxx, ADD CONSTRAINT fk_xxx FOREIGN KEY (...) REFERENCES ... ON DELETE CASCADE`를 담은 **새 마이그레이션 파일**을 추가한다.
- FK 이름은 기존 이름을 그대로 재사용한다(`DROP` 후 같은 이름으로 `ADD`) — 이름이 바뀌면 어디서 걸린 제약인지 추적하기 어려워진다.
- 스키마 정본인 [ERD.md](../domain/ERD.md)에도 CASCADE 방향 변경을 같이 반영한다(FLYWAY.md §3 "컬럼명·제약은 ERD와 동일하게").

**후보 판단과 실행은 각 도메인 담당자 몫이다.** 이 문서는 §3-1~3-3의 판단 기준만 고정하고, "지금 어떤 FK가 CASCADE 대상인가"는 여기 쌓아두지 않는다 — 기준을 잘못 적용했거나 놓친 케이스가 있으면 그 오류가 이 문서에 박제되는 걸 막기 위해서다. 담당자가 자기 테이블에 §3-1~3-3을 적용해 판단하고, 결과는 팀에 브리핑한 뒤 마이그레이션으로 반영한다.

---

## 4. 등록 현황은 이 문서에 쌓지 않는다

누가 어떤 `HardDeleteTarget`을 등록했는지, 어떤 CASCADE가 실제로 반영됐는지는 이 문서에서 관리하지 않는다 — 도메인이 하나 늘 때마다 이 파일을 고치게 되면 그 자체가 병목이자 충돌 지점이 된다. 등록 현황은 각자 `.ai/local/WORKLOG.md`에 남기거나 팀에 직접 브리핑한다. **이 문서는 "어떻게 등록하는지" 방법만 고정한다.**

`notification`의 `NotificationRetentionScheduler`(`notification/application/service`)만 예외로 남겨둔다 — 이건 등록 현황이 아니라 **설계 결정**이라 여기 남긴다: 물리 삭제가 아니라 `deletedAt`을 세팅하는 소프트 삭제라 이 SPI의 대상과 성격이 달라 편입하지 않기로 했다(2026-08-08).

---

## 5. 변경 이력

| 날짜 | 변경 내용 | 담당 |
|---|---|---|
| 2026-08-08 | 유지보수 부담 제거 — §3-4 CASCADE 후보 표 삭제(판단 기준 §3-1~3-3만 유지, 구체 후보는 담당자가 개별 판단), §4를 "등록된 도메인 수" 추적에서 "현황은 이 문서에 안 쌓는다"는 원칙 설명으로 전환. §2-4 신설(정책 미정이어도 시험 적용 가능한 조건 3가지) + `issue`를 §2-1 참고 구현으로 명시. 등록 시 테스트 작성 규칙 추가 | 김용준 |
| 2026-08-08 | 재구성 — 하드 딜리트 스케줄러 단독 문서에서 CASCADE/`SET NULL` 판단 기준까지 포함하는 문서로 확장(§0 판단 기준 신설, §3 CASCADE 신설) | 김용준 |
| 2026-08-08 | 신설 — CodeBomba(Tsarbomba-Backend LMS) `global.cleanup` 구조를 참고해 `global` 하드 딜리트 SPI 스캐폴드 등록 | 김동현 |
