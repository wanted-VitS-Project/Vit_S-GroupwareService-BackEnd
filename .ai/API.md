# 📐 API 규칙

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
| 명세가 있고 상태가 `✅ 확정` 이상 | 그대로 구현한다. 경로·메서드·필드명·타입·상태코드를 **한 글자도 바꾸지 않는다** |
| 명세는 있는데 상태가 `📝 초안` | ⛔ **구현하지 않는다.** 노션 반영 후 `✅` 로 바꿔달라고 요청한다 (§1 상태 게이트) |
| 명세가 레포에 없다 | ⛔ **작성을 멈추고**, 노션 명세를 `.ai/api/{도메인}.md` 로 옮겨달라고 사용자에게 요청한다 |
| 명세에 없는 필드·엔드포인트가 필요해 보인다 | ⛔ **임의로 추가하지 않는다.** 무엇이 왜 필요한지 사용자에게 보고한다 |
| 명세가 틀렸거나 구현이 불가능하다 | ⛔ **코드를 바꾸지 말고 사용자에게 알린다.** 명세 변경은 프론트에 영향을 주므로 팀 합의 사항이다 |
| 명세와 기존 코드가 다르다 | 어느 쪽이 맞는지 **추측하지 말고** 사용자에게 확인한다 |

> 📌 **설계 단계는 예외다.** 사용자가 "API 설계해줘 / 명세 짜줘" 라고 하면 `.ai/api/{도메인}.md` 에
> `📝 초안` 상태로 자유롭게 작성해도 된다. 위 금지 규칙은 **구현** 에 적용된다.

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

## 1. 명세 라이프사이클

명세는 **레포에서 태어나 노션에서 계약이 된다.** AI 를 설계 단계부터 활용하기 때문이다.

```
① 설계        AI 와 함께 .ai/api/{도메인}.md 에 작성      → 📝 초안
② 팀 합의     노션 API DB 에 반영, 프론트에 공유          → ✅ 확정  ← 여기서 계약 성립
③ 구현        AI 가 명세대로 코드 작성                    → 🚧 구현중
④ 완료        Swagger 로 구현 결과 확인                   → ✔️ 완료
```

### 🔑 어느 쪽이 맞는가 — 상태로 판단한다

파일 단위로 "노션이 원본"이라고 정하면 이 흐름과 맞지 않는다. **엔드포인트마다 상태가 다르기 때문**이다.
기준은 하나다:

> **노션에 올라간 순간부터 노션이 계약이다. 그 전에는 레포 초안이 최신이다.**

| 상태 | 의미 | 최신 위치 | 프론트가 보고 있나 |
|------|------|----------|------------------|
| 📝 초안 | 레포에서 작성 중. 노션 미반영 | **레포** | ❌ 아니오 |
| ✅ 확정 | 노션 반영 완료. 계약 성립 | **노션** | ✅ 예 |
| 🚧 구현중 | 코드 작성 중 | 노션 | ✅ 예 |
| ✔️ 완료 | 구현 완료 | 노션 | ✅ 예 |

`📝 초안` 은 아직 계약이 아니므로 자유롭게 고쳐도 된다.
`✅ 확정` 부터는 §0 의 이탈 금지 규칙이 전면 적용된다.

### ⛔ 상태 게이트 — 구현 착수 조건

> **`📝 초안` 상태인 엔드포인트는 구현하지 않는다.**

AI 가 API 구현을 요청받았을 때 해당 엔드포인트가 `📝 초안` 이면:

1. 구현을 시작하지 말고
2. **"노션에 반영하고 상태를 `✅ 확정` 으로 바꿔달라"** 고 사용자에게 요청한다

이유: 노션에 없는 API 를 먼저 구현하면 프론트는 그 API 의 존재 자체를 모른다.
나중에 명세가 바뀌면 이미 짠 코드를 버리게 된다. **노션 반영이 구현보다 먼저다.**

### 방향별 규칙

| 방향 | 언제 | 규칙 |
|------|------|------|
| **레포 → 노션** | 새 API 설계 시 | AI 와 초안 작성 → 사람이 노션에 반영 → 상태를 `✅` 로 변경 |
| **노션 → 레포** | 남이 만든 명세를 구현할 때 | 노션에서 해당 도메인만 복사 → 상태 `✅` 로 기록 |
| **노션 수정 발생** | 확정 후 명세가 바뀜 | 노션이 먼저 바뀌고, 레포 사본을 다시 맞춘다. 반대 방향 금지 |

### 왜 전체를 레포에 두지 않나

**작업 중인 도메인만** 둔다. 이전 모듈 프로젝트에서 노션과 1:1 미러한 명세 파일이
**2,025줄 / 72KB** 까지 커져 "작업 전 반드시 읽어라" 규칙이 물리적으로 지켜질 수 없게 됐다.

부분 사본이면 오래된 명세가 애초에 존재하지 않아 AI 가 옛 명세로 잘못 구현할 일이 없다.

### ⚠️ 이 방식의 유일한 약점

**노션 반영을 사람이 해야 한다.** 까먹으면 프론트가 모르는 API 가 생긴다.

그래서 PR 템플릿에 **"API 명세를 노션에 반영했다"** 체크 항목을 넣었다.
API 를 건드린 PR 은 이 항목을 반드시 확인한다.

---

## 2. Swagger (springdoc-openapi)

| 항목 | 내용 |
|------|------|
| 역할 | 구현 결과 확인 + 프론트가 직접 호출해볼 UI |
| **역할이 아닌 것** | 명세의 원본. 명세는 노션에 있다 |
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

> Swagger 에 나온 내용과 노션 명세가 다르면 **구현이 틀린 것**이다. 명세를 고치지 말고 구현을 고친다.

### 2-1. 도입 설정 (완료)

| 파일 | 내용 |
|------|------|
| `build.gradle` | `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17` |
| `application.yaml` | UI/JSON 경로, 정렬, `paths-to-match: /api/**` |
| `application-prod.yaml` | 운영 비활성화 |
| `config/OpenApiConfig.java` | 문서 제목·설명·버전 |

> ⚠️ **버전 주의**: `3.0.x` 는 Spring Boot 4 용이다. 우리는 Boot 3.5.16 이므로 **2.8.x** 를 쓴다.
> 버전을 올릴 때 Boot 버전과의 대응을 반드시 확인할 것.

### 2-2. ⚠️ 운영 환경에서는 끈다 (적용 완료)

Swagger UI 는 **API 전체 구조를 그대로 노출한다.** 운영에 열려 있으면 공격자에게 지도를 주는 것과 같다.

`application-prod.yaml` 에서 비활성화했고, **실제로 404 를 반환하는 것까지 검증**했다 (2026-07-28).

```yaml
# application-prod.yaml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

`local` / `dev` 프로필에서만 열어둔다. **운영 배포 시 `SPRING_PROFILES_ACTIVE=prod` 설정이 필수**이며,
이를 빠뜨리면 Swagger 가 그대로 열린다. 배포 설정에서 반드시 확인할 것.

### 2-3. 어노테이션 규칙

> **원칙: 어노테이션은 노션 명세를 그대로 옮긴 것이어야 한다.**
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
- ❌ 운영 프로필에서 Swagger UI 열어두기
- ❌ 어노테이션만 고치고 실제 구현은 그대로 두기 (문서와 동작 불일치)

---

## 3. 공통 컨벤션 🔴 미확정 — 팀 합의 필요

> ⚠️ 도메인 담당자별로 명세를 따로 작성했다면 **응답 포맷이 제각각일 가능성이 높다.**
> 노션 명세들을 대조해 실제로 통일돼 있는지 먼저 확인할 것. 통일돼 있다면 그 형태를 아래에 옮겨 적고,
> 제각각이라면 지금 통일해야 한다. (나중에 고치면 프론트 코드까지 전부 바뀐다)

### 3-1. 응답 포맷 🔴

```jsonc
// 성공
{ "status": 200, "message": "...", "data": { } }

// 실패
{ "status": 400, "message": "...", "code": "AUTH_001" }
```

### 3-2. URL 규칙 🔴

- 버전 프리픽스: `/api/v1`
- 리소스는 **복수형 명사**: `/api/v1/users`
- 행위는 URL 이 아니라 HTTP 메서드로 표현
- 케이스: kebab-case (`/api/v1/user-agreements`)

### 3-3. 에러 코드 체계 🔴

- 형식: `{도메인}_{일련번호}` — 예) `AUTH_001`, `USER_014`
- 도메인 접두어는 담당자별로 선점해 충돌을 막는다

### 3-4. 페이징 🔴

- 파라미터: `page` (0-base), `size`, `sort`

### 확정해야 할 것

- [ ] 노션 명세들의 응답 포맷이 실제로 통일돼 있는지 대조
- [ ] 공통 응답 래퍼 클래스(`ApiResponse<T>` 등) 를 만들 것인가 / 누가 만드는가
- [ ] 에러 코드 도메인 접두어 배분
- [ ] 위 3-1 ~ 3-4 확정 후 🔴 표시 제거

---

## 4. 변경 이력

| 날짜 | 변경 내용 | 담당 |
|------|----------|------|
| 2026-07-28 | 초안 생성 — 계약 보호 원칙 + 명세 관리 방식 | 김동현 |
