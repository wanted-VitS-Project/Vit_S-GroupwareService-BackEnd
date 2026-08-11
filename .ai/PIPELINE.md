# ⚙️ CI/CD 파이프라인

**최종 업데이트**: 2026-08-11 (Merge Queue 필수 체크용 `merge_group` 트리거 추가)
**최종 업데이트**: 2026-07-28 (CI · Gitleaks · CodeRabbit 도입)
**관리**: 김동현 (DevOps)

> `.github/workflows/` 를 수정하기 전에 반드시 이 문서를 먼저 읽는다.
> 워크플로우를 추가·변경하면 이 문서도 **같이** 갱신한다.
>
> 📖 관련: [INFRA.md](INFRA.md) · [CONVENTION.md](CONVENTION.md) · [API.md](API.md)

---

## 1. 전체 흐름

```
PR → develop/main       :  CI(빌드+테스트) · Gitleaks · CodeRabbit 리뷰
Merge Queue             :  CI(빌드+테스트) · Gitleaks · Flyway 검증 재실행
push → develop/main     :  CI(빌드+테스트) · Gitleaks
매주 월 09:00 KST        :  Gitleaks 전체 히스토리 스캔
배포                     :  ⬜ 미구축
```

---

## 2. 워크플로우 목록

| 파일 | 트리거 | 하는 일 | 상태 |
|------|--------|---------|------|
| `ci.yml` | PR·Merge Queue·push → `develop`/`main` | JDK17 + Gradle 빌드·테스트 + 테스트 결과 발행 | ✅ |
| `gitleaks.yml` | PR·Merge Queue·push → `develop`/`main`, 주간 cron | 시크릿 스캔 | ✅ |
| `migration.yml` | PR·Merge Queue·push → `develop`/`main` | 실제 MySQL 에 Flyway 적용 검증 | ✅ |
| `dependabot.yml` | **매월** 09:00 KST | 액션·Gradle 의존성 버전 PR | ✅ |
| CodeQL | GitHub 관리 (Default setup) | 코드 취약점 정적 분석 | ✅ |
| 배포 | — | — | ⬜ 미구축 |

### 🔒 액션 버전 고정 정책

모든 GitHub Actions 는 **태그가 아니라 커밋 SHA 로 고정**한다.

```yaml
uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1 # v7.0.1
```

`@v7` 같은 태그는 소유자가 다른 커밋으로 옮길 수 있어 공급망 공격 경로가 된다.
SHA 는 불변이라 이 위험이 사라진다.

수동 갱신 부담은 `.github/dependabot.yml` 이 **월 1회** PR 로 올려 해결한다.
부트캠프 기간(약 4주)과 브랜치 보호(모든 PR 에 승인 1명)를 고려해 월간으로 낮췄다.
운영 기간이 길어지면 `interval: weekly` 로 올린다.
주석의 `# v7.0.1` 은 Dependabot 이 버전을 인식하는 데 쓰이므로 지우지 말 것.

---

## 3. 워크플로우 상세

### `ci.yml` — 빌드 & 테스트

| 항목 | 내용 |
|------|------|
| 트리거 | `pull_request` / `push` → `develop`, `main` |
| 러너 | `ubuntu-latest` |
| JDK | Temurin 17 |
| 캐시 | `gradle/actions/setup-gradle@v6` (Gradle 캐시 자동) |
| 실행 | `./gradlew build --no-daemon` |
| 사용 시크릿 | 없음 |
| 동시성 | 같은 ref 에 새 커밋이 오면 이전 실행 취소 |
| 산출물 | **실패 시에만** 테스트 리포트 업로드 (7일 보관) |

**테스트 결과 발행** — `mikepenz/action-junit-report`

어떤 테스트가 왜 깨졌는지 PR 에서 바로 보이고, 실패한 라인에 코멘트가 달린다.
아티팩트를 내려받아 열어볼 필요가 없다.

> ⚠️ `fail_on_failure: false`, `require_tests: false` 로 두었다.
> 초기에는 테스트가 거의 없어서 이 옵션 없이는 CI 가 계속 빨간불이 된다.
> **테스트가 충분히 쌓이면 `fail_on_failure: true` 로 바꿀 것.**

**테스트 커버리지** — JaCoCo + `madrapps/jacoco-report`

`./gradlew build` 시 `jacocoTestReport` 가 자동으로 따라붙어 리포트를 만든다.

| 산출물 | 용도 |
|---|---|
| `build/reports/jacoco/test/html/index.html` | 사람이 보는 용도 |
| `build/reports/jacoco/test/jacocoTestReport.xml` | CI 가 읽어 PR 코멘트 작성 |

측정 제외: `VitaminSApplication`, `config/**`, `dto/**` — 로직이 없어 측정 의미가 없다.

> ⚠️ **커버리지 %는 품질 지표가 아니다.** 100% 여도 assert 없는 껍데기 테스트일 수 있다.
> 그래서 `min-coverage` 를 0 으로 두어 **정보 제공용**으로만 쓰고, 미달로 머지를 막지 않는다.
> 임계값을 강제하면 숫자를 채우려고 의미 없는 테스트를 쓰게 되는 부작용이 더 크다.
> 테스트가 쌓이면 팀 합의로 임계값을 올린다.
>
> ⚠️ 현재 측정 대상 클래스가 사실상 0개라 리포트가 비어 있다. 그래서 커버리지 스텝에
> `continue-on-error: true` 를 두었다. **도메인 코드가 쌓이면 이 옵션을 제거할 것.**

> 🔴 **현재 테스트가 0개다.** PR #4 에서 `VitaminSApplicationTests` 와 `src/test/resources/`
> 설정이 삭제됐다. `빌드 & 테스트` 체크는 통과하지만 **아무것도 검증하지 않는다.**
>
> ⛔ **따라서 `빌드 & 테스트` 를 필수 체크로 걸어도 지금은 게이트 역할을 하지 못한다.**
> `require_tests: false` 라 테스트가 0개여도 성공하기 때문이다.
> 실질적인 게이트로 만들려면 아래 중 하나가 선행돼야 한다.
>
> 1. 테스트를 복구한다 (권장) → 이후 `require_tests: true` 로 전환
> 2. 테스트가 0개면 실패하도록 `require_tests: true` 를 먼저 켠다
>    (단, 테스트를 복구할 때까지 모든 PR 이 막힌다)
>
> 그동안 실질적으로 동작하는 게이트는 `시크릿 스캔` 과 `마이그레이션 검증` 두 개다.

### `migration.yml` — Flyway 마이그레이션 검증

| 항목 | 내용 |
|------|------|
| 트리거 | PR·push → `develop`/`main` (**paths 필터 없음**) |
| DB | MySQL 8.0 서비스 컨테이너 (CI 전용, 실행 후 폐기, digest 고정) |
| 검사 | ① 버전 중복 ② 실제 적용 ③ 생성 테이블·이력 출력 |

> 🚨 **paths 필터를 추가하지 말 것.**
> 이 잡은 브랜치 보호의 **필수 상태 체크**다. paths 로 걸러져 실행되지 않으면
> GitHub 은 "올 예정인 체크가 도착하지 않음" 으로 판단해 **해당 PR 을 영구히 막는다.**
> 문서만 수정한 PR 도 머지 불가가 된다.
> 같은 이유로 `ci.yml`·`gitleaks.yml` 에도 paths 필터를 두지 않는다.

**왜 필요한가**: 마이그레이션 SQL 오류는 앱을 띄우는 시점에야 드러난다.
배포 중에 발견하면 이미 늦다. PR 단계에서 실제 DB 에 적용해 문법 오류·순서 문제·중복 버전을 잡는다.

> 💡 **Gradle 이 아니라 Flyway CLI(도커)를 쓴다.**
> `build.gradle` 에는 `flyway-core` **라이브러리만** 있고 Flyway **Gradle 플러그인**은 없어서
> `./gradlew flywayMigrate` 태스크가 존재하지 않는다.
> CLI 방식은 앱 빌드가 필요 없어 더 빠르고, 빌드 상태와 무관하게 마이그레이션만 검증한다.

### `gitleaks.yml` — 시크릿 스캔

| 항목 | 내용 |
|------|------|
| 트리거 | `pull_request` / `push` → `develop`, `main` + 매주 월 00:00 UTC |
| 방식 | **gitleaks CLI 바이너리 직접 실행** (v8.30.1) |
| 무결성 | 다운로드 후 **SHA-256 체크섬 검증**, 통과 시에만 압축 해제 |
| 설정 | `.gitleaks.toml` (기본 룰셋 확장 + 경로 한정 허용목록) |
| 범위 | `fetch-depth: 0` — 커밋 히스토리 전체 |
| 산출물 | 탐지 시 SARIF 리포트 업로드 |

> ⚠️ **버전을 올릴 때 `GITLEAKS_SHA256` 도 반드시 함께 갱신**한다.
> 릴리스의 `gitleaks_<ver>_checksums.txt` 에서 `linux_x64` 값을 가져온다.
> 검증 없이 받은 바이너리를 실행하면 변조된 tarball 이 CI 권한으로 실행될 수 있다.

> 💡 **왜 공식 액션을 안 쓰나**: `gitleaks/gitleaks-action` 은 **조직 소유 저장소에
> 라이선스 키(`GITLEAKS_LICENSE`)를 요구**한다. gitleaks CLI 자체는 MIT 라이선스라
> 바이너리를 직접 받아 쓰면 키 없이 동일한 스캔이 된다.

---

## 4. 보안 도구

| 도구 | 역할 | 시점 | 상태 |
|------|------|------|------|
| **GitHub Secret Scanning** | 알려진 시크릿 패턴 탐지 | 저장소 전체 상시 | ✅ |
| **Push Protection** | 시크릿이 포함된 push 를 **거부** | push 시점 | ✅ |
| **Gitleaks** | 커스텀 룰 + 히스토리 스캔 | PR·push·주간 | ✅ |
| **Dependabot 알림** | 의존성 취약점 **알림** | 상시 | ✅ |
| **Dependabot 자동 수정 PR** | 취약점 자동 패치 PR | — | ⬜ 미사용 |
| **Dependabot 버전 업데이트** | 액션·의존성 버전 PR | **매월** 09:00 KST | ✅ |
| **CodeQL** | 코드 취약점 정적 분석(SAST) | PR·push·주간 | ✅ |
| **CodeRabbit** | AI 코드 리뷰 | PR | ✅ |

### ⚠️ Dependabot 이 올리면 안 되는 업그레이드

`.github/dependabot.yml` 의 `ignore` 로 차단한다. **끄지 말 것.**

| 대상 | 차단 이유 |
|------|----------|
| `org.springframework.boot` major | 4.x 로 가면 스타터 명칭이 전부 바뀐다 |
| `springdoc-openapi-starter-webmvc-ui` major | **3.0.x 는 Boot 4 전용.** Boot 3.5 에서 올리면 Swagger 가 깨진다 |
| `org.springframework*` major | Boot 가 버전을 관리하므로 개별 메이저 업그레이드는 충돌을 만든다 |

> 📌 2026-07-29 실제로 Dependabot 이 springdoc 3.0.3 업그레이드 PR 을 올렸다.
> 초기 ignore 규칙에 springdoc 이 빠져 있어 생긴 일이며, 머지했다면 Swagger 가 죽었을 것이다.

> 📌 상태 근거 (2026-07-28 확인):
> ```
> gh api repos/<owner>/<repo> --jq '.security_and_analysis'
>   → secret_scanning: enabled, secret_scanning_push_protection: enabled
> gh api repos/<owner>/<repo>/vulnerability-alerts  → HTTP 204 (활성)
> ```
> 설정을 바꿨다면 위 명령으로 재확인한 뒤 이 표를 갱신할 것. 확인 없이 ✅ 로 두지 말 것.
>
> ⚠️ **Dependabot 은 세 가지가 서로 다르다.** 취약점 *알림*은 켜져 있지만,
> 취약점 *자동 수정 PR*(`dependabot_security_updates`)은 꺼져 있다.
> 버전 업데이트는 `.github/dependabot.yml` 로 별도 운영한다.

### 3중 방어선

```
1차  Push Protection    push 자체를 거부       ← 가장 강력. 저장소에 안 들어감
2차  Gitleaks (PR)      PR 을 빨간불로         ← 커스텀 룰·과거 히스토리 커버
3차  CodeRabbit         리뷰에서 지적          ← 맥락 기반 (설정 파일 평문 등)
```

`Push Protection` 이 1차인 이유는 **저장소에 들어간 뒤에는 이미 늦기 때문**이다.
공개 저장소에 올라간 시크릿은 커밋을 지워도 이미 크롤링됐다고 가정해야 한다.

> 🚨 시크릿이 커밋된 것을 발견하면 **커밋 삭제보다 키 폐기·재발급이 먼저다.**

---

## 5. CodeRabbit

| 항목 | 내용 |
|------|------|
| 설정 | `.coderabbit.yaml` |
| 언어 | 한국어 |
| 프로필 | `assertive` (적극적으로 지적) |
| 자동 리뷰 | 활성 (draft PR 제외, 제목에 WIP/draft 있으면 건너뜀) |

**우선 검사 항목**
1. API 명세 이탈 — `.ai/api/{도메인}.md` 와 경로·메서드·필드·상태코드 일치
2. Swagger 어노테이션 누락 (`@Tag` / `@Operation` / `@ApiResponses` / `@Schema`)
3. PUBLIC 저장소 민감 정보 노출
4. Boot 3.5 ↔ Boot 4 아티팩트 혼용
5. 계층 규칙 (헥사고날 확정 — [ARCHITECTURE.md](ARCHITECTURE.md) 참고)

> 📌 아키텍처가 헥사고날로 확정됐다 (`.ai/ARCHITECTURE.md`, `businesscategory` 기준). `presentation/`·`application/`·`domain/`·`infrastructure/`
> 규칙이 그 문서를 근거로 동작한다.

---

## 6. 시크릿 의존성

> 현재 CI 워크플로우는 **시크릿을 사용하지 않는다.** 배포 워크플로우 구축 시 추가된다.

**시크릿 추가 절차**
1. GitHub → Settings → Secrets and variables → Actions 에 등록
2. [INFRA.md §5](INFRA.md) 시크릿 목록에 **키 이름만** 추가
3. 이 문서의 표에 추가
4. 워크플로우 `env:` 블록에서 참조

---

## 7. 브랜치 보호와의 연계

현재 브랜치 보호에는 **상태 체크 필수화가 걸려 있지 않다.** 보호 규칙을 만들 때
CI 가 아직 없었기 때문이다.

- [ ] CI 가 몇 번 안정적으로 돌아간 것을 확인한 뒤, `develop`/`main` 보호 규칙에
      **required status checks** 로 `빌드 & 테스트`, `시크릿 스캔` 을 추가한다.

이걸 걸어야 "테스트 깨진 PR 은 머지 불가"가 실제로 강제된다.

---

## 8. 배포

⬜ 미구축. 배포 환경 확정 후 작성.

| 환경 | 트리거 | 방식 | 상태 |
|------|--------|------|------|
| dev | — | — | ⬜ |
| prod | — | — | ⬜ |

> ⚠️ 운영 배포 시 **`SPRING_PROFILES_ACTIVE=prod` 설정이 필수**다.
> 빠뜨리면 Swagger UI 가 그대로 열려 API 구조 전체가 노출된다.

---

## 9. 변경 이력

| 날짜 | 변경 내용 | 담당 |
|------|----------|------|
| 2026-07-28 | CI · Gitleaks 워크플로우, CodeRabbit 설정, GitHub 보안 기능 활성화 | 김동현 |
| 2026-07-28 | 문서 골격 생성 | 김동현 |
