# ⚙️ CI/CD 파이프라인

**최종 업데이트**: 2026-08-16 (배포 워크플로우 `deploy.yml` 반영 · gitleaks PR diff 스캔 반영 · 브랜치 보호 필수 체크 3종 실측 반영)
**최종 업데이트**: 2026-08-12 (migration.yml 에 스키마 스모크 스텝 추가 — 엔티티↔스키마 드리프트 검증)
**최종 업데이트**: 2026-08-11 (Merge Queue 필수 체크용 `merge_group` 트리거 추가)
**관리**: 김동현 (DevOps)

> `.github/workflows/` 를 수정하기 전에 반드시 이 문서를 먼저 읽는다.
> 워크플로우를 추가·변경하면 이 문서도 **같이** 갱신한다.
>
> 📖 관련: [INFRA.md](INFRA.md) · [CONVENTION.md](CONVENTION.md) · [API.md](API.md)

---

## §0 TL;DR

- **이 문서가 정하는 것**: `.github/workflows/*` **4개**(CI·시크릿 스캔·마이그레이션 검증·배포)의 트리거·역할·시크릿 사용 규칙과, 브랜치 보호 필수 체크와의 연계. Dependabot(`.github/dependabot.yml` 설정 파일)과 CodeQL(GitHub 관리형 Default setup)은 워크플로우 파일이 아니라 별도 항목으로 다룬다.
- ⚠️ **조용히 깨지는 함정**
  - `ci.yml`·`gitleaks.yml`·`migration.yml` 에 **paths 필터를 넣으면 안 된다** — 셋 다 필수 상태 체크라, 필터로 걸러져 실행 안 되면 GitHub 이 해당 PR 을 영구히 막는다 (§3).
  - gitleaks 는 **PR 이면 diff(base..head) 만, `push`/`merge_group`/주간이면 전체 히스토리**를 스캔한다 — 범위가 이벤트별로 다르다 (§3).

| 섹션 | 내용 |
|---|---|
| §1~2 | 전체 흐름 · 워크플로우 4개(CI·gitleaks·migration·**deploy**) + Dependabot 설정 + CodeQL(관리형) |
| §3 | 워크플로우 상세 — `deploy.yml`(OIDC→ECR→S3→SSM, 2026-08-11 운영 개시) 포함 |
| §4~5 | 보안 도구 현황 · CodeRabbit 설정 |
| §6 | `deploy.yml` 이 쓰는 시크릿 8종(키 이름만) |
| §7 | `main`/`develop` 필수 상태 체크 3종(빌드 & 테스트·시크릿 스캔·마이그레이션 검증) — 이미 걸려 있음 |
| §8 | 배포 — `deploy.yml` 로 운영 중 |

---

## 1. 전체 흐름

```text
PR → develop/main       :  CI(빌드+테스트) · Gitleaks(diff 스캔) · Flyway 검증 · CodeRabbit 리뷰
Merge Queue             :  CI(빌드+테스트) · Gitleaks(전체 히스토리) · Flyway 검증 재실행
push → develop/main     :  CI(빌드+테스트) · Gitleaks(전체 히스토리) · Flyway 검증
push → main             :  (위 3개 통과 후) 배포 — OIDC→ECR→S3→SSM으로 EC2 배포
매주 월 09:00 KST        :  Gitleaks 전체 히스토리 스캔
```

---

## 2. 워크플로우 목록

| 파일 | 트리거 | 하는 일 | 상태 |
|------|--------|---------|------|
| `ci.yml` | PR·Merge Queue·push → `develop`/`main` | JDK17 + Gradle 빌드·테스트 + 테스트 결과 발행 | ✅ |
| `gitleaks.yml` | PR·Merge Queue·push → `develop`/`main`, 주간 cron | 시크릿 스캔 (**PR 은 diff 만, 나머지는 전체 히스토리** — §3 참고) | ✅ |
| `migration.yml` | PR·Merge Queue·push → `develop`/`main` | 실제 MySQL 에 Flyway 적용 검증 + 스키마 스모크(엔티티↔스키마 드리프트) | ✅ |
| `deploy.yml` | `push`→`main`(paths: `src/**`,`build.gradle`,`settings.gradle`,`Dockerfile`,`deploy/**`,워크플로우 자체) · `workflow_dispatch` | OIDC 인증 → ECR 이미지 빌드/푸시 → S3(.env, SSE) → SSM 으로 EC2 `docker compose` 기동 (§8 참고) | ✅ 2026-08-11 운영 개시 |
| `dependabot.yml` | **매월** 09:00 KST | 액션·Gradle 의존성 버전 PR | ✅ |
| CodeQL | GitHub 관리 (Default setup) | 코드 취약점 정적 분석 | ✅ |

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
| DB | MySQL 8.0 + Redis 7 서비스 컨테이너 (CI 전용, 실행 후 폐기, digest 고정) |
| 검사 | ① 파일명 형식 ② 버전 중복 ③ 신규 버전 순서 ④ 실제 적용 ⑤ 생성 테이블·이력 출력 ⑥ **스키마 스모크(엔티티↔스키마 드리프트)** |

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

**🔬 스키마 스모크 (엔티티 ↔ 스키마 드리프트 검증)**

Flyway 적용만으로는 **SQL 이 성공적으로 실행됐는지**만 확인된다 — 엔티티 매핑이 실제 컬럼과
어긋나도(예: 컬럼 추가 마이그레이션이 **빈 파일**이라 컬럼이 안 생겼는데 엔티티는 그 컬럼을
기대) 드러나지 않고 **배포 시점에 크래시**한다. 그래서 마이그레이션이 끝난 이 DB 를 상대로
앱을 `ddl-auto: validate` 로 **실제 기동**한다. Hibernate 가 `EntityManagerFactory` 초기화 시
엔티티↔스키마를 대조하므로, 드리프트가 있으면 컨텍스트 기동이 실패해 이 잡이 빨간불이 된다.

| 항목 | 내용 |
|------|------|
| 빌드 | `./gradlew bootJar` (실행 가능한 부트 jar. devtools 는 `developmentOnly` 라 미포함) |
| 기동 | 방금 마이그레이션한 CI MySQL + Redis 컨테이너를 바라보게 하고 `java -jar` |
| 판정 | 로그에 `Started VitaminSApplication` 이면 통과, 컨텍스트 기동 실패면 실패 |
| 더미 env | `MAIL_*`·`S3_BUCKET_NAME` 은 기동 시 연결하지 않으므로 더미로 충분. `SETTLEMENT_ACCOUNT_ENC_KEY` 는 생성자가 **Base64 32바이트**를 검증하므로 `openssl rand -base64 32` 로 만든다 |

> ⚠️ **Redis 컨테이너가 필요하다.** 세션 저장소가 `repository-type: indexed` 라 기동 시
> `RedisMessageListenerContainer` 가 실제로 연결한다. 없으면 validate 에 도달하기 전에 실패한다.
>
> ⚠️ **콜레이션을 `utf8mb4_0900_ai_ci` 로 고정**한다(Flyway 전 `ALTER DATABASE`). migration SQL 은
> `DEFAULT CHARSET=utf8mb4` 만 지정하고 콜레이션은 DB 기본값을 상속하므로, 운영 RDS(MySQL 8.0)와
> 동일 콜레이션으로 못박아 문자열 비교·정렬·FK 콜레이션이 CI 와 운영에서 다르게 동작하지 않게 한다.
>
> ⚠️ **lazy-init 을 켜지 말 것.** 켜면 JPA 초기화가 지연돼 validate 가 아예 돌지 않는다.

### `gitleaks.yml` — 시크릿 스캔

| 항목 | 내용 |
|------|------|
| 트리거 | `pull_request` / `merge_group` / `push` → `develop`, `main` + 매주 월 00:00 UTC |
| 방식 | **gitleaks CLI 바이너리 직접 실행** (v8.30.1) |
| 무결성 | 다운로드 후 **SHA-256 체크섬 검증**, 통과 시에만 압축 해제 |
| 설정 | `.gitleaks.toml` (기본 룰셋 확장 + 경로 한정 허용목록) |
| 체크아웃 범위 | `fetch-depth: 0` — 커밋 히스토리 전체를 받아온다 |
| **스캔 범위** | **PR 이벤트는 그 PR 이 바꾼 커밋만**(`gitleaks git . --log-opts="${PR_BASE}..${PR_HEAD}"`). **`push`·`merge_group`·주간 cron 은 전체 히스토리**를 스캔한다 |
| 산출물 | 탐지 시 SARIF 리포트 업로드 |

> 🔀 **2026-08-16 변경 (`39f98e7c`)**: 이전에는 PR 도 전체 히스토리를 스캔해서, 과거 커밋이나
> 다른 브랜치에 섞인 오탐(데모 시드 등)이 **자기 변경과 무관한 PR 까지 막았다.**
> 이제 PR 은 `base..head` diff 범위만 보고, 전체 히스토리 검증은 `push`(develop·main)·
> `merge_group`·매주 월요일 스케줄이 담당한다 — 결국 머지 전후로 전체 히스토리도 반드시 훑는다.
>
> ⚠️ **버전을 올릴 때 `GITLEAKS_SHA256` 도 반드시 함께 갱신**한다.
> 릴리스의 `gitleaks_<ver>_checksums.txt` 에서 `linux_x64` 값을 가져온다.
> 검증 없이 받은 바이너리를 실행하면 변조된 tarball 이 CI 권한으로 실행될 수 있다.

> 💡 **왜 공식 액션을 안 쓰나**: `gitleaks/gitleaks-action` 은 **조직 소유 저장소에
> 라이선스 키(`GITLEAKS_LICENSE`)를 요구**한다. gitleaks CLI 자체는 MIT 라이선스라
> 바이너리를 직접 받아 쓰면 키 없이 동일한 스캔이 된다.

### `deploy.yml` — Spring 배포 (EC2)

> 이전 team05 배포 파이프라인 이식판. 2026-08-11 추가, 2026-08-15까지 갱신. **운영 중.**

| 항목 | 내용 |
|------|------|
| 트리거 | `push`→`main`(paths 필터: `src/**`,`build.gradle`,`settings.gradle`,`Dockerfile`,`deploy/**`,워크플로우 자체) · `workflow_dispatch`(수동) |
| 인증 | **OIDC 키리스** — `aws-actions/configure-aws-credentials` 로 `secrets.AWS_ROLE_ARN` 을 assume. 액세스키 없음 |
| 동시성 | 같은 ref 배포는 직렬화(`cancel-in-progress: false`) — 진행 중 배포를 취소하지 않는다 |

**흐름 (5 스텝)**

1. **이미지 빌드 & ECR 푸시** — `docker build` → ECR 에 `:${{ github.sha }}` 와 `:latest` 두 태그로 push
2. **.env 조립** — GitHub Secrets/Variables 값을 러너에서 파일로 조립(필수 키 누락 시 배포 자체를 실패시키는 가드 포함). 시크릿을 SSM 명령 텍스트에 절대 넣지 않기 위한 설계
3. **S3 업로드** — `deploy/` 배포 자산 + 조립한 `.env` 를 S3 에 업로드. `.env` 는 **SSE(서버측 암호화)** 로 저장
4. **SSM 으로 EC2 기동** — `aws ssm send-command` 로 EC2 에 `docker compose pull && docker compose up -d` 실행을 지시(SSM 명령 자체에는 시크릿이 실리지 않는다). 기동 확인은 `localhost:8080` 의 HTTP 응답 유무로 판정
5. **S3 임시 객체 정리** — `always()` 로 실행되어 업로드했던 `.env` 를 S3 에서 삭제(S3 에 남는 임시 객체 제거). 삭제 실패 시 스텝을 **실패시켜** 조용히 넘어가지 않게 한다. ⚠️ 이 삭제는 **S3 쪽만** 지운다 — EC2 의 `/opt/spring/.env` 는 컨테이너 기동에 필요해 남는다. EC2 파일 권한·보존 정책은 INFRA.md 에 별도 정리 필요(확인 필요)

> ⚠️ **기동 확인은 "HTTP 응답 존재" 로만 판정한다.** actuator 를 아직 도입하지 않아 인증 없이
> 200 을 주는 엔드포인트가 없기 때문이다. `/actuator/health` 도입 후 엄밀 검증으로 강화할 예정(백로그).
>
> ⚠️ **`SPRING_PROFILES_ACTIVE=prod` 를 워크플로우가 고정 주입**한다. 로컬 설정과 무관하게
> 배포에서는 항상 `prod` 프로필로 뜬다 — Swagger 노출 방지.

전체 시크릿·Variable 키 이름은 [§6](#6-시크릿-의존성), 배포 대상 인프라 구조는 [INFRA.md](INFRA.md) 참고.

---

## 4. 보안 도구

| 도구 | 역할 | 시점 | 상태 |
|------|------|------|------|
| **GitHub Secret Scanning** | 알려진 시크릿 패턴 탐지 | 저장소 전체 상시 | ✅ |
| **Push Protection** | 시크릿이 포함된 push 를 **거부** | push 시점 | ✅ |
| **Gitleaks** | 커스텀 룰 스캔 (PR: diff / push·merge_group·주간: 전체 히스토리) | PR·push·주간 | ✅ |
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

`ci.yml`·`gitleaks.yml`·`migration.yml` 은 시크릿을 쓰지 않는다(`migration.yml` 은 CI 전용 컨테이너
더미 값만 사용). `deploy.yml` 이 아래 GitHub Secrets 를 쓴다 — **키 이름만** 적는다.

| 키 이름 | 용도 |
|---------|------|
| `AWS_ROLE_ARN` | OIDC 로 assume 할 배포용 IAM Role |
| `DB_PASSWORD` | RDS 접속 비밀번호 |
| `REDIS_PASSWORD` | Redis 인증(`--requirepass`) |
| `MAIL_PASSWORD` | Gmail 앱 비밀번호 |
| `SETTLEMENT_ACCOUNT_ENC_KEY` | 정산 계좌 암호화 키(Base64 32바이트) — 없으면 기동 실패 |
| `VITAMATE_WORKER_TOKEN` | 비타메이트 연동 토큰 (선택) |
| `NARA_API_SERVICE_KEY` | 나라장터 API 서비스키 (선택) |
| `BIDDING_WORKER_TOKEN` | 입찰 수집 워커 인증 토큰 — 파이썬 레포와 **같은 값** 이어야 함 |

값이 URL·호스트명처럼 비밀은 아니지만 배포별로 달라지는 것들은 GitHub **Variables**(`vars.*`)로
따로 관리한다. 전체 Secrets/Variables 키 목록은 [INFRA.md §5](INFRA.md) 참고.

> ⚠️ **Variables 는 Secrets 와 달리 Actions 로그에서 자동 마스킹되지 않는다.**
> `echo`, `aws ssm get-command-invocation` 출력 등에 Variable 값이 그대로 찍힐 수 있다.
> 도메인·호스트명처럼 그 자체로는 "시크릿"이 아니어도 노출을 원치 않는 값은 로그 출력을
> 최소화하도록 워크플로우를 작성할 것.

**시크릿 추가 절차**
1. GitHub → Settings → Secrets and variables → Actions 에 등록
2. [INFRA.md §5](INFRA.md) 시크릿 목록에 **키 이름만** 추가
3. 이 문서의 표에 추가
4. 워크플로우 `env:` 블록에서 참조

---

## 7. 브랜치 보호와의 연계

`main` · `develop` 모두 아래 3개 잡이 **필수 상태 체크(required status checks)** 로 걸려 있다
(2026-08-16 `gh api repos/.../branches/{브랜치}/protection` 실측).

| 필수 체크 이름 | 워크플로우 | 잡 |
|---|---|---|
| `빌드 & 테스트` | `ci.yml` | `build` |
| `시크릿 스캔` | `gitleaks.yml` | `scan` |
| `마이그레이션 검증` | `migration.yml` | `migrate` |

이 3개 중 하나라도 실패하거나 아직 도착하지 않으면 PR 머지 버튼이 열리지 않는다.
(단 `enforce_admins` 설정에 대해서는 [CONVENTION.md §1](CONVENTION.md) 참고.)

---

## 8. 배포

✅ **`deploy.yml` 로 운영 중** (2026-08-11 개시). 상세는 [§3 `deploy.yml`](#deployyml--spring-배포-ec2) 참고.

| 환경 | 트리거 | 방식 | 상태 |
|------|--------|------|------|
| prod | `push`→`main`(경로 필터) · `workflow_dispatch` | OIDC → ECR 빌드/푸시 → S3(.env, SSE) → SSM 으로 EC2 `docker compose` 기동 | ✅ |
| dev | — | 별도 배포 흐름 없음 | ⬜ 미구축 |

**전체 흐름 요약**

```text
push → main (또는 수동 실행)
  → OIDC 로 AWS 자격증명 획득 (액세스키 없음)
  → 이미지 빌드 → ECR 푸시
  → Secrets/Variables → .env 조립 → S3 업로드 (SSE)
  → SSM send-command → EC2 에서 S3 동기화 → docker compose pull/up
  → HTTP 응답으로 기동 확인
  → S3 의 .env 삭제 (at-rest 시크릿 제거)
```

> ⚠️ 운영 배포 시 **`SPRING_PROFILES_ACTIVE=prod` 설정이 필수**다. `deploy.yml` 이 이 값을
> 고정 주입하므로 워크플로우를 거치는 한 자동으로 지켜진다. 빠뜨리면 Swagger UI 가 그대로 열려
> API 구조 전체가 노출된다 — 수동으로 배포 스크립트를 우회할 때만 위험하다.
>
> ⚠️ EC2·ECR·S3 등 배포 대상 인프라의 리소스 이름·포트 구조는 [INFRA.md](INFRA.md) 참고.
> 실제 IP·계정ID·버킷명은 이 문서에도 INFRA.md 에도 쓰지 않는다(GitHub Variables 로만 존재).

---

## 9. 변경 이력

| 날짜 | 변경 내용 | 담당 |
|------|----------|------|
| 2026-08-16 | gitleaks.yml — PR 은 diff(base..head) 만 스캔하도록 범위 분리 (`39f98e7c`) | 김동현 |
| 2026-08-11 | deploy.yml 신설·운영 개시 — OIDC→ECR→S3→SSM EC2 배포 파이프라인 | 김동현 |
| 2026-08-12 | migration.yml 에 스키마 스모크 스텝 추가 (Redis 컨테이너·콜레이션 고정·`validate` 기동) | 김동현 |
| 2026-07-28 | CI · Gitleaks 워크플로우, CodeRabbit 설정, GitHub 보안 기능 활성화 | 김동현 |
