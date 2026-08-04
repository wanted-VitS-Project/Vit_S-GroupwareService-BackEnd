# 🏗️ 아키텍처 컨벤션

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
