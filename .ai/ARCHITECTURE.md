# 🏗️ 아키텍처 컨벤션

**최종 업데이트**: 2026-08-04 (§2-1 신설 — 애그리게이트가 여러 개인 도메인의 서브패키지 규칙, `project` 계층 38개 엔드포인트 대응)
**최종 업데이트**: 2026-08-04 (신설 — 헥사고날 구조 확정, `businesscategory` 기준)
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

### 권한 처리

`@PreAuthorize`·`@NotBlank`/`@Size` 같은 프레임워크 기본 경로는 쓰지 않는다 (`.ai/API.md` §3-5 참고 — `COMMON_FORBIDDEN`/`COMMON_INVALID_REQUEST` 로 새어나가 명세 에러코드가 안 나간다).
인가는 `application/policy`, 입력 검증은 `service` 내부 수동 검증으로 처리하고 도메인 전용 `ErrorCode` 를 던진다.

---

## 5. 기존 도메인 예외 (레거시)

`auth`·`account` 는 이 컨벤션이 확정되기 전에 만들어졌다. 두 도메인 모두:

- `usecase`/`service` 분리가 없다 — Controller 가 `XxxService` 를 직접 호출한다.
- `policy`/`port` 서브패키지가 없다.
- `domain/repository` 포트가 없다 — Spring Data `JpaRepository`/MyBatis 매퍼 인터페이스를 `infrastructure` 에서 바로 서비스에 주입한다.
- `presentation/api/dto/request|response` 처럼 `dto` 를 한 단계 더 감싼다 (신규 컨벤션은 `api/request|response`, `dto` 없이 바로 둔다).

**신규 기능은 `auth`/`account` 의 구조를 참고하지 말고, 이 문서와 `businesscategory` 를 따른다.**
두 도메인의 기존 코드를 이 컨벤션으로 옮기는 것은 이 문서의 범위 밖이다 — 필요하면 `.ai/local/STATE.md` 백로그에 별도로 등록한다.

---

## 6. 변경 이력

| 날짜 | 변경 내용 | 담당 |
|---|---|---|
| 2026-08-04 | 신설 — `businesscategory` 구현을 기준으로 헥사고날 계층·네이밍 컨벤션 확정, `auth`/`account` 레거시 명시 | 김동현 |
| 2026-08-04 | §2-1 신설 — 애그리게이트별 서브패키지 규칙(`project` 계층). 애그리게이트 간 참조도 `port`+`adapter`, URL↔패키지 불일치 허용 | 동훈 |
