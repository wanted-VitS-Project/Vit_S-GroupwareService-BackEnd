# 🔒 동시수정 정합성 — 낙관적 락 표준

**최종 업데이트**: 2026-08-11 (§9-2 신설 — 스텝 낙관락 구현 · §7 번호 대역 재배정 · §3-3 정정)
**최종 업데이트**: 2026-08-10 (신설 — 동시수정 방어를 **낙관락 단일 정책**으로 확정. Redis 편집 잠금·SSE 사전 차단 폐기)
**담당**: 김동현 (DevOps)
**적용 범위**: 프로젝트 협업 영역 전 도메인 (10개 테이블 + 묶음 4종)
**명세상태**: ✅ **확정** — 구현 착수 가능

> 🔴 이 문서가 동시수정 정합성의 **정본**이다.
> [`../domain/프로젝트/PRJ-V1-REALTIME.md`](../domain/프로젝트/PRJ-V1-REALTIME.md) 의 편집 잠금 정책(§4·§6-3)은 **이 문서로 대체됐다.**

---

## 0. 3줄 요약

1. **동시수정 방어는 낙관적 락 하나로 통일한다.** 편집 잠금(Redis)·SSE 사전 차단은 **폐기**.
2. 충돌은 **저장 시점 409** 로 잡고, 사용자에게 **재조회 / 덮어쓰기**를 묻는다.
3. **체크리스트·관리자 설정 5종은 제외.** 클릭이 곧 저장이거나, 애초에 겹치지 않는다.

---

## 1. 개념 — 낙관적 락이 무엇인가

### 1-1. 한 줄

**"내가 화면에 띄운 이후로 남이 이 행을 바꿨나?"를 저장하는 순간에 검사해서, 바뀌었으면 거부하는 것.**
미리 잠그지 않는다.

### 1-2. 왜 "낙관" 인가

충돌이 **드물 거라고 낙관**하고 미리 막지 않는다. 반대말이 비관적 락(`SELECT ... FOR UPDATE`)이다.

| | 비관적 락 | **낙관적 락** |
|---|---|---|
| 언제 막나 | **읽을 때부터** 행을 잠근다 | 안 막는다. **저장할 때 검사만** |
| 남은 뭘 하나 | 내가 커밋할 때까지 **대기** | 자유롭게 읽고 씀 |
| 충돌 나면 | 애초에 안 남 (기다림) | **나중 요청이 거부됨(409)** → 재조회 후 재시도 |
| 웹에서 쓸 수 있나 | ❌ **못 쓴다** (§1-4) | ✅ 이것뿐이다 |

### 1-3. 메커니즘 — 컬럼 하나와 `WHERE` 하나가 전부다

```sql
-- 1. 조회할 때 version 도 같이 내려준다
SELECT stage_id, name, version FROM stage WHERE stage_id = 8;
-- → version = 7

-- 2. 저장할 때 그 version 을 조건에 건다
UPDATE stage
   SET name = '제안·계약', version = version + 1
 WHERE stage_id = 8 AND version = 7;
```

**`UPDATE` 의 영향 행 수(affected rows)가 판정이다.**

| 결과 | 뜻 |
|---|---|
| `1` | 내가 본 게 최신이었다 → 성공, version 8이 됨 |
| **`0`** | 그 사이 남이 먼저 저장해서 version이 8이 됐다 → **충돌. 409 던지고 롤백** |

⚠️ **"검사 → 저장" 을 두 문장으로 나누면 그 틈에 남이 끼어든다.**
한 문장의 `WHERE` 안에서 검사와 저장이 **원자적으로** 일어나는 것이 이 방식의 전부다.

### 1-4. 🚨 왜 DB 트랜잭션·격리수준으로는 못 막나

이걸 이해해야 낙관락이 왜 필요한지 안다.

```
A: GET /stages     → version 7 받음    ┐
                                        │  ← 사용자가 화면을 보고 이름을 고치는 30초
B: PATCH /stages/8 → version 8 됨       │     이 구간은 트랜잭션이 아니다.
                                        │     DB 커넥션도 잡고 있지 않다.
A: PATCH /stages/8 → 옛 이름으로 저장   ┘
```

**A의 조회와 A의 저장은 서로 다른 HTTP 요청 = 서로 다른 트랜잭션이다.**
그 사이 30초 동안 DB는 A라는 존재조차 모른다.
그래서 `REPEATABLE READ` 든 `SERIALIZABLE` 이든 **아무것도 막지 못한다.**
이 현상을 **lost update(갱신 유실)** 라고 부른다.

비관적 락을 못 쓰는 이유도 같다 — 막으려면 30초 내내 DB 락을 쥐고 있어야 하는데,
**사용자가 자리를 비우면 그 행이 영원히 잠긴다.**

→ 그래서 **"내가 본 버전" 을 클라이언트에게 들려 보냈다가 돌려받는 방식**밖에 없다. 그게 낙관적 락이다.

### 1-5. ⚠️ 검사는 **두 곳**이다

가장 흔한 오해가 *"수정 버튼 누를 때 검사하면 되지 않나"* 다. **그것만으로는 못 막는다.**

| 시각 | A | B | DB |
|---|---|---|---|
| 0초 | 수정 버튼 클릭 → **v1이 최신. 모달 안 뜸** ✅ | | v1 |
| 5초 | (모달에서 30초째 작성 중) | 수정·저장 | **v2** |
| 40초 | **저장** | | 🚨 **B의 작업 덮어씀** |

A는 진입할 때 아무 이상이 없었다. **모달을 볼 기회 자체가 없었다.**

| 시점 | 역할 | 없으면 |
|---|---|---|
| **진입 시** (수정 버튼) | **편의** — 헛수고 방지. *"이미 바뀌었는데 30분 쓸래?"* | 다 쓰고 나서야 충돌을 안다 |
| **저장 시** (`WHERE version = ?`) | **방어 — 필수** | 위 시나리오로 **유실** |

**저장 시 검사가 본체고, 진입 시 검사는 서비스다.** 진입 검사는 선택, 저장 검사는 필수.

---

## 2. 정책 — 무엇에 걸고 무엇에 안 거나

### 2-1. 판단 기준 하나

> **사람이 "결정" 을 내릴 순간이 있는가.**

모달을 띄울 자리가 있으면 낙관락, 클릭이 곧 저장이면 LWW(마지막 저장 승리).

| 구분 | 방식 | 대상 |
|---|---|---|
| **1번** 작업이 잘 안 겹치고, 겹쳐도 위험도 낮음 | 현행 유지 — **LWW** | 체크리스트 체크·항목 · 알림 읽음 · 본인 계정 |
| **2번** 작업이 잘 겹치고, 겹치면 큰일남 | **낙관적 락** | 아래 §2-2 전량 |

### 2-2. ✅ 적용 대상

| # | 도메인 | 엔드포인트 | 테이블 | 담당 |
|:--:|---|---|---|---|
| 1 | 프로젝트 | `PATCH /projects/{id}` · `/status` | `project` | 동훈 |
| 2 | 스테이지 | `PATCH /stages/{id}` | `stage` | 동훈 |
| 3 | 스텝 | `PATCH /steps/{id}` · `/status` | `step` | 동훈 |
| 4 | 블록 | `PATCH /blocks/{id}` · `/step` | `block` | 동훈 |
| 5 | 텍스트 | `PATCH /texts/{txtId}` | `text` | 정림 |
| 6 | 정산 | `PATCH /blocks/settlements/{settleId}/items` | `settlement_block` | 정산 담당 |
| 7 | 파일 | `PATCH /files/{fileId}` | `file` | 김동현 |
| 8 | 결재 | `PATCH /revisions/{id}` · `PUT /lines` | `approval_revision` | 강욱 |
| 9 | 이슈 | `PATCH /issues/{id}` · `/status` | `issue` | 이슈 담당 |

### 2-3. ❌ 제외 대상 — 이유를 남긴다

| 대상 | 제외 이유 |
|---|---|
| **체크리스트 체크·항목** | **클릭이 곧 저장이다.** 여기 충돌 모달을 띄우면 체크할 때마다 팝업이 뜬다. 그리고 **항목 = 레코드**라 다른 항목을 건드리면 충돌이 구조적으로 안 난다 |
| **관리자 설정 5종** (부서·직급·사원·사원그룹·비즈니스카테고리) | 관리자가 혼자 하는 작업이다. 두 관리자가 같은 부서명을 동시에 고칠 일이 실무에서 안 난다. 넣으면 **40파일이 늘고 얻는 게 거의 없다** |
| 알림 읽음 · 비밀번호 변경 · 계정 권한/상태 | 본인 것이거나 관리자 단독 조작 |

> 📌 **덮어써도 복구 경로는 있다.** [`activity_log`](../../api/activity-log.md) 가 `beforeValue`·`afterValue` 를 남긴다.
> 낙관락을 안 건 대상에서 유실이 나도 **로그에서 되찾을 수 있다** — 이게 제외를 허용하는 근거다.

---

## 3. ⭐ 참조 구현 — 스테이지 이름 변경

**전 도메인이 이 모양을 따른다.** 파일 9개.

### 3-0. 지금 코드

```java
// StageCommandService.updateStage — 무조건 덮어쓴다
Stage stage = requireEditableStage(...);
Stage updated = stageRepository.save(stage.rename(command.name()));
```

### 3-1. `db/migration/project/V20260811120000__add_version_project_domain.sql`

**무엇**: 프로젝트 계열 4테이블에 version 추가
**왜**: 기존 행은 전부 1로 시작해야 프론트가 받은 값과 맞물린다

```sql
ALTER TABLE project ADD COLUMN version INT NOT NULL DEFAULT 1;
ALTER TABLE stage   ADD COLUMN version INT NOT NULL DEFAULT 1;
ALTER TABLE step    ADD COLUMN version INT NOT NULL DEFAULT 1;
ALTER TABLE block   ADD COLUMN version INT NOT NULL DEFAULT 1;
```

> ⭐ **테이블당 컬럼 하나뿐이다.** 순서 변경·배치 저장 같은 "목록 통째 전송" API 도
> **별도 묶음 컬럼 없이 이 개별 version 으로 처리한다** (§4).

### 3-2. `project/stage/infrastructure/persistence/StageJpaEntity.java`

**무엇**: 컬럼 매핑 1줄
**왜**: 엔티티에 없으면 매퍼가 값을 못 나른다

```java
@Column(name = "version", nullable = false)
private int version;
```

⚠️ **`@Version` 을 붙이지 마라.** 이유는 §6-1.

### 3-3. `project/stage/domain/model/Stage.java`

**무엇**: version 필드 (읽어온 시점의 값)
**왜**: 도메인이 "몇 번째 상태인지" 를 들고 있어야 **덮어쓰기 기대값**으로 쓸 수 있다

⚠️ **도메인은 version 을 절대 올리지 않는다.** `+1` 은 `WHERE` 와 같은 문장 안에서 **DB 가** 한다.
도메인이 든 version 은 *"내가 읽어온 시점의 값"* 이고, 그게 곧 **저장 조건**이다.
`rename()` 안에서 올리면 `overwrite` 가 쓰는 `stage.getVersion()` 이 DB+1 이 되어 **덮어쓰기가 전부 409** 가 된다.

```java
private final int version;                  // 조회 시점 값. 도메인에서 증가시키지 않는다

public static Stage restore(Long stageId, Long projectId, String name, int sortOrder,
                            int version,                       // ⭐추가
                            LocalDateTime createdAt, LocalDateTime deletedAt) { ... }

public int getVersion() { return version; }
```

> 📌 `rename()` 은 **삭제했다.** `renameIfVersionMatches` 가 이름 변경을 통째로 대체하므로
> 도메인 메서드를 거칠 일이 없다. `moveTo()` 는 순서 변경 결과값 계산에 여전히 쓰인다.

### 3-4. `project/stage/domain/repository/StageRepository.java`

**무엇**: 조건부 UPDATE 포트
**왜**: `save()` 로는 조건을 걸 수 없다 — **여기가 낙관락의 심장이다**

```java
/** 기대 버전과 DB 버전이 같을 때만 이름을 바꾼다. 바뀐 행 수를 돌려준다 (0 = 충돌). */
int renameIfVersionMatches(Long stageId, String name, int expectedVersion);
```

### 3-5. `project/stage/infrastructure/persistence/SpringDataStageRepository.java`

**무엇**: 실제 조건부 UPDATE
**왜**: 검사와 저장이 한 문장 안에서 원자적으로 일어나야 한다

⚠️ **`clearAutomatically`·`flushAutomatically` 를 빼면 조용히 깨진다.**
같은 트랜잭션에서 조회한 엔티티가 영속성 컨텍스트에 남아 있어, UPDATE 후에도 낡은 값을 읽는다.

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update StageJpaEntity s set s.name = :name, s.version = s.version + 1 "
     + "where s.stageId = :stageId and s.version = :expectedVersion and s.deletedAt is null")
int renameIfVersionMatches(@Param("stageId") Long stageId,
                           @Param("name") String name,
                           @Param("expectedVersion") int expectedVersion);
```

### 3-6. `project/stage/infrastructure/persistence/StageRepositoryAdapter.java`

**무엇**: 포트 → SpringData 위임
**왜**: `application` 이 JPA 를 몰라야 한다

```java
@Override
public int renameIfVersionMatches(Long stageId, String name, int expectedVersion) {
    return springDataRepository.renameIfVersionMatches(stageId, name, expectedVersion);
}
```

`StageMapper` 양방향에도 `version` 을 추가한다.

### 3-7. `project/stage/application/service/StageCommandService.java`

**무엇**: 0행이면 409
**왜**: 덮어쓰기 선택 시엔 DB 의 현재 버전을 기대값으로 써서 반드시 통과시킨다

```java
/** 이름만 바꾼다 (STG-001). 내가 본 버전이 아직 최신일 때만 저장된다. */
@Override
public StageResult updateStage(UpdateStageCommand command) {
    Stage stage = requireEditableStage(
            command.stageId(), command.requesterUserId(), command.role());

    int expected = command.overwrite() ? stage.getVersion() : command.version();

    int updated = stageRepository.renameIfVersionMatches(
            command.stageId(), command.name(), expected);

    if (updated == 0) {
        throw new ConflictException(StageErrorCode.STAGE_VERSION_CONFLICT);
    }

    return new StageResult(stage.getStageId(), stage.getProjectId(),
            command.name(), stage.getSortOrder(), expected + 1);
}
```

### 3-8. 나머지 (기계적)

| 파일 | 변경 |
|---|---|
| `StageUpdateRequest` | `Integer version` · `Boolean overwrite` + `toCommand` 전달 |
| `UpdateStageCommand` | `int version` · `boolean overwrite` |
| `StageResult` · `StageUpdateResponse` | 새 `version` 반환 |
| `StageErrorCode` | `STAGE_VERSION_CONFLICT("STAGE_VERSION_CONFLICT", "다른 사용자가 먼저 수정했습니다.")` |
| **`StageQueryService` · `StageSummary`** | 🚨 **조회 응답에 version** — 없으면 프론트가 보낼 값이 없다 (§6-3) |

### 3-9. 동작 확인

| 시각 | A | B | DB |
|---|---|---|---|
| 0초 | 조회 → `version 1` | 조회 → `version 1` | `제안·계약 / v1` |
| 5초 | | 저장 `{version:1}` → `WHERE version=1` **1행** ✅ | `제안 / v2` |
| 10초 | 저장 `{version:1}` → `WHERE version=1` **0행** | | 변화 없음 |
| 10초 | **409 → 모달**: *"먼저 수정됨. 재조회 / 덮어쓰기?"* | | |
| 12초 | 덮어쓰기 → `{overwrite:true}` | | `계약 / v3` |

---

## 4. 목록 통째 전송 API — 항목별 version + 전체 롤백

### 4-1. 대상

요청 하나가 **여러 행**을 확정하는 API 들이다.

| API | 요청 형태 |
|---|---|
| `PATCH /projects/{id}/stages/order` | *"사이드바 **전체**의 최종 순서를 담아 보내는 계약"* (`StageOrderRequest`) |
| `PATCH /projects/{id}/steps/order` | *"보드 **전체**의 최종 배치를 담아 보내는 계약"* (`StepOrderRequest`) |
| `PATCH /steps/{id}/blocks/layout` | 스텝 단위 배치를 통째로 확정 |
| `PATCH /blocks/images/items/{imgBlockId}` | `images[]` 배열 통째 (캡션 여러 개) |

### 4-2. ⭐ 방식 — 별도 컬럼을 만들지 않는다

**요청에 담긴 항목마다 개별 `version` 을 검사하고, 하나라도 0행이면 전체를 롤백한다.**

실구현 (`StageCommandService.reorderStages`):

```java
List<StageOrderResult> results = new ArrayList<>(command.items().size());

for (ReorderStagesCommand.Item item : command.items()) {
    Stage moved = stages.get(item.stageId()).moveTo(item.sortOrder());

    int updated = stageRepository.moveIfVersionMatches(
            moved.getStageId(), moved.getSortOrder(), item.version());

    //ConflictException 은 런타임 예외라 여기서 던지면 앞서 갱신한 항목까지 함께 롤백된다.
    //⚠️ 이 예외를 잡아서 넘기면 순서가 반쯤 바뀐 상태로 커밋된다
    if (updated == 0) {
        throw new ConflictException(StageErrorCode.STAGE_VERSION_CONFLICT);
    }

    results.add(new StageOrderResult(
            moved.getStageId(), moved.getSortOrder(), item.version() + 1));
}
```

동작은 "목록 단위 version" 과 **완전히 같다** — 남이 1건만 건드려도 요청 전체가 409 다.

| | 묶음 version 컬럼 방식 (❌ 폐기) | **항목별 + 전체 롤백** (✅ 채택) |
|---|---|---|
| 새 컬럼 | `project` 2개 · `step` 1개 · `image_block` 1개 | **0개** |
| 새 포트 | 🚨 `stage`·`step` 이 `project` 테이블을 읽어야 함 → **포트+어댑터 2벌** (`BlockCatalogPort` 3벌 복제와 같은 문제) | **0개** |
| 쿼리 수 | 동일 | 동일 — **이미 항목마다 `save()` 를 돌고 있다** (`StageCommandService.reorderStages`) |
| 요청에 **없는** 항목과의 순서 충돌 | 못 잡음 | `validateNoConflictWithUnlisted` 가 **이미 400 으로 잡는다** |

> ⛔ **`stage_order_version`·`step_order_version`·`block_layout_version`·`image_block.version` 을 만들지 마라.**
> 2026-08-10 검토에서 폐기됐다. 기존 코드가 이미 항목 단위로 저장하므로 개별 version 으로 충분하다.

### 4-3. ⚠️ 전체 롤백이 실제로 되는지 확인해야 한다

항목 3개 중 2번째가 0행인데 **1번째가 커밋되면 순서가 반쯤 바뀐 상태로 남는다.**
`ConflictException` 은 런타임 예외라 `@Transactional` 기본 설정에서 롤백되지만,
**서비스가 예외를 삼키거나 `try-catch` 로 감싸면 부분 커밋이 된다.**

### 4-4. 🚨 순서 변경이 왜 가장 위험한가

순서 변경은 **"충돌하면 유실" 이 아니라 "거의 항상 유실"** 이다.

| | 스테이지 **이름** 수정 | 스테이지 **순서** 변경 |
|---|---|---|
| 요청에 담기는 것 | 그 스테이지 **1건** | **전 스테이지 목록** |
| A·B가 **다른** 것을 건드리면 | 충돌 없음 | 🚨 **충돌한다.** A의 요청에 B가 바꾼 스테이지의 **옛 순서가 실려 되돌아간다** |
| 충돌 확률 | 같은 행을 골랐을 때만 | **화면을 연 시점만 겹치면 100%** |

그리고 유실당한 쪽은 **자기가 유실당한 줄도 모른다** — A는 자기가 옮긴 게 정상이라 이상을 못 느끼고,
B는 한참 뒤 새로고침하고 *"내가 옮긴 게 왜 원래대로지?"* 한다. **로그도 에러도 없어 원인 추적이 불가능하다.**

---

## 5. API 계약 — 전 도메인 공통

### 5-1. 조회 응답

**대상 리소스의 조회 응답에 `version` 을 반드시 포함한다.**

```jsonc
{
  "httpStatus": 200,
  "data": {
    "stageId": 8,
    "name": "제안·계약",
    "sortOrder": 2,
    "version": 7          // ⭐필수
  }
}
```

### 5-2. 수정 요청

```jsonc
{
  "name": "제안·계약(수정)",
  "version": 7,           // ⭐필수 — 조회에서 받은 값 그대로
  "overwrite": false      // 선택 — true 면 충돌을 무시하고 덮어쓴다
}
```

### 5-3. 수정 응답 · 에러

| 코드 | code | 언제 | 프론트 처리 |
|---|---|---|---|
| 200 | — | 성공 | 응답의 **새 `version`** 으로 교체 |
| **409** | **`{도메인}_VERSION_CONFLICT`** | 남이 먼저 저장함 | **모달**: 재조회 / 덮어쓰기 |

**에러코드 네이밍**: `STAGE_VERSION_CONFLICT` · `TEXT_VERSION_CONFLICT` · `ISSUE_VERSION_CONFLICT` …
각 도메인 `XxxErrorCode` enum 에 추가한다. ⛔ 공통 코드 하나로 합치지 않는다 — 프론트가 어느 리소스인지 알아야 한다.

### 5-4. 충돌 모달 — 두 선택지

| 선택 | 서버가 하는 일 |
|---|---|
| **재조회** | 해당 리소스만 다시 조회해 최신값으로 화면 교체 |
| **덮어쓰기** | `version` 조건을 DB 현재값으로 바꿔 **반드시 통과시킨다** |

> ⚠️ **덮어쓰기를 열어줄 거면 모달에 "무엇이 달라졌는지" 를 보여줘야 한다.**
> 안 보여주면 사용자는 전부 "덮어쓰기" 를 누른다 — 그럼 version 을 넣은 의미가 없어진다.
> **일단 "재조회" 만으로 시작하고, 덮어쓰기 요구가 실제로 나오면 그때 붙이는 것을 권한다.**

---

## 6. ⛔ 금지 · 조용히 깨지는 지점

### 6-1. JPA `@Version` 을 쓰지 마라

이 프로젝트는 `XxxMapper.toEntity` 가 매번 `new XxxJpaEntity(...)` 로 **detached 객체**를 만든다.
JPA 는 이걸 `merge` 로 처리하면서 **DB 의 최신 version 을 다시 읽어와 검사하므로 항상 통과한다.**

**예외도 안 나고 테스트도 통과하는데 유실만 그대로 남는다.** 수동 `WHERE version = ?` 이어야 한다.

### 6-2. `@Modifying` 에 `clearAutomatically` 를 빼지 마라

영속성 컨텍스트에 낡은 엔티티가 남아 UPDATE 후에도 옛 값을 읽는다.
같은 트랜잭션에서 조회 → UPDATE → 재조회 하는 경로에서 터진다.

### 6-3. 🚨 조회 응답에 `version` 을 빠뜨리지 마라

**이 전체 설계가 무용지물이 되는 유일한 실수다.**

프론트가 보낼 값이 없어서 `version: 0` 이나 `null` 을 보내게 되고,
그러면 `WHERE version = 0` 이라 **모든 저장이 409** 가 된다. **컴파일도 되고 테스트도 통과한다.**

### 6-4. 서비스에서 "조회한 version 과 비교" 만 하지 마라

```java
// ⛔ 이렇게 하면 안 된다
if (stage.getVersion() != command.version()) throw new ConflictException(...);
stageRepository.save(stage.rename(command.name()));    // 이 사이에 남이 끼어든다
```

평소엔 잘 되다가 **부하가 올라가면 샌다.** 재현도 안 된다.

### 6-5. 요약표

| # | 함정 | 증상 |
|:--:|---|---|
| 1 | 조회 응답에 `version` 누락 | **모든 저장이 409.** 컴파일·테스트 다 통과 |
| 2 | JPA `@Version` 사용 | detached merge 라 **항상 통과.** 예외도 안 남 |
| 3 | `clearAutomatically` 누락 | UPDATE 후에도 옛 값을 읽음 |
| 4 | 비교만 하고 `WHERE` 를 안 검 | 조회~저장 사이 레이스 |
| 5 | 목록 전송 API 에서 **부분 커밋** | 순서가 반쯤 바뀐 상태로 남는다 (§4-3) |
| 6 | 클릭=저장 조작(체크박스)에 version | 체크할 때마다 팝업 |
| 7 | MyBatis Row record 에 version 을 **엉뚱한 위치**에 추가 | 위치 기반 매핑이라 **값이 전부 한 칸씩 밀린다.** 예외 없음 (`ProjectDetailRow`) |

---

## 7. 마이그레이션 대상 전량 · 번호 대역

### 7-1. 파일 배치

`locations: classpath:db/migration` 이라 **하위 폴더는 재귀 스캔된다** — 새 폴더를 만들어도 잡힌다.

**전부 `ALTER TABLE {테이블} ADD COLUMN version INT NOT NULL DEFAULT 1;` 한 줄씩이다.** 묶음 컬럼은 없다 (§4-2).

| 파일 | 테이블 |
|---|---|
| `db/migration/project/V20260811120000__add_version_project_domain.sql` | `project` · `stage` · `step` · `block` |
| `db/migration/text/V20260811130000__add_version_text.sql` | `text` |
| `db/migration/image/V20260811140000__add_version_image.sql` | `image` ⚠️ `image_block` 이 아니다 — 캡션은 자식 `image` 행에 있다 |
| `db/migration/settlement/V20260811150000__add_version_settlement.sql` | `settlement_block` |
| `db/migration/approval/V20260811160000__add_version_approval_revision.sql` | `approval_revision` |
| `db/migration/issue/V20260811170000__add_version_issue_file.sql` | `issue` · `file` |

### 7-2. ⚠️ 번호 대역을 미리 배정한다

**`out-of-order: false` 다.** 담당자 6명이 각자 번호를 정하면 **나중에 머지된 쪽이 `validate` 에서 막힌다.**

🚨 **버전 번호는 폴더가 달라도 충돌한다.** `locations: classpath:db/migration` 이 하위를 재귀 스캔하므로
`project/V…100000` 과 `tenant/V…100000` 이 함께 있으면 앱이 기동조차 못 한다
(`FlywayException: Found more than one migration with version …`).

**2026-08-11 재배정.** 최초 배정(`…100000`~`…150000`)이 나중에 머지된 `tenant` 마이그레이션
(`V20260811100000` · `V20260811100100` · `V20260811110000`)과 겹쳐 **전 대역을 뒤로 밀었다.**

| 담당 | 배정 번호 | (구) 최초 배정 |
|---|---|---|
| 동훈 (project 계열) | **`V20260811120000`** | ~~`V20260811100000`~~ |
| 정림 (text) | **`V20260811130000`** | ~~`V20260811110000`~~ |
| 정림 (image) | **`V20260811140000`** | ~~`V20260811120000`~~ |
| 정산 담당 | **`V20260811150000`** | ~~`V20260811130000`~~ |
| 강욱 (approval) | **`V20260811160000`** | ~~`V20260811140000`~~ |
| 이슈·파일 담당 | **`V20260811170000`** | ~~`V20260811150000`~~ |

⛔ **배정받은 번호를 임의로 바꾸지 마라.** 바꿔야 하면 이 문서를 먼저 고친다.
⛔ **아직 배포되지 않은 파일만 rename 할 수 있다.** 이미 운영 DB 에 적용된 파일의 번호를 바꾸면
`flyway_schema_history` 와 어긋나 `validate` 가 막는다.

---

## 8. 폐기된 검토안 — 왜 안 갔는지

| 검토안 | 폐기 이유 |
|---|---|
| **웹소켓 0.5초 동기화 (노션 방식)** | 우리는 노션만큼 동시 편집이 잦지 않다. 인프라·구현 비용 대비 실익 없음 |
| **Redis 편집 잠금 (TTL 60초)** | 낙관락으로 유실이 이미 막힌다. 잠금은 **신규 6파일 + FE 하트비트 타이머 + TTL 만료 처리**가 붙는데, 얻는 건 "사전 차단" 뿐이다. **정책이 2개가 되는 비용이 더 크다** |
| **SSE 사전 차단 알림** | 잠금 폐기와 함께 불필요해짐. 판정은 항상 **저장 시점 서버**가 한다 |
| **`If-Match` 헤더 (스텝 단위)** | 남이 딴 블록을 건드려도 내 저장이 409 (오탐 상시) |
| **테이블 수정중 STATUS + USER_ID 컬럼** | 탭이 죽으면 **영영 잠긴 채로 남는다.** 청소 배치 잡이 필요해진다 |

> 📌 **SSE 자체가 폐기된 것은 아니다.** [`activity_log` 이벤트](../../api/activity-log.md)(`ActivityOccurredEvent`)에
> 리스너를 하나 더 붙이면 **타 도메인 수정 0곳**으로 "누가 뭘 바꿨다" 알림을 쏠 수 있다.
> 단 **`AFTER_COMMIT`** 이어야 한다 — 기존 활동 로그 리스너는 `BEFORE_COMMIT` 이라 그대로 복사하면
> 롤백된 변경을 "바뀌었다" 고 알리게 된다. 이건 **별건**으로 다룬다.

---

## 9. 작업 순서

| 순서 | 내용 | 담당 | 상태 |
|:--:|---|---|---|
| 1 | 이 문서 작성 | 김동현 | ✅ 완료 |
| 2 | **스테이지 낙관락 실구현** (참조 구현 검증) | 김동현 | ✅ 완료 — 마이그레이션 1 + Java 16 |
| 3 | `PRJ-V1-REALTIME.md` 개정 (잠금 정책 → 낙관락) + FE 통보 | 김동현 | ✅ 완료 (2026-08-11) |
| 4 | **스텝 낙관락** | 김동현 | ✅ 완료 (2026-08-11) — Java 24 · 조건부 UPDATE 3종 |
| 5 | `block` → `project` 낙관락 (마이그레이션 컬럼은 2번에서 이미 넣었다) | 김동현 | ⬜ |
| 6 | 나머지 6개 도메인 각자 구현 | 담당자 6명 | ⬜ (선행: 2) |

> 📌 **2·4 검증 (2026-08-11)**: `compileJava`·`compileTestJava` 통과 + stage·step 테스트 **65개 전부 통과**
> (StageCommandService 15 · StageStepPermission 6 · StepCommandService 18 · StepDelete 8 · StepPermission 9 · StepStatusCommandService 9).
> ⚠️ **전체 스위트는 아직 못 돌렸다** — 같은 시각 멀티테넌시(`companyId`) 작업이 병렬로 진행돼 빌드 산출물이 계속 바뀐다.

⚠️ **2번을 먼저 끝냈다.** 문서만 주고 6명이 동시에 시작하면 §6 의 함정이 6번 반복된다.

### 9-1. 스테이지 참조 구현 — 실제 변경 파일 (2026-08-10)

| 구분 | 파일 |
|---|---|
| 🆕 마이그레이션 | `project/V20260811120000__add_version_project_domain.sql` (project·stage·step·block 4테이블) |
| infra | `StageJpaEntity` · `StageMapper` · `SpringDataStageRepository`(조건부 UPDATE 2개) · `StageRepositoryAdapter` |
| domain | `Stage` · `StageRepository` · `StageErrorCode`(`STAGE_VERSION_REQUIRED`·`STAGE_VERSION_CONFLICT`) |
| app | `UpdateStageCommand` · `ReorderStagesCommand` · `StageCommandService` · `StageQueryService` · `StageResult` · `StageOrderResult` · `StageSummary` |
| web | `StageUpdateRequest` · `StageOrderRequest` · `StageUpdateResponse` · `StageOrderResponse` · `StageListResponse` · `StageController` · `ProjectStageController`(409 Swagger) |
| test | `StageCommandServiceTest`(충돌·덮어쓰기·부분롤백 3건 추가) · `StageStepPermissionServiceTest` |

> 📌 **`project`·`block` 은 컬럼만 먼저 들어갔고 코드는 아직이다.** `DEFAULT 1` 이라 기존 동작에 영향은 없다.

### 9-2. 스텝 구현 — 스테이지와 다른 점 (2026-08-11)

| 구분 | 파일 |
|---|---|
| infra | `StepJpaEntity` · `StepMapper` · `SpringDataStepRepository`(조건부 UPDATE **3종**) · `StepRepositoryAdapter` |
| domain | `Step` · `StepRepository` · `StepErrorCode`(`STEP_VERSION_REQUIRED`·`STEP_VERSION_CONFLICT`) |
| app | `UpdateStepCommand` · `ChangeStepStatusCommand` · `ReorderStepsCommand` · `StepUpdateResult` · `StepStatusResult` · `StepOrderResult` · `StepSummary` · `StepDetailResult` · `StepCommandService` · `StepQueryService` |
| web | `StepUpdateRequest` · `StepStatusUpdateRequest` · `StepOrderRequest` · 응답 5종 · `StepController` · `ProjectStepController`(409 Swagger) |
| test | `StepCommandServiceTest`(충돌·덮어쓰기·부분롤백 3건 추가) · `StepStatusCommandServiceTest`(충돌 1건 추가) · `StepDeleteServiceTest` · `StepPermissionServiceTest` |

**스테이지는 UPDATE 가 2종인데 스텝은 3종이다** — 바뀌는 필드 묶음이 API 마다 달라서다.

| 포트 | SET 하는 것 |
|---|---|
| `updateIfVersionMatches` | `name` · `startedOn` · `endedOn` · `ownerUserId` · `updatedAt` |
| `changeStatusIfVersionMatches` | `status` · **`completedAt` · `completedBy`** · `updatedAt` |
| `moveIfVersionMatches` | `stageId` · `sortOrder` · `updatedAt` |

⚠️ **`changeStatus` 는 완료 정보까지 함께 SET 해야 한다.** DONE 에서 벗어나면 완료자·완료시각을 지우는
규칙이 도메인 `Step.changeStatus` 안에 있다. SQL 에 같은 조건을 다시 쓰면 규칙이 두 곳으로 갈라지므로,
**도메인이 계산한 결과값을 그대로 UPDATE 에 넘긴다.** 빠뜨리면 상태만 바뀌고 완료 기록이 DB 에 남는데
**예외도 안 나고 응답도 정상**이라 조회 화면을 봐야만 드러난다.

⛔ **`completeStep` 에는 version 을 걸지 않는다.** "이미 DONE 이면 그대로 둔다" 규칙이 두 번째 요청을
이미 막고 있어서, 낙관락을 더 걸면 정상 동작(둘이 동시에 완료를 눌렀는데 결과가 같음)에 409 모달만 띄운다.

⛔ **`deleteStep` · `StepRelocationService` 도 제외다.** 여전히 `save()` 를 쓴다 — 스테이지 삭제 cascade 경로다.

---

## 10. 검증 순서 — 틀렸을 때 눈에 안 보이는 것부터

1. **조회 응답에 `version` 이 실려 나가는지** DevTools 네트워크 탭에서 직접 확인. 빌드·테스트로는 안 잡힌다
2. 계정 2개로 같은 스테이지를 열고, 한쪽 저장 후 다른 쪽 저장 → **409 가 나는지**
3. 409 후 **재조회** 를 선택하면 최신값이 오는지 / **덮어쓰기** 를 선택하면 통과하는지
4. 저장 성공 응답의 `version` 이 **+1 되어 돌아오는지** (안 되면 다음 저장이 전부 409)
5. 순서 변경 2종은 **A·B가 서로 다른 항목**을 옮겨도 나중 요청이 409 인지 (묶음 version 이라 정상)

---

## 11. 변경 이력

| 날짜 | 내용 | 담당 |
|---|---|---|
| 2026-08-10 | 신설 — 낙관락 단일 정책 확정. Redis 편집 잠금·SSE 사전 차단 폐기. 대상 9테이블, 스테이지 참조 구현, 마이그레이션 번호 대역 배정 | 김동현 |
| 2026-08-11 | **스텝 낙관락 구현** (§9-2 신설 — 조건부 UPDATE 3종 · `changeStatus` 완료정보 동반 SET · `completeStep` 제외 근거) · §7 **마이그레이션 번호 대역 전면 재배정**(tenant 와 충돌) · §3-3 정정(도메인은 version 을 올리지 않는다 · `rename()` 삭제) | 김동현 |
| 2026-08-10 | ⭐ **§4 묶음 version 컬럼 4종 폐기** — 순서 변경이 이미 항목마다 `save()` 를 돌고 있어(`StageCommandService.reorderStages`) **개별 version + 전체 롤백**으로 동일한 결과를 얻는다. 새 컬럼 0개·새 포트 0개. §7 마이그레이션도 `ADD COLUMN version` 한 줄씩으로 단순화 · §6 함정에 부분 커밋·MyBatis 위치 매핑 2건 추가 | 김동현 |
