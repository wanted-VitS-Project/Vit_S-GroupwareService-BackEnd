# 📐 API 규칙

**최종 업데이트**: 2026-08-04 (§0·§1·§2 개정 — 노션 계약 폐지, `.ai/api/*.md` 단일 기준. 상태 게이트 삭제)
**최종 업데이트**: 2026-07-28 (초안 생성 — 공통 컨벤션 팀 합의 전)
**관리**: 김동현 (DevOps) · **명세 작성**: 각 도메인 담당자

> 📖 관련: [CONVENTION.md](CONVENTION.md) · [INFRA.md](INFRA.md) · [PIPELINE.md](PIPELINE.md)

---

## 0. 🚨 최우선 원칙 — 명세는 프론트와의 계약이다

**프론트엔드는 이미 명세서를 보고 개발하고 있다.** 백엔드가 명세와 다르게 구현하면
연동 시점에 프론트 코드가 깨지고, 원인 추적에 시간이 든다.

따라서 이 문서의 목적은 문서 관리가 아니라 **명세 이탈 방지**다.

### AI 작업 규칙 (필수)

API 코드(Controller · Service · DTO · Repository · 엔드포인트 · 에러코드)를 작성하거나 수정할 때:

| 상황 | 해야 할 것 |
|------|-----------|
| `.ai/api/{도메인}.md` 에 해당 엔드포인트 명세가 있다 | 그대로 구현한다. 경로·메서드·필드명·타입·상태코드를 **한 글자도 바꾸지 않는다** |
| 명세가 md 에 없다 (파일이 없거나 그 엔드포인트가 없다) | ⛔ **작성을 멈추고**, 먼저 명세를 `.ai/api/{도메인}.md` 에 작성하자고 사용자에게 제안한다 (설계 요청이면 바로 작성) |
| 명세에 없는 필드·엔드포인트가 필요해 보인다 | ⛔ **임의로 추가하지 않는다.** 무엇이 왜 필요한지 사용자에게 보고하고 md 에 먼저 반영한다 |
| 명세가 틀렸거나 구현이 불가능하다 | ⛔ **코드를 바꾸지 말고 사용자에게 알린다.** 명세(md) 변경은 프론트에 영향을 주므로 팀 합의 사항이다 |
| 명세와 기존 코드가 다르다 | 어느 쪽이 맞는지 **추측하지 말고** 사용자에게 확인한다 (기준은 md 명세다) |

> 📌 **설계 요청은 자유롭게.** 사용자가 "API 설계해줘 / 명세 짜줘" 라고 하면 `.ai/api/{도메인}.md` 에
> 바로 작성하면 된다. md 에 적힌 순간부터 그게 계약이다 — 별도 승격 절차는 없다.

### ❌ 절대 금지

- 명세에 없는 엔드포인트 경로를 **지어내기**
- 필드명을 "더 나은 이름"으로 **바꾸기** (`nickname` → `userName` 등)
- 명세에 없는 요청/응답 필드를 **추가하기**
- 상태 코드를 임의 변경하기 (명세 `201` → 구현 `200`)
- 에러 코드를 임의로 만들기
- "명세가 없으니 일반적인 관례대로" 구현하기

> 💡 명세 이탈이 필요하다고 판단되면 그것은 **코드 문제가 아니라 팀 커뮤니케이션 사안**이다.
> AI 는 판단하지 말고 보고한다.

---

## 1. 명세 라이프사이클 — md 가 단일 기준이다

**명세는 `.ai/api/{도메인}.md` 에서 태어나고, 거기가 곧 계약이다** (2026-08-04 전환).
~~노션~~ 을 계약 원본으로 쓰던 방식은 폐지됐다. 반영 지연·이중 관리 비용이 md 단일화의 이득보다 컸다.

```
① 설계·수정   .ai/api/{도메인}.md 에 작성/갱신   ← 여기서 계약 성립
② 구현        md 명세대로 코드 작성
③ 확인        Swagger 로 구현 결과 대조
```

### 🔑 기준은 하나다

> **`.ai/api/{도메인}.md` 에 적힌 것이 계약이다. md 에 있으면 구현한다.**

- 별도의 `📝 초안` / `✅ 확정` 상태나 승격 절차는 **없다.** md 에 명세가 있으면 그게 최신이고 계약이다.
- 명세를 고치는 것은 곧 계약을 고치는 것이다 — 프론트에 영향을 주므로 **코드보다 md 를 먼저** 바꾸고, 팀에 공유한다.
- 개별 도메인 파일에 구현 진행상태(미구현/구현중/완료)를 메모로 남기는 것은 자유지만, 그것이 **구현 가능 여부를 막지는 않는다.**

### 방향별 규칙

| 상황 | 규칙 |
|------|------|
| **새 API 설계** | `.ai/api/{도메인}.md` 에 바로 작성한다. 작성한 순간부터 계약 |
| **명세 변경** | md 를 먼저 고치고 → 코드를 맞춘다. 코드를 먼저 바꾸고 명세를 나중에 맞추는 방향 금지 |
| **명세와 코드 불일치** | md 가 맞다. 코드를 md 에 맞춘다 (md 자체가 틀렸다면 md 를 먼저 고친다) |

### 왜 전체를 한 파일에 몰지 않나

**작업 중인 도메인만** `.ai/api/` 에 둔다. 이전 모듈에서 1:1 미러한 명세 파일이
**2,025줄 / 72KB** 까지 커져 "작업 전 반드시 읽어라" 규칙이 물리적으로 지켜질 수 없게 됐다.
도메인별로 쪼개 두면 작업 전 해당 파일만 읽으면 된다.

---

## 2. Swagger (springdoc-openapi)

| 항목 | 내용 |
|------|------|
| 역할 | 구현 결과 확인 + 프론트가 직접 호출해볼 UI |
| **역할이 아닌 것** | 명세의 원본. 명세는 `.ai/api/{도메인}.md` 에 있다 |
| 도입 상태 | ✅ **도입 완료** (2026-07-28, springdoc 2.8.17) |
| Swagger UI | `/swagger-ui.html` |
| OpenAPI JSON | `/v3/api-docs` |
| 설정 클래스 | `com.group3.vitamins.config.OpenApiConfig` |

> ⚠️ **현재 Spring Security 기본 설정에 막혀 있다.** SecurityConfig 작성 시 아래 경로를 허용해야 한다.
> ```
> /swagger-ui.html, /swagger-ui/**, /v3/api-docs/**
> ```
> 운영 프로필에서는 springdoc 자체가 꺼지므로 허용해도 노출되지 않는다.

> ⚠️ **인증 스킴 미정의**: 현재 `spring-session`(Redis/JDBC) 기반이라 세션 쿠키 인증으로 보인다.
> 인증 방식이 확정되면 `OpenApiConfig` 에 스킴을 추가한다. 잘못된 스킴은 프론트를 오도하므로
> 확정 전까지는 정의하지 않는다.

> Swagger 에 나온 내용과 md 명세가 다르면 **구현이 틀린 것**이다. 명세를 고치지 말고 구현을 고친다.

### 2-1. 도입 설정 (완료)

| 파일 | 내용 |
|------|------|
| `build.gradle` | `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17` |
| `application.yml` | UI/JSON 경로, 정렬, `paths-to-match: /api/**` |
| `application-prod.yml` | 운영 비활성화 |
| `config/OpenApiConfig.java` | 문서 제목·설명·버전 |

> ⚠️ **버전 주의**: `3.0.x` 는 Spring Boot 4 용이다. 우리는 Boot 3.5.16 이므로 **2.8.x** 를 쓴다.
> 버전을 올릴 때 Boot 버전과의 대응을 반드시 확인할 것.

### 2-2. ⚠️ 운영 환경에서는 끈다 (적용 완료)

Swagger UI 는 **API 전체 구조를 그대로 노출한다.** 운영에 열려 있으면 공격자에게 지도를 주는 것과 같다.

`application-prod.yml` 에서 비활성화했고, **실제로 404 를 반환하는 것까지 검증**했다 (2026-07-28).

```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

`local` / `dev` 프로필에서만 열어둔다. **운영 배포 시 `SPRING_PROFILES_ACTIVE=prod` 설정이 필수**이며,
이를 빠뜨리면 Swagger 가 그대로 열린다. 배포 설정에서 반드시 확인할 것.

### 2-3. 어노테이션 규칙

> **원칙: 어노테이션은 md 명세를 그대로 옮긴 것이어야 한다.**
> 어노테이션에 적는 설명·예시·에러코드는 명세와 일치해야 하며, 여기서 새로운 내용을 지어내지 않는다.

| 위치 | 어노테이션 | 필수 | 용도 |
|------|-----------|------|------|
| Controller 클래스 | `@Tag(name, description)` | ✅ | 도메인 그룹핑. name 은 도메인명 (`Auth`, `User`) |
| Controller 메서드 | `@Operation(summary, description)` | ✅ | summary = 명세의 "API 명칭" |
| Controller 메서드 | `@ApiResponses` + `@ApiResponse` | ✅ | 명세에 있는 **모든** 상태코드·에러코드 |
| 인증 필요 엔드포인트 | `@SecurityRequirement(name = "...")` | ✅ | 자물쇠 표시 |
| Path/Query 파라미터 | `@Parameter(description, example)` | ✅ | |
| DTO 필드 | `@Schema(description, example)` | ✅ | 프론트가 보는 필드 설명 |
| 내부 전용 엔드포인트 | `@Hidden` | — | 문서에서 숨김 |

### 2-3-1. null 표현 규칙 (2026-08-06 확정)

> **null 가능 여부는 설명문이 아니라 `nullable = true` 로 표현한다.**

설명문에 "미지정이면 null" 이라고 적어도 **생성된 OpenAPI 문서에는 아무 흔적이 남지 않는다.**
프론트가 스키마로 타입을 생성하면 그 필드는 non-null 로 잡히고, 실제 응답에 null 이 오는 순간 깨진다.
설명문은 사람이 읽는 보조 설명이지 기계가 읽는 계약이 아니다.

springdoc 은 `springdoc.api-docs.version` 을 지정하지 않으면 **OpenAPI 3.0** 문서를 만들고,
3.0 에서 null 허용을 나타내는 유일한 수단이 `nullable: true` 다. (3.1 로 올리면 `type: [..., "null"]`
방식으로 바뀌므로, 버전을 바꿀 때 이 규칙도 함께 재검토한다.)

| 대상 | 규칙 |
|------|------|
| **응답** 필드가 null 일 수 있다 | ✅ `@Schema(nullable = true)` **필수** + 설명문에도 조건을 적는다 |
| **요청** 필드에 **명시적 null 이 의미를 갖는다** (예: "null 을 보내면 해제") | ✅ `nullable = true` |
| **요청** 필드가 **생략 가능**할 뿐이다 (예: "생략하면 기본값") | ❌ 붙이지 않는다 — 이건 `required` 의 영역이지 `nullable` 이 아니다 |
| 필드 타입이 **다른 객체**(`$ref`) 다 | ⚠️ **보류.** springdoc 2.8.17 에서 `$ref` 필드에 nullable 을 붙이면 참조된 스키마 자체가 오염되는 회귀가 보고돼 있다. 실제 `/v3/api-docs` 출력을 확인하기 전까지 붙이지 않는다 |

**`example` 에 `"null"` 을 적지 마라.** 문자열 `"null"` 이라는 예시값이 되어 프론트를 오도한다.
null 가능은 `nullable = true` 로 표현하고, `example` 에는 **실제 값 예시**를 넣는다.
넣을 값이 마땅치 않으면 `example` 을 생략한다.

```java
// ✅ 올바른 예
@Schema(description = "부서명. 부서 미배정이면 null", example = "사업1팀", nullable = true)
String department,

// ❌ 잘못된 예 — 설명문에만 null 이 있고 스키마엔 안 나타난다
@Schema(description = "부서명. 부서 미배정이면 null", example = "사업1팀")
String department,

// ❌ 잘못된 예 — example 이 문자열 "null" 이 된다
@Schema(description = "연결된 공고 ID", example = "null")
Long noticeId,
```

> 📌 **적용 범위**: 규칙 확정과 동시에 `project`·`businesscategory` 도메인(담당: 동훈)은 일괄 정리했다.
> 나머지 도메인은 **각 담당자가 해당 도메인을 손볼 때 함께 정리**한다. 도메인을 가로지르는 소급 일괄 수정은
> 하지 않는다 — 리뷰 단위가 커져 실제 변경이 묻히고, 담당자가 아닌 사람이 계약을 바꾸게 된다.

### 2-4. 작성 예시

```java
@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SignupService signupService;

    @Operation(
        summary = "회원가입",
        description = "이메일과 비밀번호로 회원을 생성한다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "가입 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "입력값 오류 (AUTH_001)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "이메일 중복 (AUTH_002)")
    })
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(signupService.signup(request));
    }
}
```

```java
public record SignupRequest(

    @Schema(description = "이메일", example = "user@example.com")
    @NotBlank @Email
    String email,

    @Schema(description = "비밀번호 (8자 이상)", example = "password123!")
    @NotBlank @Size(min = 8)
    String password,

    @Schema(description = "닉네임", example = "홍길동")
    @NotBlank
    String nickname
) {}
```

> 🐛 **이름 충돌 주의**: 공통 응답 래퍼를 `ApiResponse<T>` 로 만들면 Swagger 의
> `io.swagger.v3.oas.annotations.responses.ApiResponse` 와 **이름이 겹친다.**
> 위 예시처럼 FQCN 을 쓰거나, 래퍼 클래스 이름을 `CommonResponse` / `ApiResult` 등으로 짓는다.
> (§3-1 공통 응답 포맷 확정 시 함께 결정할 것)

### 2-5. 금지 사항

- ❌ 어노테이션 설명에 **명세에 없는 내용**을 지어내기
- ❌ 명세에 있는 **에러코드를 빠뜨리기** — 프론트가 에러 분기를 못 짬
- ❌ `example` 에 **실제 계정·토큰·개인정보** 넣기 (⚠️ 이 레포는 PUBLIC)
- ❌ null 가능 필드를 **설명문으로만** 표시하고 `nullable = true` 를 빠뜨리기 (§2-3-1)
- ❌ `example = "null"` — 문자열 `"null"` 이 예시값이 된다 (§2-3-1)
- ❌ 운영 프로필에서 Swagger UI 열어두기
- ❌ 어노테이션만 고치고 실제 구현은 그대로 두기 (문서와 동작 불일치)

---

## 3. 공통 컨벤션 🔴 미확정 — 팀 합의 필요

> ⚠️ 도메인 담당자별로 명세를 따로 작성했다면 **응답 포맷이 제각각일 가능성이 높다.**
> `.ai/api/` 의 md 명세들을 대조해 실제로 통일돼 있는지 먼저 확인할 것. 통일돼 있다면 그 형태를 아래에 옮겨 적고,
> 제각각이라면 지금 통일해야 한다. (나중에 고치면 프론트 코드까지 전부 바뀐다)

### 3-1. 응답 포맷 ✅ (실제 코드 = 계약)

> 실제 구현(`ApiResponse` · `ApiErrorResponse` · `GlobalExceptionHandler`)이 이 형태다. 필드명·구조를 바꾸지 마라.

```jsonc
// 성공
{ "httpStatus": 200, "message": "...", "data": { } }

// 실패
{ "httpStatus": 400, "message": "...", "code": "AUTH_LOGIN_FAILED" }
```

> ⛔ 필드는 `httpStatus`(`status` 아님) · `message` · `data`(성공) / `code`(실패)다.
> `timestamp` 는 두지 않는다 — 명세에 없다(디버깅 정보는 서버 로그).

### 3-2. URL 규칙 🔴

- 버전 프리픽스: `/api/v1`
- 리소스는 **복수형 명사**: `/api/v1/users`
- 행위는 URL 이 아니라 HTTP 메서드로 표현
- 케이스: kebab-case (`/api/v1/user-agreements`)

### 3-3. 에러 코드 체계 ✅ 의미식으로 확정 (2026-08-04)

- 형식: **`{도메인접두어}_{의미}`** — 예) `AUTH_LOGIN_FAILED` · `AUTH_ACCOUNT_LOCKED` · `ACC_NOT_FOUND` · `DEPT_NAME_DUPLICATED` · `FILE_APPROVAL_IN_PROGRESS`.
  ⛔ **번호식(`AUTH_001`)은 쓰지 않는다** — `.ai/api/` 명세와 실제 코드 전부 의미식이다.
- 도메인 접두어는 담당자별로 선점해 충돌을 막는다 (`AUTH_`·`ACC_`·`DEPT_`·`EMP_`·`FILE_` 등)

#### ⭐ 공통 에러 코드 — `COMMON_*` (2026-08-04 추가 · 2026-08-07 `405` 추가)

**도메인과 무관하게 모든 엔드포인트에서 나올 수 있다.** 프레임워크 레벨 오류라 도메인 코드로 표현할 수 없다.
프론트는 도메인 코드 분기의 **폴백**으로 이 5개를 처리해야 한다.

| code | HTTP | 언제 |
|------|:----:|------|
| `COMMON_INVALID_REQUEST` | 400 | 요청 형식 오류 — 검증 실패 · 타입 불일치 · 필수 파라미터 누락 · 본문 파싱 실패 |
| `COMMON_FORBIDDEN` | 403 | 인증은 됐으나 권한 부족 (도메인별 403 은 각 도메인 코드를 쓴다) |
| `COMMON_NOT_FOUND` | 404 | **존재하지 않는 경로.** 리소스 없음이 아니라 URL 자체가 매핑되지 않은 경우 |
| `COMMON_METHOD_NOT_ALLOWED` | 405 | **경로는 맞지만 그 HTTP 메서드가 없음.** 응답에 `Allow` 헤더로 지원 메서드를 담는다 |
| `COMMON_INTERNAL_ERROR` | 500 | 처리되지 않은 서버 오류 |

> ⚠️ **`404` 두 종류를 구분하라.** URL 오타는 `COMMON_NOT_FOUND`, "그 계정이 없다" 는
> 도메인 코드(`ACC_NOT_FOUND` 등)다. 전자는 프론트 버그, 후자는 정상 흐름이다.
>
> 🆕 **`405` 는 2026-08-07 추가됐다.** 그 전에는 핸들러가 없어 **잘못된 메서드가 전부 `500`** 으로 나갔다
> (예: `PATCH /api/v1/notifications` — GET 만 있는 경로). 프론트가 자기 버그와 서버 장애를 구분하지
> 못하고 `ERROR` 로그도 쌓였다. `COMMON_NOT_FOUND` 와 같은 부류의 누락이었다.
>
> 🚨 **도메인 에러에 `COMMON_*` 을 쓰지 마라.** 이 5개는 `GlobalExceptionHandler` 만 발급한다.
> 구현 위치: `global/presentation/api/common/GlobalExceptionHandler.java`
>
> 📌 **§3 전체가 미확정 상태**라 이 표도 팀 합의가 필요하다.
> 다만 **코드에는 이미 5개가 다 나가고 있으므로**, 문서가 현실을 따라가도록 먼저 적어둔다.

### 3-4. 페이징 🔴

- 파라미터: `page` (0-base), `size`, `sort`

### 확정해야 할 것

- [x] `2026-08-04` **응답 포맷 확정** — `{httpStatus, message, data}` / `{httpStatus, message, code}` (실제 코드 = 계약, §3-1)
- [x] `2026-08-03` ~~공통 응답 래퍼 클래스를 만들 것인가~~ → **`ApiResponse<T>` 로 구현됨.**
      ⚠️ Swagger 의 `io.swagger.v3.oas.annotations.responses.ApiResponse` 와 **이름이 충돌**한다
      (컨트롤러에서 완전정규명을 써야 한다). `ApiResult` 등으로 개명 검토 필요 (팀 결정 대기)
- [x] `2026-08-04` **에러 코드 표기 = 의미식 확정** (§3-3) — 번호식 폐기
- [ ] 에러 코드 도메인 접두어 배분
- [ ] 🔴 **`COMMON_*` 5종을 공통 명세에 반영** (§3-3) — 코드에는 이미 나가고 있는데 명세에 없다.
      프론트가 폴백 분기를 못 짠다
- [ ] 위 3-1 ~ 3-4 확정 후 🔴 표시 제거

---

## 4. 변경 이력

| 날짜 | 변경 내용 | 담당 |
|------|----------|------|
| 2026-08-04 | **노션 계약 폐지** — `.ai/api/*.md` 를 명세 단일 기준으로. §1 라이프사이클 재작성, 상태 게이트(`📝초안`/`✅확정`) 삭제 | 김동현 |
| 2026-07-28 | 초안 생성 — 계약 보호 원칙 + 명세 관리 방식 | 김동현 |
