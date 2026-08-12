# 🧩 블록 정보 — 카탈로그 (**enum 10값 / 실사용 8종**)

**최종 업데이트**
- 2026-08-12 — §8 예외 1건 등재: 결재 **상신 이후 직접 삭제 차단**(`DEL-016`). 스텝 삭제 cascade 는 현행 유지 — 잠금 부활이 아니다
- 2026-08-10 — 결재 삭제 경계 정합화·1차 구현: 상태 무관 동기 전파 후 미종결 상태 `CANCELED`, 결재 4테이블 soft delete, 삭제분 명령/조회 차단
- 2026-08-10 — ⭐ **정산 재설계 반영 + 등록표 전수 재확인.** `SETTLEMENT` 등재(`settlement_block`) · `PAYMENT_CONFIRM`·`TAX_INVOICE_VIEW` 는 **상세 테이블 DROP** 으로 빈 껍데기가 됐다 · `IMAGE`·`APPROVAL` 어댑터 실재 확인
- 2026-08-05 — ⛔ **`PERFORMANCE_VIEW` 폐기** — 상세 테이블이 없는 채 T2 미결이던 타입을 enum 에서 제거(10종 → **9종**). `MEMO` 폐기(2026-08-03)와 같은 처리 · DB 는 `V20260805170000` 로 ALTER

> 🔴 **DDL 정본은 [`../domain/ERD.md`](../domain/ERD.md) §3 이다.** 어긋나면 그쪽이 이긴다.

## 1. 기본 규칙 <sub>(구 DOMAIN §3-1)</sub>

| 항목 | 확정 |
|------|------|
| 소유자 | **전부 스텝.** 예외 없음 |
| 타입 | **닫힌 enum 10종.** 확장형 JSON 스키마 금지 |
| 테이블 | `block` 공통 테이블 + **타입별 상세 테이블** |
| ⭐ **다형성 방향** | **양방향 ID · 양쪽 다 FK 없음.** `block.type`(판별자) + `block.type_id`(상세 PK) ↔ `{상세}.block_id`(역방향) |
| 공통 컬럼 | `step_id`, `type`, **`type_id`**, `title`, `owner`, 배치 3종, `created_by`, 생성/수정/삭제 시각 |
| **블록 상태** | **없다.** `block.status` 를 만들지 마라 — 진행 상태는 연결된 이슈가 표현한다 (§6) |

⛔ **`block.project_id` 는 없다** (2026-08-03 폐기). 프로젝트를 알아야 하면 **`step` 을 조인한다** —
`idx_step_project` + `idx_block_step` 으로 커버된다. 정산 회차 집계는 `block_payment_confirm.project_id` 를 그대로 쓴다.

### ⭐ 다형성 규약 5줄

1. **판별자는 `block.type`** — 이 값으로 어느 상세 테이블인지 고른다
2. **`block.type_id`** = 상세 행 PK · `BIGINT NULL` · ⛔ **FK 없음** (타입마다 대상 테이블이 다르다)
3. **`{상세}.block_id`** = 역방향 · ⛔ **FK 없음** · ✅ **`UNIQUE(block_id)` 는 반드시 건다**
4. **다중 항목은 상세 *아래* 자식 테이블로 내린다** — `block → image_block → image`
5. **생성은 3단계 한 트랜잭션** — ① `block` INSERT(`type_id=NULL`) → ② `{상세}` INSERT(`block_id`) → ③ `block` UPDATE `type_id`

> ⚠️ **정합성은 전적으로 앱 책임이다.** FK 가 없어 DB 가 아무것도 막지 않는다.
> 그래서 생성·삭제를 **한 트랜잭션에 묶는다** — 고아 행을 만들지 않는 것이 회수하는 것보다 싸다 (§1 규약 5 보강).
>
> ⛔ **MyBatis `<discriminator>` 로 10종을 한 쿼리에 담는 방식은 폐기했다 (2026-08-05).**
> ① 1:N 타입(`CHECKLIST`·`FILE`·`IMAGE`)은 `<collection>` 이 필요해 **N+1** 이 된다 — "한 방" 이 애초에 성립하지 않는다
> ② 미구현 타입(`bid_notice_block` 테이블 없음) 하나가 **조회 전체를 500** 낸다
> ③ 담당자 5명이 **한 XML 을 동시 편집**한다
> → 대신 **타입별 어댑터가 각자 배치 조회**한다. 금지선은 *타입 수* 가 아니라 **블록 개수에 비례하는 쿼리(N+1)** 다 (BLK-006).

### ⭐ 규약 5 보강 (2026-08-05 확정)

| 항목 | 확정 |
|------|------|
| **순서 고정** | ⛔ **뒤집을 수 없다.** 상세 테이블은 전부 `block_id NOT NULL` 이고 `block.type_id` 만 `NULL` 허용이다 — 스키마가 순서를 강제한다. 상세를 먼저 넣으려고 `block_id` 를 nullable 로 바꾸면 `UNIQUE(block_id)` 가 NULL 중복을 허용해 **고아 행과 생성 중 상태를 구분할 수 없게 된다** |
| **②는 빈 행이다** | 내용은 **타입별 수정 API 가 나중에** 채운다. `TEXT` → `content = NULL` · `CHECKLIST` → `checklist_block` 만 만들고 항목 0개. 그래서 타입별 수정 API 는 **행이 이미 있다고 전제**한다(없으면 404) |
| **삭제도 같은 트랜잭션** | ⛔ **이벤트로 처리하지 않는다.** 상세는 독립 생명주기가 없고(`block_id NOT NULL`), 삭제 판정 주인이 `block.deleted_at`(BLK-007)이다. 이벤트가 유실되면 상세 행을 **회수할 주체가 없다** — 결과적 일관성이 아니라 그냥 유실이다 |
| **주인** | 상세 행 **생성·삭제는 Block 도메인 소관.** 타입 도메인은 **내용 수정만** 담당한다 |
| **확장 방식** | Block 이 포트를 소유하고(소비자 소유 원칙) 타입별 어댑터가 생성·삭제·조회를 구현한다. **타입 추가 = 어댑터 파일 추가**이며 공용 파일을 고치지 않는다 (여러 담당자가 한 XML/클래스를 동시 편집하는 상황을 만들지 않는다) |

**닫힌 enum 으로 가는 이유:**
1. 도메인 블록(입금확인·결재)은 JSON 에 안 들어간다. 매칭·상태머신·권한 제약이 붙는다.
2. "확장 가능"은 환상이다. 새 타입을 넣어도 **프론트에 렌더러가 없으면 못 쓴다** → 결국 매번 배포한다.


## 2. ⭐ 타입별 상세 확장 가이드 (담당자용) <sub>2026-08-05 신설</sub>

> 자기 타입의 상세를 블록 조회·생성·삭제에 물릴 때 읽어라. **Block 도메인 파일은 고치지 않는다.**

### 2-1. 소유 경계

```text
┌─ project/block ─────────────────────── Block 도메인 소관 (고치지 마세요) ─┐
│  application/port/BlockDetailPort.java          ★ 계약                   │
│  application/result/BlockDetail.java            ★ 마커 인터페이스        │
│  application/result/{Xxx}Detail.java               ↑ 타입별 응답 shape    │
│  application/service/BlockDetailRegistry.java   타입 → 어댑터 매칭        │
│  application/service/BlockCommandService.java   생성 3단계 · 삭제         │
│  application/service/BlockQueryService.java     타입별 배치 조회          │
└────────────────────────────┬─────────────────────────────────────────────┘
                             │ implements (스프링이 List<BlockDetailPort> 로 주입)
   ┌─────────────────────────┼─────────────────────────┐
   ↓                         ↓                         ↓
{도메인}/infrastructure/blockdetail/          ← 담당자 소관
   {Xxx}BlockDetailAdapter.java   포트 구현 · 자기 도메인 서비스에 위임
   {Xxx}DetailMapper.java         MyBatis 인터페이스 (조회 전용)
   {Xxx}DetailRow.java            SELECT 결과 매핑 record
resources/mapper/{도메인}/{Xxx}DetailMapper.xml   ← 담당자 소관
```

**패키지를 담당자 쪽에 두는 이유**: 상세 테이블 스키마가 바뀌면 그 SQL 을 고칠 사람이 테이블 주인이다.
Block 도메인이 남의 컬럼명을 들고 있으면 **남의 스키마 변경이 Block 도메인을 깨뜨린다.**
어댑터 위치는 자유다 — `@Mapper` 스캔과 `mapper-locations: classpath:mapper/**/*.xml` 가 위치에 무관하다.

### 2-2. ⭐ 쓰기는 JPA 위임 · 조회만 MyBatis

`application.yml` 의 팀 규약(*"쓰기는 JPA, 목록 조회는 MyBatis"*)을 그대로 따른다.

| 동작 | 수단 | 이유 |
|------|------|------|
| `createDetail` (INSERT) | **자기 도메인 서비스에 위임 → JPA `save()`** | ① IDENTITY 라 `save()` 가 PK 를 채워 돌려준다 → **PK 되찾기 SELECT 가 없다** ② 기본값·`@CreationTimestamp` 가 엔티티에 이미 있다 ③ NOT NULL 컬럼 누락이 SQL 문자열이 아니라 매핑에서 잡힌다 |
| `deleteDetail` (UPDATE) | **자기 도메인 서비스에 위임** | 멱등 판정·자식 캐스케이드가 그쪽에 있다. 복제하면 로직이 갈라진다 |
| `loadDetails` (SELECT) | **MyBatis XML** | 화면 모양이 도메인 모양과 다르다(집계·부분 필드·1:N 평탄화). JPA 로 하면 `@OneToMany` 나 N+1 이 된다 |

⛔ **어댑터에서 `INSERT`/`UPDATE` SQL 을 직접 쓰지 마라.** 남의 테이블에 내 SQL 을 심는 것이고,
도메인 규칙(기본값·불변식·타임스탬프)이 엔티티와 XML 두 곳으로 갈라진다.
어댑터의 역할은 **"상대 도메인의 언어로 번역"** 이다.

> **누가 부르는가 ≠ 누가 쓰는가.** 만들 **시점** 판단은 Block 도메인이, 실제 **INSERT** 는 타입 도메인이 한다.

### 2-3. 담당자가 만들 파일 4개 + 요청 1건

| # | 파일 | 역할 |
|:-:|------|------|
| 1 | `{도메인}/infrastructure/blockdetail/{Xxx}BlockDetailAdapter.java` | `BlockDetailPort` 구현. `@Component` |
| 2 | `{도메인}/infrastructure/blockdetail/{Xxx}DetailMapper.java` | MyBatis 인터페이스. `@Mapper`. **조회만** |
| 3 | `{도메인}/infrastructure/blockdetail/{Xxx}DetailRow.java` | SELECT 결과 record |
| 4 | `resources/mapper/{도메인}/{Xxx}DetailMapper.xml` | SQL. **namespace = 1번 인터페이스 FQN** |
| 5 | 자기 도메인 리포지토리·서비스에 **`Long create(Long blockId)`** 추가 | 상세 빈 행 INSERT (JPA) |

**Block 담당자(동훈)에게 요청할 것**: `{Xxx}Detail` record 를 `block/application/result/` 에 추가 + API 명세에
`detail` shape 등록. **필드 목록만 주면 된다.** (record 를 담당자 패키지로 넘기지 않는 이유 — 그건 스키마가 아니라
**FE 계약**이고, 10개 패키지로 흩어지면 프론트가 물어볼 곳이 없어진다)

### 2-4. 스키마 요구사항

| 요구 | 어기면 |
|------|--------|
| `block_id BIGINT NOT NULL` (⛔ FK 없음) | 역방향 참조가 성립하지 않는다 |
| `block_id` 에 **`UNIQUE`** | 상세 행이 중복돼 어느 게 진짜인지 알 수 없다 |
| `deleted_at DATETIME NULL` | 논리 삭제를 못 한다 |
| 내용 컬럼은 **nullable** | 생성 직후 **빈 행**이 정상 상태다 (§1 규약 5 보강) |
| PK 는 단일 `BIGINT AUTO_INCREMENT` | 복합 PK 면 `block.type_id` 로 못 가리킨다 → `createDetail` 이 `null` 반환 (`FILE` 이 이 경로) |

### 2-5. 계약 10건

| # | 지켜야 할 것 | 어기면 |
|:-:|------|--------|
| 1 | `supportedType()` 은 타입당 **1개** | **기동 실패** (레지스트리 duplicate key) |
| 2 | 상세 테이블에 `block_id NOT NULL UNIQUE` | 상세 행 중복 |
| 3 | `createDetail` 은 **빈 행만** 만든다 | 사용자가 안 쓴 내용이 카드에 뜬다 |
| 4 | 상세 PK 를 만들 수 없는 타입은 **`null` 반환** | — (정상 경로. `block.type_id` 가 NULL 로 남고 `detail` 도 `null`) |
| 5 | `createDetail`·`deleteDetail` 은 **JPA 로 위임**. ⛔ INSERT/UPDATE SQL 직접 작성 금지 | §2-2 |
| 6 | 위임받는 서비스에 ⛔ **`REQUIRES_NEW` 금지** | 부분 커밋 → 고아 상세 행 / 죽은 `type_id` |
| 7 | `deleteDetail` 은 **멱등** (0행 UPDATE = 이미 삭제 → 무시) | 재시도 시 예외 |
| 8 | `loadDetails` 는 **`IN` + `<foreach>` 배치 1발** | 블록 20개면 쿼리 20발 (BLK-006 위반) |
| 9 | `loadDetails` 는 **요청받은 PK 전체**에 엔트리를 만든다 (내용 0건이어도) | 갓 만든 빈 블록의 `detail` 이 `null` → FE 가 카드를 못 그린다 |
| 10 | `{Xxx}Detail` record 추가 + **API 명세에 shape 등록** | 프론트가 필드명을 모른다 |

**8·9 가 실수하기 쉽다.** 특히 9번 — 1:N 타입은 쿼리 결과가 아니라 **요청 PK 를 순회**해야 한다.
`ChecklistBlockDetailAdapter.loadDetails` 가 그 참조 구현이다 (항목 0개 블록도 `0/0 · items:[]` 로 내린다).

> 📌 **선택 확장점 `assertDeletable(Long typeId)`** (2026-08-12 신설 · `DEL-016`).
> 상태에 따라 **블록 직접 삭제를 막아야 하는 타입**만 오버라이드한다. 기본 구현이 no-op 이라
> 구현하지 않으면 지금과 똑같이 동작하므로 **기존 어댑터는 손댈 필요가 없다.**
> 참조 구현은 `ApprovalBlockDetailAdapter` 다.
>
> ⛔ **cascade(스텝 삭제)는 이 메서드를 부르지 않는다.** 여기서 막아도 스텝 삭제는 그대로 진행된다 —
> 의도된 설계다(§8). 막고 싶다고 `deleteDetail` 안에서 예외를 던지면 **스텝 삭제 전체가 롤백**되므로
> 그렇게 하지 마라.

### 2-6. 스켈레톤

⚠️ 컬럼명은 자기 스키마에 맞춰라. 아래는 **형식만** 이다.

```java
// {도메인}/infrastructure/blockdetail/XxxBlockDetailAdapter.java
@Component
@RequiredArgsConstructor
public class XxxBlockDetailAdapter implements BlockDetailPort {

    private final XxxDetailMapper xxxDetailMapper;
    private final XxxHandlerService xxxHandlerService;   // 자기 도메인 (JPA)

    @Override
    public BlockType supportedType() {
        return BlockType.XXX;
    }

    /** 빈 행을 만든다. INSERT 는 자기 도메인이 JPA 로 처리하고 PK 를 돌려준다. */
    @Override
    public Long createDetail(Long blockId) {
        return xxxHandlerService.create(blockId);
    }

    /** 블록 삭제와 같은 트랜잭션에서 호출된다. */
    @Override
    public void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt) {
        xxxHandlerService.delete(typeId, userId, blockTitle, deletedAt);
    }

    /** IN 배치 1발. 키는 상세 PK 다. */
    @Override
    public Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds) {
        if (typeIds.isEmpty()) {
            return Map.of();
        }
        return xxxDetailMapper.findByXxxIds(typeIds).stream()
                .collect(Collectors.toMap(XxxDetailRow::xxxId,
                        row -> new XxxDetail(row.xxxId(), row.someField())));
    }
}
```

```java
// 자기 도메인 리포지토리 구현체 — 상세 빈 행 INSERT
@Override
@Transactional
public Long create(Long blockId) {
    // IDENTITY 라 save() 시점에 INSERT 가 나가고 PK 가 채워져 돌아온다 — 되찾기 조회가 필요없다.
    return springDataXxxRepository.save(new XxxJpaEntity(blockId)).getXxxId();
}
```

엔티티에 **`@GeneratedValue(IDENTITY)` · `@CreationTimestamp` · `blockId` 생성자** 3개가 있어야 한다.
읽기 전용으로만 매핑해뒀다면 세 개 다 빠져 있을 수 있다 (`ChecklistBlockJpaEntity` 가 그랬다).

### 2-7. 자주 하는 실수

| 실수 | 증상 |
|------|------|
| XML `namespace` 를 인터페이스 FQN 과 다르게 씀 | 첫 호출에 `BindingException: Invalid bound statement` |
| `@Mapper` 누락 | 주입 실패로 기동 실패 |
| 엔티티에 `@GeneratedValue` 없이 `save()` | PK 가 `null` 인 채 INSERT 시도 → 실패 |
| 엔티티에 `@CreationTimestamp` 없이 `save()` | `created_at NOT NULL` 위반 |
| 어댑터에서 INSERT SQL 직접 작성 | 계약 5 위반. 리뷰 반려 |
| `loadDetails` 에서 블록마다 쿼리 | BLK-006 위반. 리뷰 반려 |
| `${}` 사용 / `SELECT *` | [`MYBATIS.md`](MYBATIS.md) §8 금지 |

### 2-8. 현재 등록 상태

> 🔄 **2026-08-10 전수 재확인** — 아래는 실제 파일 존재를 대조한 결과다 (이전 판은 `IMAGE`·`APPROVAL` 을 미등록으로 적고 있었으나 **둘 다 실재한다**).

| 타입 | 어댑터 | `type_id` | 비고 |
|------|:-----:|:---------:|------|
| `TEXT` | ✅ `text/infrastructure/blockdetail/` | 값 | 참조 구현 (1:1) |
| `CHECKLIST` | ✅ `checklist/infrastructure/blockdetail/` | 값 | 참조 구현 (1:N) |
| `AI` | ✅ `vitamate/infrastructure/blockdetail/` | 값 | 비타메이트 상세 빈 행 생성·삭제 로그 + `VitamateDetail(welcomeMessage)` 조회. 내부 `vitamate_block_id`는 응답에 노출하지 않음 |
| `IMAGE` | ✅ `image/infrastructure/blockdetail/` | 값 | 1:N (`image_block` → `image`) |
| `APPROVAL` | ✅ `approval/infrastructure/blockdetail/` | 값 | **cascade(스텝 삭제)** 는 `IN_PROGRESS` 포함 상태 무관하게 `deleteDetail`을 동기 호출한다. 미종결 상태는 `CANCELED`, 결재 4테이블은 soft delete하며 기존 API에서 삭제분을 차단한다. ⚠️ **직접 삭제는 `assertDeletable`이 상신 이후를 409로 막는다** (2026-08-12 · `DEL-016` · §4-7·§8) |
| ⭐ `SETTLEMENT` | ✅ `settlement/infrastructure/blockdetail/` | 값 | 2026-08-09 신설. `settlement_block` |
| `FILE` | ❌ | **NULL** | 복합 PK — `createDetail` 이 `null` 반환. 🚨 **조회 `detail` 도 안 채워진다** (명세는 `{fileCount}`) |
| ~~`PAYMENT_CONFIRM`~~ · ~~`TAX_INVOICE_VIEW`~~ | ❌ | **NULL** | ⛔ **상세 테이블이 DROP 됐다** (`V20260809130000`) — `SETTLEMENT` 로 통합. enum 값만 남은 빈 껍데기이며 **정리는 Block 도메인 소관** |
| `BID_NOTICE` | — | — | 사용자 생성 금지 (`POST` 에서 400). `bid_notice_block` 테이블 아직 없음 |

> 어댑터가 없어도 **`POST` 는 정상 동작한다** — `type_id` 가 NULL 로 남고 조회에서 `detail: null` 이 된다.
> 500 이 아니다. 담당자가 어댑터만 추가하면 Block 도메인 코드는 **한 줄도 안 바뀐다.**


## 3. 배치 — 3열 그리드 <sub>(구 DOMAIN §3-1)</sub>

블록은 스텝 페이지에 **3열 그리드**로 깔린다. 같은 행의 블록은 높이가 맞춰진다.

| 컬럼 | 뜻 |
|------|-----|
| `row_index` | 몇 번째 행 (0부터) |
| `sort_order` | **그 행 안에서 좌→우 순서** |
| `col_span` | 차지하는 열 수 (1~3) |

**총 열 수는 3으로 고정한다.** 프론트가 12칸으로 짜고 백엔드가 3칸으로 검증하면 그대로 깨진다.

절대 열좌표(`grid_col`) 대신 **행 내 순서**를 쓰는 이유 — 중간에 블록을 끼워 넣을 때 뒤쪽 좌표를 전부 UPDATE 하지 않아도 된다. 드래그 재배치가 싸진다.
⛔ **`UNIQUE(step_id, row_index, sort_order)` 는 걸지 않는다.** 드래그 중간 상태에서 순간적으로 중복이 생긴다.

---

## 4. ⭐ 10종 카탈로그 <sub>(구 DOMAIN §3-2 확장)</sub>

> 계열은 **설명을 위한 구분**이지 상속 계층이 아니다. 소유자는 전부 스텝으로 동일하다.

### 4-1. `TEXT` — 텍스트

| 항목 | 값 |
|------|-----|
| 계열 | 콘텐츠 |
| 상세 테이블 | `text` — PK `txt_id` · `block_id` UNIQUE(FK없음) · `content TEXT` |
| **역할** | 자유 서술. 본문 · 목차 · 회의 메모 · 구조화가 필요 없는 모든 기록 |
| 권한 | 스텝 권한 그대로 (추가 제약 없음) |
| 삭제 | 그냥 soft delete |
| 템플릿 | 담김: **본문** (양식 문구 · 목차) |
| 담당 | 정림 |

> 📌 구조화 저장이 안 되는 항목들이 여기로 몰린다 — 투찰 금액·하한율 검증(UC-10) · 개찰 결과 상세(UC-11) · 송부 방법·수신처(UC-09) · 발주처 피드백(UC-14). **집계가 필요해지면 그때 블록 타입을 만든다.**

### 4-2. `IMAGE` — 이미지

| 항목 | 값 |
|------|-----|
| 계열 | 콘텐츠 |
| 상세 테이블 | `image_block` — PK `img_block_id` · `block_id` UNIQUE(FK없음)<br/>└ 자식 `image` (**1:N**, `img_id` PK + `img_block_id` FK) — `image_url` · `caption` · `order_index` |
| **역할** | 이미지 여러 장을 순서대로. 캡션 있음 |
| 권한 | 스텝 권한 그대로 |
| 삭제 | 그냥 soft delete |
| 템플릿 | 담김: 블록 껍데기만 / 안 담김: **이미지 내용** |
| 담당 | 정림 |

### 4-3. `CHECKLIST` — 체크리스트

| 항목 | 값 |
|------|-----|
| 계열 | 콘텐츠 |
| 상세 테이블 | `checklist_block` — PK `chk_block_id` · `block_id` UNIQUE(FK없음)<br/>└ 자식 `checklist` (**1:N**, `chk_id` PK + `chk_block_id` FK) — `content` · `is_completed` |
| **역할** | **내가 빠뜨리지 않으려는 확인 목록.** 담당자·기한이 없다 (§6) |
| 특수 기능 | 항목마다 **`이슈로 승격`** → 🚨 **구현 불가** — 아래 참조 |
| 권한 | 스텝 권한 그대로 |
| 삭제 | 그냥 soft delete |
| 템플릿 | 담김: **항목 목록** / 안 담김: **체크 상태** |
| 담당 | 정림 |

⛔ **체크리스트에 담당자·기한을 넣지 마라.** 넣는 순간 이슈와 구분이 사라지고, 알림·마감관리·`내 할일` 인프라를 통째로 복제해야 한다 (§6).

> 🚨 **정본 `checklist` 에 `issue_id`·`sort_order`·`checked_by` 가 없다** ([`../domain/ERD.md`](../domain/ERD.md) §3).
> 그래서 **`이슈로 승격` → 자동 체크 · 항목 정렬 · 체크한 사람 표시가 전부 구현 불가**다.
> 컬럼을 추가할지 기능을 접을지 결정해야 한다 — **담당: 정림** → [`../domain/HANDOFF.md`](../domain/HANDOFF.md)

### 4-4. `FILE` — 문서 업로드

| 항목 | 값 |
|------|-----|
| 계열 | 콘텐츠 |
| 상세 테이블 | `block_file` (**1:N**, 복합 PK `block_id` + `file_id`) — `linked_by` · ⛔ `block_id` FK 없음 · **`block.type_id` 는 NULL** |
| **역할** | **파일은 하나, 버전이 붙는다.** `보고서_최종2.xlsx` 를 죽이는 블록 |
| 소유 구조 ⚠️ | **파일은 프로젝트 소속**(`file.project_id`)이고 블록은 그걸 **참조**한다. 블록을 지워도 파일은 산다 |
| 권한 | 스텝 권한 그대로 |
| **삭제 잠금** | **없음.** 결재가 참조해도 파일 **블록** 삭제는 허용한다. 단 파일 자체의 휴지통 이동·영구삭제 정책은 블록 삭제와 별개이며 `.ai/api/file.md`를 따른다 |
| 템플릿 | 담김: 블록 껍데기만 / 안 담김: **업로드된 파일** |
| 담당 | 김동현 |

⚠️ **결재와의 인터페이스** — 결재 블록이 이 블록을 지목하고 `approval_document.file_version_id` 로 **그 시점 버전을 고정**한다.
**`file_id`(파일)가 아니라 `file_version_id`(버전)를 박는다.** 파일 블록 담당이 이 조회 인터페이스를 제공한다 (`SCREEN.md` §6-3).

⛔ **템플릿에 파일을 담지 마라.** 프로젝트 100개에 같은 파일이 100벌 생긴다.

### 4-5. `PAYMENT_CONFIRM` — 입금확인

| 항목 | 값 |
|------|-----|
| 계열 | 도메인 |
| 상세 테이블 | `block_payment_confirm` — PK `payment_block_id` · `block_id` UNIQUE(FK없음) · `project_id` · `round_no` ⚠️ 입금 연결은 **1:N** 이라 `payment.block_id` 가 갖는다 → [`PAY-V1.md`](PAY-V1.md) §5-4 |
| **역할** | **정산 회차 그 자체다.** 별도 회차 레코드를 두지 않는다. **블록 제목이 회차명**(`1차 정산(선급 60%)`) |
| **카디널리티** | **블록 1 : 입금 N** (분할 입금). 반대로 **입금 1 : 블록 1** — 같은 돈이 두 회차에 잡히면 안 된다 |
| **⚠️ 스텝당 1개** | 한 스텝에 이 블록은 **하나만.** 둘이면 같은 스텝의 세금계산서 블록이 어느 회차 것인지 알 수 없다 |
| 회차 속성 | `round_no`(프로젝트 단위 유일) · **입금 예정일** · **예정금액**(둘 다 선택 입력) |
| **권한** ⚠️ | **프로젝트 쪽은 전원 읽기 전용.** 확인 표시 확정·연결은 **`page_code='FINANCE'` + `EDITOR`** 만 |
| **삭제 잠금** | **없음(폐기).** `PAYMENT_CONFIRM` 상세 모델은 `SETTLEMENT`로 통합됐다 |
| 템플릿 | 담김: 블록 + **제목**(`2차 정산`) / 안 담김: **연결된 입금** |
| 담당 | **동훈** (2026-08-01 정현 → 동훈) |
| 상세 문서 | [`PAY-V1.md`](PAY-V1.md) |

**매칭은 2단이다** — ① 프로젝트 매칭(필수, 블록 없어도 됨) ② 블록 연결(선택). **돈은 블록보다 먼저 들어올 수 있다** (`DOMAIN §7-4`).

### 4-6. `TAX_INVOICE_VIEW` — 세금계산서 조회

| 항목 | 값 |
|------|-----|
| 계열 | 도메인 |
| 상세 테이블 | `tax_invoice_confirm` — PK `tax_invoice_block_id` · `block_id` UNIQUE(FK없음) · `tax_invoice_id` · `linked_by` |
| **역할** | 홈택스에서 발행돼 수집된 세금계산서를 **프로젝트 회차에서 조회**한다 |
| **권한** ⚠️ | 연결·해제는 **`page_code='FINANCE'` + `EDITOR`.** 프로젝트 쪽은 읽기 전용 |
| **삭제 잠금** | **없음(폐기).** `TAX_INVOICE_VIEW` 상세 모델은 `SETTLEMENT`로 통합됐다 |
| 템플릿 | 담김: 블록 껍데기만 |
| 담당 | 동훈 |
| 상세 문서 | [`TAX-V1.md`](TAX-V1.md) |

⛔ **어디에도 발행 기능을 넣지 마라.** 타입 이름이 `VIEW` 인 이유다. **발행은 홈택스에서 하고** 이 시스템은 CSV·API 로 수집해 조회만 한다.

### 4-7. `APPROVAL` — 결재 상신

| 항목 | 값 |
|------|-----|
| 계열 | 프로세스 |
| 상세 테이블 | `approval` — PK `approval_id` · `block_id` **NULL 허용** UNIQUE(FK없음) → `approval_revision` → `approval_line` · `approval_document` (**3층**) |
| **역할** | **상신만 한다.** 승인·반려는 결재관리 탭(P-31)에서 (`DOMAIN §12-1-1`) |
| 대상 지정 ⚠️ | 결재 블록에서 공용 파일 업로드 API로 업로드한 `file_version_id`를 직접 연결한다. 파일 블록 경유 방식은 폐기됐다 |
| 버전 고정 | 상신 시점 `file_version_id` 를 박는다. 새 버전 업로드는 허용하되 `대상보다 새 버전 있음` 경고 배지 |
| 권한 | 상신은 스텝 편집 권한자. 승인·반려는 **결재선에 있는 사람만** (role 무관) |
| **삭제 잠금** ⚠️ | **직접 삭제만 차단** (2026-08-12 · `DEL-016`). `DRAFT`·`CANCELED` 는 허용, **`IN_PROGRESS`·`REJECTED`·`COMPLETED` 는 409 `APPROVAL_ALREADY_SUBMITTED`** (`message` 는 상태별로 다르다). ⛔ **스텝 삭제 cascade 는 예외** — 상태 무관 삭제 후 `deleteDetail()` 로 같은 트랜잭션에 전파(현행 유지). 상세 → **§8** |
| 템플릿 | 담김: **결재선** / 안 담김: 진행 상태 · 대상 지목 |
| 담당 | 이강욱 |

⚠️ **`MASTER` 는 결재와 무관하다.** 최종 결재자는 `approval_line.sequence_no` **최댓값**이고 건마다 달라진다 ([`PERMISSION.md`](PERMISSION.md) §7-2).

### 4-8. `AI` — AI 검토 (비타메이트)

| 항목 | 값 |
|------|-----|
| 계열 | 외부 |
| 상세 테이블 | `vitamate_block` — PK `vitamate_block_id` · `block_id` UNIQUE(FK없음) → `vitamate_analysis` → `vitamate_analysis_document` · `vitamate_analysis_citation` |
| **역할** | 프로젝트 문서를 파이썬 서버로 보내 분석. 사용자가 프롬프트로 분석 기준을 넣는다 |
| 권한 | 스텝 권한 그대로 |
| 삭제 | 그냥 soft delete |
| 템플릿 | 담김: **프롬프트 설정** / 안 담김: 실행 결과 |
| 담당 | 정현 |
| 상세 문서 | 정현 소관 (문서 별도 관리) |

> 공고 탭의 **AI 요약과는 다른 기능**이다. 그건 공고 영역 기능이고 이건 스텝 안의 문서 작업용이다 (UC-03).
> 사용자는 채팅하는 것이 아니라 분석 기준 프롬프트를 입력하고, 시스템은 프로젝트 문서 청크를 검색해 분석 결과와 출처 문장을 남긴다.

### 4-9. `BID_NOTICE` — 입찰 공고 ⭐ **신설 (2026-08-03)**

| 항목 | 값 |
|------|-----|
| 계열 | 도메인 |
| 상세 테이블 | `bid_notice_block` — PK `bid_notice_block_id` · `block_id` UNIQUE(FK없음) · `bid_notice_id` **FK 있음** · `notice_snapshot JSON` · `notice_changed` |
| **역할** | 공고 → 프로젝트 전환 시 자동 생성. **프로젝트 안에서 공고를 보는 창**이다 |
| **생성 주체** | 사용자가 아니라 **전환 API 가 자동 생성**한다 (`BID-V1` CNV-06 · 프로젝트·멤버·스테이지·스텝과 한 트랜잭션) |
| **스냅샷** | 전환 시점 공고 값을 `notice_snapshot JSON` 에 박는다. 재수집이 이 값을 **덮지 않는다** (`BID-V1` INV-01) |
| **변경 감지** | 재수집 결과가 스냅샷과 다르면 `notice_changed = 1`. **반영 여부는 사람이 결정**한다 (INV-02) |
| 권한 | 스텝 권한 그대로. ⛔ **블록에서 `bid_notice` 원본을 수정할 수 없다** (INV-08) |
| 삭제 | 그냥 soft delete (잠금 없음) |
| 템플릿 | 담기지 않는다 — 공고에 종속된 블록이다 |
| 담당 | 정현 |
| 상세 문서 | [`bid.md`](../../api/bid.md) (그 외는 정현 소관 · 별도 관리) |

⛔ **`bid_notice_id` 에 UNIQUE 를 걸지 않는다.** soft delete 라 지운 블록의 행이 재생성을 막는다.
"공고 1건 = 블록 1개" 는 `uk_project_bid_notice` 와 앱(`BID-V1` INV-10)이 지킨다.

⚠️ **`bid_notice_id` 의 FK 는 유지한다.** 이건 다형성 대상이 아니라 **실제 테이블 참조**다 —
`payment.block_id`·`notification.block_id` 와 같은 취급이다 (§1 규약 2·3 은 `block_id` 에만 적용).

입찰 블록은 **조회 전용 진입점**이다. 공고 원본 수정도 프로젝트 스냅샷 반영도 블록이 하지 않는다 —
경로·요청·응답은 [`bid.md`](../../api/bid.md) 가 단일 기준이다.
⛔ **입찰 블록 전용 수정/삭제 API 를 만들지 않는다.** 제목·위치·삭제는 공통 블록 API 정책을 따른다.

🚨 **`bid_notice_block` 테이블이 마이그레이션에 없다 (2026-08-05 확인 · 정현 통보 필요).**
`V202608041109` 는 `block.type` enum 에 `BID_NOTICE` 값만 추가했다. 테이블이 생기기 전까지 이 타입은
`type_id` 가 NULL 로 남고 조회 응답의 `detail` 도 `null` 이다 (§2-8).

---

## 5. 한눈에 보는 요약

| `block.type` | 이름 | 계열 | 상세 테이블 (`block.type_id` 대상 PK) | 자식 (1:N) | 삭제 잠금 | 담당 | 상세 문서 |
|-------------|------|------|-----------|-----------|:---------:|------|----------|
| `TEXT` | 텍스트 | 콘텐츠 | `text` (`txt_id`) | — | — | 정림 | — |
| `IMAGE` | 이미지 | 콘텐츠 | `image_block` (`img_block_id`) | `image` | — | 정림 | — |
| `CHECKLIST` | 체크리스트 | 콘텐츠 | `checklist_block` (`chk_block_id`) | `checklist` | — | 정림 | — |
| `FILE` | 문서 업로드 | 콘텐츠 | `block_file` (⛔ **NULL** · 복합 PK) | — | — | 김동현 | — |
| `PAYMENT_CONFIRM` | **입금확인** | 도메인 | `block_payment_confirm` (`payment_block_id`) | `payment` (N:1) | — (폐기) | **동훈** | [`PAY-V1.md`](PAY-V1.md) |
| `TAX_INVOICE_VIEW` | **세금계산서 조회** | 도메인 | `tax_invoice_confirm` (`tax_invoice_block_id`) — ⭐ **연결 전에는 행이 없어 `type_id` NULL** | — | — (폐기) | **동훈** | [`TAX-V1.md`](TAX-V1.md) |
| `APPROVAL` | **결재 상신** | 프로세스 | `approval` (`approval_id`) | `approval_revision` → … | ⚠️ **직접만** (§4-7) | 이강욱 | [`APR-V1.md`](../domain/결재·알림/APR-V1.md) |
| `AI` | AI 검토 | 외부 | `vitamate_block` (`vitamate_block_id`) | `vitamate_analysis` → … | — | 정현 | 정현 소관 (문서 별도 관리) |
| **`BID_NOTICE`** ⭐ | **입찰 공고** | 도메인 | **`bid_notice_block`** (`bid_notice_block_id`) | — | — | 정현 | 정현 소관 (문서 별도 관리) |

**도메인 계열은 재무·공고 영역 데이터를 읽기만 한다** ([`PERMISSION.md`](PERMISSION.md) §5).

⭐ **`type_id` 가 `NULL` 인 타입은 2종이다**

| 타입 | NULL 인 이유 |
|------|------------|
| `FILE` | `block_file` 이 복합 PK(`block_id`+`file_id`) 라 가리킬 단일 PK 가 없다 |
| **`TAX_INVOICE_VIEW`** ⭐ | `tax_invoice_confirm.tax_invoice_id` 가 **NOT NULL** 이라 **계산서가 연결되기 전에는 행을 만들 수 없다.** TXL-008 의 *"행이 없으면 `WAITING`"* 이 정확히 이 의미다 — **행 존재 자체가 "연결됨" 신호**이므로 컬럼을 nullable 로 바꾸면 그 의미가 깨진다 |

---

## 6. 체크리스트 vs 이슈 — 헷갈리지 마라 <sub>(구 DOMAIN §3-3)</sub>

| | **체크리스트 블록** | **이슈** |
|---|-------------------|---------|
| 목적 | 내가 빠뜨리지 않으려는 확인 | **남에게 시키는 일** |
| 담당자 | **없음** | 필수 |
| 기한 | **없음** | 있음 |
| 알림 | 없음 | 발생 |
| 조회 | 그 블록 안에서만 | 칸반 · 내 이슈 · 대시보드 |
| 예 | `회사소개서 최신본 첨부 ☑` | `제안서 초안 — 김민수 — 8/13` |

**한 줄 규칙: 담당자와 기한이 붙으면 이슈, 안 붙으면 체크리스트.**
체크리스트 항목에서 `이슈로 승격` 버튼으로 이슈를 만들 수 있고, 그 이슈가 완료되면 항목도 자동 체크된다.

---

## 7. 이슈 ↔ 블록 연결 ⚠️ <sub>(구 DOMAIN §3-4)</sub>

연결 지점이 **둘**이고, 각각 다른 테이블이 담당한다. 헷갈리면 자동 체크가 안 된다.

| 연결 | 테이블 | 쓰는 곳 |
|------|--------|--------|
| **블록 ↔ 이슈** (N:M, 선택) | `issue_block` ✅ 있다 | 블록 카드 우하단 `완료/전체` 배지 |
| **체크리스트 항목 → 승격된 이슈** (1:1, 선택) | 🚨 **`checklist.issue_id` — 정본에 없다** | 이슈 완료 시 **항목 자동 체크** → **구현 불가** |

**`issue_block` 만으로는 자동 체크를 못 한다.** 그건 블록 단위 연결이라 "5번 항목이 이 이슈다"를 표현할 수 없다.
그래서 `checklist.issue_id` 가 따로 필요한데 **확정 ERD 에 그 컬럼이 없다** (§4-3 · 담당 정림).

| 규칙 | 확정 |
|------|------|
| 중복 연결 | `UNIQUE(issue_id, block_id)` (`uk_ib`) 로 막는다 |
| **같은 스텝 제약** | `issue_block` 연결 시 **`block.step_id = issue.step_id` 를 애플리케이션이 검증**한다. 위반 시 400 |
| 블록 삭제 시 | `issue_block` 행은 **유지**. 조회에서 `block.deleted_at IS NULL` 로 거른다 |
| ⛔ **연결 해제 시** | **하드 `DELETE`** — `issue_block` 에 `deleted_at` 이 없다. soft 로 두면 `uk_ib` 를 시체가 점유해 재연결이 막힌다 (§8-1) |

**같은 스텝 제약이 없으면**, 스텝 A 의 블록 카드에는 `2/5` 가 뜨는데 스텝 A 진척률([`PROJECT.md`](PROJECT.md) §6-1)에는 그 5개가 안 들어간다. 사용자 눈에는 그냥 버그다.
**DB 제약으로는 못 걸어서**(두 테이블을 타야 한다) **애플리케이션이 막아야 한다.**

---

## 8. ⛔ 삭제 잠금은 폐기됐다 (2026-08-09)

> 원래 잠금 4종(입금 연결 · 계산서 연결 · 진행 중 결재 · 결재 대상 파일)이 있었고, 그 블록이 든 스텝까지 막았다.
> **전부 폐기한다** — BLK-008 · STP-009 · PCB-005·006 · TXL-009·010.

**폐기 이유**: 잠금의 탈출구가 「재무팀에 연결 해제를 요청」뿐이었다. 사용자는 왜 안 지워지는지 모른 채 갇히고,
막상 해제하면 회계 매칭을 되돌리는 셈이 된다. 애초에 이 문서가 *"잠금이 많으면 사용자가 지우지도 못하고 왜 안 되는지도 모른다"* 고
경고했는데, 넷도 이미 많았다.

**대신 이렇게 한다.**

| 대상 | 삭제하면 |
|------|------|
| 입금이 연결된 입금확인 블록 | `detachFinanceLinks=true` 를 요구한다. 없으면 400 `FINANCE_LINK_DETACH_REQUIRED` + 연결 건수. 확인하면 `payment.block_id = NULL` 로 끊고 삭제. **입금 행은 남는다** |
| 계산서가 연결된 조회 블록 | 같은 확인 요구. 확인하면 `tax_invoice_confirm` 행을 **하드 삭제**하고 블록 삭제. 계산서는 재연결 가능해진다 |
| 진행 중인 결재 블록 | **스텝 삭제 cascade** 는 같은 트랜잭션에서 `ApprovalBlockDetailAdapter.deleteDetail()`을 호출한다. 미종결 결재는 `CANCELED`로 종결하고 문서 연결을 포함한 하위 행을 논리 삭제한다. ⚠️ **직접 삭제는 2026-08-12 부터 409 로 막힌다** — 아래 「예외 1건」 참고 |
| 결재 대상 파일 블록 | 그냥 삭제한다 |

**스텝을 지울 때 살리고 싶은 블록은 다른 스텝으로 옮긴다** (STP-013 · BLK-014) — 그게 잠금을 대신하는 탈출구다.

⛔ **`BlockDeleteLockPort` · `BlockDeleteLockRegistry` 도 함께 철거했다.** 어댑터는 하나도 만들어지지 않은 상태였다.
**잠금을 다시 만들지 마라.** 막는 대신 **옮길 수단**을 준다는 것이 이 도메인의 결론이다.

> 📌 **예외 1건 — 결재 상신 이후 직접 삭제 차단 (2026-08-12 · `DEL-016`).**
> 위 결론의 **핵심은 유지된다** — 폐기 이유가 "진행 중인 것을 막는 게 나쁘다"가 아니라
> **"막힌 사람에게 탈출구가 없었다"** 였기 때문이다. 그래서 이 예외는 범위를 좁혀 적용한다.
>
> | 경로 | 잠금 |
> |---|:---:|
> | 블록 **직접** 삭제 (`DELETE /api/v1/blocks/{blockId}`) | ✅ 상신된 결재는 409 |
> | **스텝 삭제 cascade** (STP-013) | ⛔ **적용 안 함** — 상태 무관 삭제 현행 유지 |
>
> 즉 스텝·프로젝트 삭제가 결재 때문에 롤백되는 일은 **없다.** 폐기된 잠금과 결정적으로 다른 점이다.
> `BlockDeleteLockPort` 를 되살리지도 않는다 — 판정은 기존 `BlockDetailPort` 확장점에 붙인다.
>
> ⚠️ 구현 시 함정: 직접 삭제와 cascade 가 `BlockCommandService` 의 private `deleteBlock(block, userId)`
> **본체를 공유한다.** 판정을 그 본체에 넣으면 cascade 도 막혀 스텝 삭제가 죽는다 — 반드시 public
> `deleteBlock(DeleteBlockCommand)` 쪽에만 넣는다.
>
> 근거·상태별 판정: [`../domain/결재·알림/APR-DELETE-DRAFT.md`](../domain/결재·알림/APR-DELETE-DRAFT.md) §11

### 8-1. ⭐ 블록 계열 삭제 방식 — soft 가 전부는 아니다

> **판정의 주인은 언제나 `block.deleted_at` 이다** (BLK-007). 상세의 `deleted_at` 은 상세 행만 따로 정리할 때 쓴다.

| 방식 | 테이블 |
|---|---|
| ✅ **soft** (`deleted_at` 있음) | `block` · `text` · `image_block` · `checklist_block` · `vitamate_block` · `block_payment_confirm` · **`bid_notice_block`** |
| ⛔ **hard `DELETE`** (`deleted_at` 없음) | `block_file` · `tax_invoice_confirm` · `issue_block` |

**hard 3개의 공통점** — 담긴 정보가 없는 **순수 연결 행**이다. `deleted_at` 을 달면 UNIQUE·복합 PK 를 시체가 점유해
**재연결이 `1062` 로 죽는다.** `tax_invoice_confirm` 은 원래 *"행이 없으면 `WAITING`"* (TXL-008) 이라 이 의미였다.
삭제 사실은 `activity_log` 가 갖는다 → [`../domain/ERD.md`](../domain/ERD.md) §0-5.

`approval` 계열은 단순 블록 상세와 다르다. 회차·결재선·첨부 이력이 감사 근거이고,
`approval_document.file_version_id` 참조는 파일 영구삭제 차단의 근거이므로 즉시 DB `CASCADE` 대상으로 단정하지 않는다.
현재 구현과 후속 보완 기준은 [`APR-DELETE-DRAFT.md`](../domain/결재·알림/APR-DELETE-DRAFT.md)를 따른다.

---

## 9. 템플릿에 담기는 것 
**원칙: `구조 + 설정` 은 담고, `실적(instance data)` 은 안 담는다.**

| 블록 | 담긴다 | 안 담긴다 |
|------|--------|----------|
| `TEXT` | **본문** (양식 문구 · 목차) | — |
| **`CHECKLIST`** | **항목 목록** | 체크 상태 |
| `FILE` | 블록 껍데기만 | **업로드된 파일** |
| `IMAGE` | 블록 껍데기만 | 내용 |
| **`APPROVAL`** | **결재선** | 진행 상태 · 대상 지목 |
| `PAYMENT_CONFIRM` | 블록 + 제목(`2차 정산`) | **연결된 입금** |
| `TAX_INVOICE_VIEW` | 블록 껍데기만 | — |
| `AI` | 프롬프트 설정 | 실행 결과 |

**체크리스트 항목과 결재선이 템플릿의 진짜 값어치다.** 껍데기만 복사하면 스텝 이름만 깔리는 셈이다.

> 🚨 **확정 ERD 에 `template` 테이블이 없다** → [`HANDOFF.md`](HANDOFF.md) T1. 이 절은 저장소가 정해진 뒤에 유효해진다.
