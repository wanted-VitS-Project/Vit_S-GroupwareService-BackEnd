# 🏗️ 아키텍처 컨벤션

**최종 업데이트**: 2026-08-09 (§4 **검증 책임 분리 신설** — 형식은 Request 애노테이션 · 관계/불변식은 서비스. `GlobalExceptionHandler` 가 `"CODE|문구"` 를 응답 `code` 로 승격하도록 고쳐 **애노테이션 금지 규칙을 폐기**했다)
**최종 업데이트**: 2026-08-09 (§4 PATCH 요청 파싱 2건 — 파싱은 `XxxRequest` 소유, `JsonNode` 는 명세가 "생략 vs null" 을 구분한 API 한정)
**최종 업데이트**: 2026-08-05 (§2-3 신설 — 이벤트 기반 **N:1 cross-cutting 도메인** 규칙. 공용 이벤트 계약 + `presentation/event` 진입점, 근거: `activitylog`)
**담당**: 김동현 (DevOps)
**근거**: `businesscategory` 도메인 구현 (#96~#99) — 4계층 + `command/query/result/usecase/service/policy/port` 전체를 갖춘 첫 완성 사례

> ✅ **헥사고날(포트&어댑터) 구조로 확정이다.** `.ai/PIPELINE.md` §5, `.coderabbit.yaml` 의 계층별 리뷰 규칙은 이 문서를 근거로 동작한다.
> 새 도메인을 만들 때는 이 문서의 패키지 구조·네이밍을 그대로 따른다.

---

## 1. 개요

이 프로젝트는 **헥사고날 아키텍처(포트&어댑터)** 를 쓴다. 도메인 로직을 프레임워크·DB·외부 API로부터 격리하고,
외부와의 접점은 전부 인터페이스(포트)로 추상화한 뒤 구현체(어댑터)를 갈아끼우는 구조다.

기준 도메인은 `businesscategory` (`src/main/java/com/group3/vitamins/businesscategory`)다.
목록 조회·생성·수정·삭제 4개 엔드포인트를 전부 이 구조로 구현했고, 아래 패키지 구조·네이밍 규칙은 전부 이 코드에서 그대로 뽑았다.

새 도메인을 시작할 때는 **`businesscategory` 패키지를 템플릿처럼 보고 시작**하면 된다.

---

## 2. 패키지 구조

| 계층 | 서브패키지 | 책임 | 예시 (`businesscategory`) |
|---|---|---|---|
| `presentation` | `api` | Controller, 응답 메시지 상수 | `BusinessCategoryController`, `BusinessCategoryResponseMessage` |
| | `api/request` | 요청 DTO. `toCommand()` 로 커맨드 변환 | `BusinessCategoryCreateRequest` |
| | `api/response` | 응답 DTO. `from(Result)` 로 결과 변환 | `BusinessCategoryDetailResponse` |
| `application` | `usecase` | 인바운드 포트 인터페이스 (Controller 가 의존하는 계약) | `BusinessCategoryCommandUseCase` |
| | `service` | usecase 구현체. 트랜잭션 경계, 흐름 조율 | `BusinessCategoryCommandService` |
| | `command` / `query` | 서비스 입력 DTO (record) | `UpdateBusinessCategoryCommand` |
| | `result` | 서비스 출력 DTO (record) — 도메인 객체를 그대로 반환하지 않는다 | `BusinessCategoryResult` |
| | `policy` | 인가·비즈니스 규칙 판정 컴포넌트 | `BusinessCategoryAdminPolicy` |
| | `port` | 다른 도메인에 대한 아웃바운드 포트 | `ProjectCategoryLinkPort` |
| `domain` | `model` | 순수 도메인 객체. JPA·Spring 비의존 | `BusinessCategory` |
| | `repository` | 영속성 아웃바운드 포트 (인터페이스만) | `BusinessCategoryRepository` |
| | `exception` | `ErrorCode` 구현 enum | `BusinessCategoryErrorCode` |
| `infrastructure` | `persistence` | JpaEntity · Entity↔도메인 Mapper · `XxxRepository` 구현체(`RepositoryAdapter`) · SpringData 인터페이스 | `BusinessCategoryJpaEntity`, `BusinessCategoryMapper`, `BusinessCategoryRepositoryAdapter`, `SpringDataBusinessCategoryRepository` |
| | `adapter` | **다른 도메인**의 포트를 구현하는 어댑터 (MyBatis 등) | `ProjectCategoryLinkAdapter` + `ProjectCategoryLinkQueryMapper` |

### 참고 트리 (`businesscategory`)

```
businesscategory/
├── application/
│   ├── command/       CreateXxxCommand, UpdateXxxCommand, DeleteXxxCommand
│   ├── query/          XxxListQuery
│   ├── result/         XxxResult
│   ├── usecase/         XxxCommandUseCase, XxxQueryUseCase
│   ├── service/         XxxCommandService, XxxQueryService (usecase 구현)
│   ├── policy/          XxxAdminPolicy
│   └── port/            ProjectCategoryLinkPort (아웃바운드)
├── domain/
│   ├── model/           BusinessCategory
│   ├── repository/      BusinessCategoryRepository (아웃바운드 포트)
│   └── exception/       BusinessCategoryErrorCode
├── infrastructure/
│   ├── persistence/     JpaEntity, Mapper, RepositoryAdapter, SpringData 인터페이스
│   └── adapter/          다른 도메인 포트에 대한 구현체
└── presentation/
    └── api/
        ├── request/      CreateRequest, UpdateRequest (record + toCommand())
        └── response/     DetailResponse, ListResponse (record + from(Result))
```

---

## 2-1. 애그리게이트가 여러 개인 도메인 — 서브패키지

`businesscategory` 는 애그리게이트가 하나라 §2 의 평면 구조로 충분하다.
**애그리게이트가 여러 개고 파일이 수십 개로 불어나는 도메인은 애그리게이트별 서브패키지로 나눈다.**
현재 해당하는 건 `project` 뿐이다 (프로젝트~블록 계층 · 엔드포인트 38개).

### 용어 — "바운디드 컨텍스트" 가 아니다

`project`·`stage`·`step`·`block` 은 **하나의 컨텍스트 안의 4개 애그리게이트**다. 컨텍스트로 부르지 않는다:
유비쿼터스 언어가 하나고(요구사항·API 명세가 한 문서), 담당자가 하나고, 트랜잭션이 서로 걸린다
(스텝 완료 → 프로젝트 진척률 · 스텝 삭제 → 이슈 처리). 컨텍스트라고 부르면 "직접 조인 금지" 같은
과한 제약을 스스로 짊어지게 된다.

### 트리

**루트 애그리게이트는 도메인 루트에 그대로 두고, 나머지만 서브패키지로 뺀다.**
`project/project/domain/model/Project` 처럼 이름이 겹치는 걸 피하고 기존 파일 이동도 없다.

```
project/
├── domain/ application/ infrastructure/ presentation/   ← Project 애그리게이트 (루트)
├── stage/  └ domain/ application/ infrastructure/ presentation/
├── step/   └ domain/ application/ infrastructure/ presentation/
└── block/  └ domain/ application/ infrastructure/ presentation/
```

서브패키지 **내부는 §2 의 4계층·네이밍을 그대로** 따른다. 계층을 서브패키지 밖으로 끌어올리지 않는다
(⛔ `project/domain/model/Step` 처럼 애그리게이트를 섞지 않는다).

### 애그리게이트 간 참조

§2 의 규칙이 도메인 간뿐 아니라 **애그리게이트 간에도 그대로 적용된다.**

| 대상 | 처리 |
| --- | --- |
| 내 애그리게이트 테이블 | JPA 엔티티 + `domain/repository` 포트 + `infrastructure/persistence` 의 `RepositoryAdapter` |
| **다른 애그리게이트 테이블** (같은 도메인이라도) | `application/port` 인터페이스 + `infrastructure/adapter` 구현 (MyBatis) |

포트는 **소비자가 소유한다.** 조회당하는 쪽이 아니라 조회하는 쪽 패키지에 포트와 어댑터를 둔다
(선례: `businesscategory` 의 `ProjectCategoryLinkPort` · `project` 의 `BusinessCategoryLookupPort`).
이렇게 두면 상대 애그리게이트가 나중에 서브패키지로 분리돼도 포트·어댑터는 움직이지 않는다.

### URL 과 패키지는 일치하지 않아도 된다

URL 은 프론트와의 계약이라 패키지 구조를 따라 바꿀 수 없다.
`GET /api/v1/projects/{projectId}/progress` 처럼 **`/projects` 경로를 다른 애그리게이트 모듈이 매핑하는 것을 허용한다.**
엔드포인트를 어느 모듈이 구현할지는 **API 명세의 `도메인` 열**을 따른다.

---

## 2-2. 쓰기 방향 포트 · 타입별 확장 (SPI)

§2-1 의 참조 규칙은 **조회**를 전제로 쓰였다(선례가 전부 `*LookupPort` 다). **다른 모듈의 테이블에 써야 할 때**와 **한 개념을 여러 담당자가 타입별로 나눠 구현할 때**의 규칙을 아래에 못박는다.
근거 사례: `project/block` — 블록 골격은 한 사람이 갖고 타입별 상세 10종은 5명이 나눠 갖는다.

### 쓰기도 포트다 — 소유자는 여전히 소비자

| 대상 | 처리 |
| --- | --- |
| 다른 모듈 테이블 **조회** | `application/port` + `infrastructure/adapter` (MyBatis) — §2-1 그대로 |
| 다른 모듈 테이블 **쓰기** | **같다.** 포트를 소비자가 소유하고 어댑터도 소비자 패키지에 둔다 |

어댑터 **안에서** 무엇을 하느냐는 따로 판단한다:

| 상황 | 어댑터 구현 |
| --- | --- |
| 상대에게 **재사용할 로직이 있다** (cascade · 멱등 처리 등) | 상대의 **인바운드 유스케이스(또는 `@Service` 구체 클래스)를 호출**한다. 로직을 복제하지 않는다 |
| 상대에게 **그 로직이 없다** | 어댑터가 **직접 SQL/JPA 로 쓴다.** 없는 코드를 상대 패키지에 만들어 달라고 기다리지 않는다 |

⚠️ **트랜잭션은 하나여야 한다.** 상대 서비스는 `@Transactional`(기본 `REQUIRED`)로 호출자 트랜잭션에 참여한다.
⛔ **경계를 넘는 쓰기를 이벤트로 처리하지 마라 — 상대가 독립 생명주기를 갖지 않는 경우.**
판단 기준: 상대 행이 **내 행 없이 존재할 수 있는가**(`FK`/`NOT NULL` 로 확인) · **삭제 판정 권한이 누구에게 있는가**.
둘 다 "나" 라면 같은 애그리게이트의 일부이므로 **강한 일관성(한 트랜잭션)** 이 맞다. 이벤트는 결과적 일관성을
감수할 수 있는 경계에서만 쓴다 — 회수 주체가 없는데 이벤트를 쓰면 그건 결과적 일관성이 아니라 **유실**이다.

### 타입별 확장은 SPI 로 — 공용 파일을 여럿이 편집하게 만들지 마라

한 개념이 타입별로 갈리고 **타입마다 담당자가 다르면**, 판별자 분기를 한곳에 모으지 않는다.

```
{소비자}/application/port/{X}Port          ← 인터페이스. supportedType() 으로 자기 타입을 선언
{소비자}/infrastructure/adapter/A{X}Adapter ← 타입 A 담당자가 추가
{소비자}/infrastructure/adapter/B{X}Adapter ← 타입 B 담당자가 추가
```

소비자 서비스는 `List<{X}Port>` 를 주입받아 `supportedType()` 으로 `Map` 을 만든다. **`switch`·`if` 분기가 없다.**

| 이유 | 설명 |
| --- | --- |
| **머지 컨플릭트 차단** | 타입 추가 = **파일 추가**. 공용 XML `<discriminator>`·거대 flat Row·`switch` 문은 **N명이 같은 파일을 편집**하게 만든다 |
| **부분 실패 격리** | 어댑터가 없는 타입은 해당 필드만 `null`. 한 타입이 미구현·오류여도 나머지가 응답된다. 단일 조인 쿼리는 **한 타입이 깨지면 전체가 죽는다** |
| **record 유지** | MyBatis `<discriminator>` 는 세터 주입 기반이라 불변 `record` 와 조합이 나쁘다. 어댑터별 조회는 각자 `record` 를 쓴다 |
| ⛔ **`sealed` 금지** | 반환 타입 계층은 **비-sealed 마커 인터페이스**로 둔다. `sealed` 는 `permits` 절 때문에 또 공용 파일을 편집하게 된다 |

**쿼리 비용 기준**: 배치 조회(`WHERE pk IN (…)`)라면 타입 수만큼 쿼리가 늘어도 된다.
금지선은 **레코드 개수에 비례하는 쿼리(N+1)** 이지 타입 수가 아니다.

---

## 3. 계층 간 의존 방향

`.coderabbit.yaml` 의 계층별 리뷰 규칙이 강제하는 원칙과 동일하다 (내용 재작성 없이 그대로 옮김):

- **`domain`** 은 Spring·JPA·Web 등 프레임워크 기술에 과도하게 의존하지 않는다. public setter·무분별한 `@Setter` 를 쓰지 않고, 상태 변경은 도메인 내부 메서드로 표현한다.
- **`application`** 은 도메인 흐름을 조율한다. 트랜잭션 경계는 usecase 단위로 잡는다. 외부 구현체·JPA Entity·Web DTO 에 과도하게 의존하지 않는다.
- **`infrastructure`** 는 `application`/`domain` 의 포트를 **구현하는 방향**으로만 의존한다. DB·외부 API 구현 세부사항이 `application` 밖으로 새면 안 된다.
- **`presentation`** 은 요청/응답 변환과 usecase 호출에만 집중한다. Entity·Repository·JPA 구현체에 직접 의존하지 않고, 비즈니스 로직·쿼리 조립을 담지 않는다.

즉 의존 방향은 `presentation → application → domain` 이고, `infrastructure` 는 `domain`/`application` 이 정의한 포트를 구현해 반대 방향으로 연결된다 (의존성 역전).

---

## 4. 네이밍 컨벤션

| 패턴 | 역할 | 위치 |
|---|---|---|
| `XxxCommand` / `XxxQuery` (record) | 서비스 입력 | `application/command`, `application/query` |
| `XxxResult` (record) | 서비스 출력 | `application/result` |
| `XxxUseCase` (interface) | 인바운드 포트 | `application/usecase` |
| `XxxService` (구현체) | usecase 구현 | `application/service` |
| `XxxPolicy` | 인가/규칙 판정 (`@PreAuthorize` 대체) | `application/policy` |
| `XxxPort` (interface) / `XxxAdapter` (구현체) | 타 도메인 아웃바운드 포트 | `application/port` / `infrastructure/adapter` |
| `XxxRepository` (interface) | 영속성 아웃바운드 포트 | `domain/repository` |
| `XxxRepositoryAdapter` (구현체) | `XxxRepository` 구현 | `infrastructure/persistence` |
| `XxxErrorCode` (enum) | `ErrorCode` 구현체 | `domain/exception` |
| `XxxRequest` / `XxxResponse` (record) | Web DTO | `presentation/api/request`, `presentation/api/response` |
| `XxxCleanupConfig` (`@Configuration`) | `global`의 하드 딜리트 SPI(`HardDeleteTarget`)에 자기 도메인 몫을 등록하는 팩토리 | `infrastructure/cleanup` — 상세 규칙은 `.ai/docs/global/CLEANUP.md` |

### 권한 처리

`@PreAuthorize`·`@NotBlank`/`@Size` 같은 프레임워크 기본 경로는 쓰지 않는다 (`.ai/API.md` §3-5 참고 — `COMMON_FORBIDDEN`/`COMMON_INVALID_REQUEST` 로 새어나가 명세 에러코드가 안 나간다).
인가는 `application/policy`, 입력 검증은 `service` 내부 수동 검증으로 처리하고 도메인 전용 `ErrorCode` 를 던진다.

---

## 5. 레거시 도메인 이관 완료 (2026-08-05)

`department`·`account`·`auth` 는 이 컨벤션 확정 전에 만들어진 구세대 3계층이었으나 **2026-08-05 헥사고날로 이관 완료**했다.
이제 전 도메인이 이 문서의 구조를 따른다 (`refactor/department-hexagonal` · `refactor/account-auth-hexagonal`).

- **department** — 완전 이관. `domain/model`·`domain/repository` 포트 + `infrastructure/persistence` 어댑터까지 `businesscategory` 와 동형.
- **account** — `application`(usecase·service·command·result·policy·port)·`presentation`(request/response) 이관 완료.
  단 `AccountEntity`·`AccountJpaRepository` 는 **auth 와 공유하는 인증 애그리게이트**라 `domain/model` 완전 분리는 보류했다(아래 잔여).
- **auth** — 이관 완료. **자기 소유 테이블이 없는 오케스트레이션 도메인**이라 `domain/model`·`domain/repository` 가 없다(정상).
  로그인·잠금·약관은 account 의 공유 엔티티를 직접 쓰고, 실패 기록만 `LoginFailureRecordPort`(REQUIRES_NEW)로 분리했다.

> 🔖 **잔여**: `AccountEntity` 를 account 소유 순수 도메인 모델로 완전 분리하는 것은 account·auth 양쪽에 걸친 별도 작업이다
> (`.ai/local/STATE.md` 백로그). 인증 핵심 재배선이라 리스크가 커, 지금은 공유 인증 엔티티로 두는 것을 의도적으로 택했다(B2).

---

## 6. 변경 이력

| 날짜 | 변경 내용 | 담당 |
|---|---|---|
| 2026-08-08 | §4 갱신 — `XxxCleanupConfig` 네이밍 추가. `global`에 하드 딜리트 스케줄러 SPI(`HardDeleteTarget`/`HardDeleteOperation`/`HardDeleteExecutor`/`HardDeleteScheduler`) 신설 — 스케줄러는 `global`에 하나, 도메인은 포트만 구현해 등록. 상세는 `.ai/docs/global/CLEANUP.md` (⚠️ develop 병합 중 한 번 유실돼 재반영, 2026-08-09) | 김동현 |
| 2026-08-04 | 신설 — `businesscategory` 구현을 기준으로 헥사고날 계층·네이밍 컨벤션 확정, `auth`/`account` 레거시 명시 | 김동현 |
| 2026-08-04 | §2-1 신설 — 애그리게이트별 서브패키지 규칙(`project` 계층). 애그리게이트 간 참조도 `port`+`adapter`, URL↔패키지 불일치 허용 | 동훈 |
| 2026-08-05 | §2-2 신설 — 쓰기 방향 포트(경계 넘는 쓰기는 한 트랜잭션, 이벤트 아님) · 타입별 확장 SPI(`List<Port>` + `supportedType()`, 공용 파일 동시 편집 금지, `sealed` 금지). 근거: `project/block` 타입 10종을 5명이 나눠 갖는다 | 동훈 |
