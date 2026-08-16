# 🏗️ 인프라 인벤토리

**최종 업데이트**: 2026-08-16 (§1~§7 실제 구성 반영 — EC2/ECR/S3/RDS/Redis/모니터링 exporter, 전부 마스킹)
**최종 업데이트**: 2026-08-12 (§8 신설 — 실시간 알림(SSE)이 리버스 프록시에 요구하는 설정)
**최종 업데이트**: 2026-07-28 (골격 생성)
**관리**: 김동현 (DevOps)

> ⚠️ **이 레포는 PUBLIC 이다.** 아래 마스킹 규칙을 반드시 지킬 것.
> 🔒 실제 값은 `.ai/local/INFRA-real.md` (gitignore) 에 둔다.
>
> 📖 관련: [PIPELINE.md](PIPELINE.md) · [CONVENTION.md](CONVENTION.md)

---

## 0. 작성 규칙

### ✅ 여기에 쓴다

- 리소스의 **역할**과 **구조** (무엇이 왜 있는가)
- 포트 번호, 인스턴스 타입, 볼륨 크기
- 시크릿의 **키 이름** (`DB_PASSWORD`)
- 서비스 간 연결 관계

### ❌ 여기에 쓰지 않는다

| 금지 | 대신 |
|------|------|
| 퍼블릭/프라이빗 IP | `{{EC2_HOST}}` |
| RDS·Redis 엔드포인트 | `{{RDS_ENDPOINT}}` |
| 도메인 | `{{DOMAIN}}` |
| AWS 계정 ID · ARN | `{{AWS_ACCOUNT_ID}}` |
| 키페어 이름 | `{{KEY_PAIR}}` |
| 시크릿 **값** | 키 이름만 기록 |

> **AI에게**: 이 문서에 없는 리소스 이름·엔드포인트를 추측해서 지어내지 마라.
> 없으면 "INFRA.md에 없음 — 확인 필요" 라고 말할 것.

---

## 1. 전체 구성도

> team05 인프라를 재사용한다(A안) — EC2·ECR·RDS 등 컴퓨트/저장소를 새로 만들지 않고 이전 프로젝트 리소스를 이어 쓴다.
> 리버스 프록시(nginx 등) 앞단 구성은 아직 이 문서에 확정되지 않았다 — §8 의 요구사항만 정해져 있다.

```
[GitHub Actions: deploy.yml] --OIDC(키리스)--> [AWS]
                                                  │
                                          [ECR: team05/backend]
                                                  │ pull
                     ┌────────────────────────────┴───────────────────────────────┐
                     │  EC2 ({{EC2_NAME_TAG}}) — docker compose 로 아래 4개 동거     │
                     │                                                            │
                     │  [Spring :8080] ← .env(S3, SSE) ─ [Redis :6379]            │
                     │  [node-exporter :9100]        [cAdvisor :8081]             │
                     └───────┬──────────────────────────────────────┬────────────┘
                             │                                      │
                     [RDS MySQL {{RDS_ENDPOINT}}]         [S3 {{S3_DEPLOY_PREFIX}}]
                      (DB: vitamins)                       (배포 자산 + .env)
```

배포 실행 순서(러너 → EC2)는 [PIPELINE.md §3 `deploy.yml`](PIPELINE.md#deployyml--spring-배포-ec2) 참고.

---

## 2. 컴퓨트

| 리소스 | 역할 | 비고 | 호스트/식별 변수 | 상태 |
|--------|------|------|------------------|------|
| EC2 (`{{EC2_NAME_TAG}}`) | 애플리케이션 서버. `docker compose` 로 Spring·Redis·모니터링 exporter 를 한 박스에서 기동 | team05 인프라 재사용(같은 인스턴스). 스펙(인스턴스 타입 등)은 INFRA.md 에 미기록 — 확인 필요 | GitHub Variable `EC2_NAME_TAG` (태그 기반으로 배포 시 인스턴스 조회) | ✅ 운영 중 |
| ECR (`team05/backend`) | Spring 이미지 저장소 | team05 리포지토리 재사용. 이전 프로젝트 이미지는 `:latest` 태그로 덮어씀 | env `ECR_REPO`(`deploy.yml` 하드코딩값, 비밀 아님) | ✅ 운영 중 |

---

## 3. 데이터 저장소

| 리소스 | 역할 | 엔진/버전 | 엔드포인트 변수 | 상태 |
|--------|------|----------|----------------|------|
| RDS MySQL (운영) | `prod` 프로필이 붙는 주 데이터베이스 (DB명 `vitamins`) | MySQL 8.0 | GitHub Variable `DB_URL`(JDBC URL 전체) · `DB_USERNAME` · Secret `DB_PASSWORD` | ✅ 운영 중 |
| RDS MySQL (팀 공용, 개발용) | 개발자 로컬(`local` 프로필)이 붙는 팀 공용 RDS — **운영 RDS 와 별도 엔드포인트로 보임**(2026-08-03 결정: 로컬 개발엔 MySQL 을 직접 안 띄우고 이 RDS 를 공유) | MySQL 8.0 | `src/main/resources/application-local.yml`(개인, gitignore) — 값 미기록 | ✅ 사용 중 |
| Redis | Spring Session(`repository-type: indexed`) 저장소. `prod` 는 EC2 위 compose 컨테이너로 Spring 과 동거 | `redis:7-alpine`(배포) · `redis:7.4-alpine`(로컬, digest 고정) | GitHub Variables `REDIS_HOST`/`REDIS_PORT` · Secret `REDIS_PASSWORD` | ✅ 운영 중 |

> 🔖 **Redis 는 아직 ElastiCache 가 아니다.** `application.yml` 주석에 "ElastiCache 는 파라미터 그룹에서
> keyspace 알림(`notify-keyspace-events Egx`)을 켜야 한다"는 메모가 있어 **향후 전환을 염두에 둔 상태**로
> 보이지만, 현재 배포(`deploy/docker-compose.yml`)는 EC2 위 Docker 컨테이너로 직접 띄운다. 전환 여부·시점은
> 확인 필요 — 이 문서에는 실제로 떠 있는 현재 구성만 적는다.

---

## 4. 포트 맵

### 배포(EC2, `deploy/docker-compose.yml`)

| 포트 | 서비스 | 노출 범위 |
|------|--------|----------|
| `8080` | Spring Boot 앱 (`8080:8080`) | EC2 내부 — 앞단 구성(nginx/ALB 등)은 미확정, §8 요구사항만 정해짐 |
| `6379` | Redis (`6379:6379`) | `0.0.0.0` 바인딩. 파이썬 워커(다른 호스트)가 스트림 소비하려 필요. 보안그룹으로 접근 제한 + `requirepass` 이중 방어. ⚠️ PUBLIC 레포라 실제 SG 값은 여기 안 씀 |
| `9100` | node-exporter (Prometheus 스크레이프) | 모니터링 보안그룹만 허용 |
| `8081` | cAdvisor (컨테이너 `8080`→호스트 `8081`) | 모니터링 보안그룹만 허용 |

### 로컬 개발(루트 `docker-compose.yml`)

| 포트 | 서비스 | 노출 범위 |
|------|--------|----------|
| `127.0.0.1:${REDIS_PORT:-6379}` → `6379` | Redis (세션 저장소 전용) | **loopback 고정.** 호스트 IP 를 생략하면 `0.0.0.0` 에 바인딩돼 같은 네트워크의 아무나 무인증 접근 가능 — 배포 설정과 의도적으로 다름 |

> MySQL 은 로컬 compose 에 없다 — 팀 공용 RDS 를 쓴다(§3).

---

## 5. 시크릿 목록

> ⚠️ **키 이름만.** 값은 절대 기록하지 않는다.
> 실제 값 주입은 GitHub Repo Secrets → 워크플로우 `env:` ([PIPELINE.md](PIPELINE.md) 참고)

### GitHub Secrets (`secrets.*` — 값, Actions 로그 자동 마스킹)

| 키 이름 | 용도 | 상태 |
|---------|------|------|
| `AWS_ROLE_ARN` | OIDC 로 assume 할 배포용 IAM Role | ✅ 등록됨 |
| `DB_PASSWORD` | RDS 접속 비밀번호 | ✅ 등록됨 |
| `REDIS_PASSWORD` | Redis 인증(`--requirepass`) | ✅ 등록됨 |
| `MAIL_PASSWORD` | Gmail 앱 비밀번호(계정 비번 아님, 16자리) | ✅ 등록됨 |
| `SETTLEMENT_ACCOUNT_ENC_KEY` | 정산 계좌 암호화 키(Base64 32바이트) | ✅ 등록됨 |
| `VITAMATE_WORKER_TOKEN` | 비타메이트 연동 토큰 — 파이썬 레포와 **같은 값** | ✅ 등록됨 (선택 기능) |
| `NARA_API_SERVICE_KEY` | 나라장터 API 서비스키 | ✅ 등록됨 (선택 기능) |
| `BIDDING_WORKER_TOKEN` | 입찰 수집 워커 인증 토큰 — 파이썬 레포와 **같은 값** | ✅ 등록됨 (선택 기능) |

### GitHub Variables (`vars.*` — ⚠️ 값이 Actions 로그에서 자동 마스킹되지 않음)

| 키 이름 | 용도 | 상태 |
|---------|------|------|
| `AWS_REGION` | 배포 리전 | ✅ 등록됨 |
| `DB_URL` | RDS JDBC URL 전체 | ✅ 등록됨 |
| `DB_USERNAME` | RDS 계정명 | ✅ 등록됨 |
| `REDIS_HOST` / `REDIS_PORT` | Redis 접속 정보 | ✅ 등록됨 |
| `SESSION_TIMEOUT` | 세션 만료 시간 | ✅ 등록됨 |
| `MAIL_USERNAME` | 발신 Gmail 계정 | ✅ 등록됨 |
| `CORS_ALLOWED_ORIGINS` | 허용 오리진(프론트 도메인) | ✅ 등록됨 |
| `APP_LOGIN_URL` | 메일 링크용 로그인 URL | ✅ 등록됨 |
| `S3_BUCKET_NAME` | 앱 파일 저장용 S3 버킷명(인증은 EC2 인스턴스 role) | ✅ 등록됨 |
| `S3_DEPLOY_PREFIX` | 배포 자산 업로드 경로(버킷명에 AWS 계정ID 포함 — 이 문서엔 안 씀) | ✅ 등록됨 |
| `EC2_NAME_TAG` | 배포 대상 EC2 를 찾는 Name 태그 값 | ✅ 등록됨 |
| `NARA_API_BASE_URL` | 나라장터 API 베이스 URL(선택) | ✅ 등록됨 |
| `BIDDING_COLLECTION_WORKER_ENABLED` | 입찰 수집 워커 활성화 스위치(`true`/`false`, 기본 꺼짐) | ✅ 등록됨 |

> ⚠️ Variables 값은 배포 로그(`echo`, `aws ssm get-command-invocation` 출력 등)에 그대로 찍힐 수 있다.
> 값 자체는 이 문서에 절대 적지 않는다 — 필요하면 GitHub Settings → Secrets and variables → Actions 에서 직접 확인할 것.

---

## 6. 환경 분리

| 환경 | 프로필 | 용도 | 상태 |
|------|--------|------|------|
| local | `local` | 개인 개발 PC. `application.yml` 기본 `active: local` | ✅ |
| dev | `dev` | 통합 개발 서버 | ⬜ **`application-dev.yml` 자체가 존재하지 않는다.** `deploy.yml` 도 `SPRING_PROFILES_ACTIVE=prod` 만 다룬다 — 별도 dev 배포 흐름 없음. 계획 여부 확인 필요 |
| prod | `prod` | 운영. `deploy.yml` 이 고정 주입 | ✅ 운영 중 |

**실제 존재하는 프로필 파일은 3개뿐이다**: `application.yml`, `application-local.yml`, `application-prod.yml`.

**설정 파일 규칙**
- `application.yml` — 공통 설정 + 환경변수 자리(`${DB_PASSWORD}`)만. **커밋함**
- `application-local.yml` — 개인 로컬 값. **gitignore** (`.gitignore:80` — `application-*.yml`)
- `application-prod.yml` — Swagger 비활성화 · 세션 쿠키 `secure: true` 고정 · CORS 기본값 없음(누락 시 기동 실패). **커밋함**
- 운영 값 — GitHub Secrets / Variables → `deploy.yml` 이 `.env` 로 조립 → 컨테이너에 주입 ([PIPELINE.md §3 `deploy.yml`](PIPELINE.md#deployyml--spring-배포-ec2))

---

## 7. 로컬 개발 환경

루트 `docker-compose.yml` — **Redis 전용**이다. 실행: `docker compose up -d` / 중지: `docker compose down`.

| 컨테이너 | 이미지 | 포트 | 용도 |
|----------|--------|------|------|
| `redis` | `redis:7.4-alpine` (**digest 고정** — mutable 태그라 재pull 시 내용이 바뀔 수 있어서) | `127.0.0.1:${REDIS_PORT:-6379}:6379` (loopback 고정) | 세션 저장소(Spring Session, `repository-type: indexed`) |

**필수 기동 옵션**: `--notify-keyspace-events Egx`
`application.yml` 의 `spring.session.redis.repository-type=indexed` 가 keyspace 알림을 요구한다.
빠지면 **에러 없이 조용히** ① 단일 세션 정책(새 로그인이 기존 세션을 못 끊음) ② 계정 잠금·권한 변경 즉시 무효화, 두 기능이 죽는다.
세션 저장소이므로 영속화(`--appendonly no`, `--save ""`)는 끈다 — 재시작으로 날아가도 재로그인하면 된다.

> ⚠️ **MySQL 은 여기 없다.** 개발 DB 는 **팀 공용 RDS** 를 쓴다 (2026-08-03 결정, §3 참고).
> 접속 정보는 커밋하지 않는다 — `src/main/resources/application-local.yml`(gitignore, `.gitignore:80`)에 각자 넣는다.
> ⚠️ 루트 `docker-compose.yml` 주석은 템플릿으로 `application-local.yml.example` 을 언급하지만
> **실제로는 이 파일이 레포에 존재하지 않는다** (2026-08-16 확인) — 필요한 키는 아래 준비물 항목과
> `application.yml` 의 `${...}` 자리로 유추해서 직접 채워야 한다. 템플릿 부재는 별도 확인·정리가 필요하다.

로컬 실행에 필요한 2가지 준비물: ① `SETTLEMENT_ACCOUNT_ENC_KEY`(Base64 32바이트, 직접 생성) ② 로컬 MySQL 에 DB 생성 — 이름은 자기 `application-local.yml` 의 `spring.datasource.url` 과 맞춘다 (배포 예시 `deploy/.env.example` 은 `vitamins`).

---

## 8. 애플리케이션이 리버스 프록시에 요구하는 것

> 앱 코드만으로는 충족되지 않고 **프록시 설정이 함께 맞아야 동작하는** 항목만 적는다.

### 8-1. 실시간 알림(SSE) — 응답 버퍼링을 끄고 유휴 타임아웃을 늘려야 한다

`GET /api/v1/notifications/stream` 은 응답을 닫지 않고 계속 흘려보내는 스트림이다
(명세: [`api/notification.md`](api/notification.md) §5).

| 요구 | 이유 | 안 지키면 |
|---|---|---|
| `proxy_buffering off;` (해당 location) | nginx 가 기본으로 응답을 모아뒀다가 흘린다 | ⚠️ **로컬은 실시간인데 배포하면 여전히 새로고침이 필요하다.** 앱 로그·예외에는 아무것도 안 남아 원인 추적이 어렵다 |
| 유휴 타임아웃 ≥ 60초 (`proxy_read_timeout`) | 앱이 15초마다 하트비트를 보낸다 | 하트비트 주기보다 짧으면 연결이 계속 끊기고 재연결을 반복한다 |
| — | 앱이 응답에 `X-Accel-Buffering: no` 를 실어 보낸다 | nginx 는 이 헤더만으로도 해당 응답의 버퍼링을 끄지만, **ALB 등 앞단이 더 있으면 그쪽엔 안 통한다.** 명시 설정을 권장하는 이유 |

> 🔖 **앱 서버를 2대 이상으로 늘릴 때**: SSE 커넥션은 JVM 메모리에 있어서, 사용자가 붙은 인스턴스와
> 알림을 만든 인스턴스가 다르면 실시간 전송이 안 된다(알림 자체는 DB 에 저장돼 목록 조회로는 보인다).
> Redis Pub/Sub 어댑터로 교체하는 것이 해법이고, 교체 지점은 `NotificationPushPort` 하나다.
> 스티키 세션으로 우회하는 방법도 있지만 알림을 만드는 쪽이 다른 인스턴스라 해결되지 않는다.

---

## 9. 변경 이력

| 날짜 | 변경 내용 | 담당 |
|------|----------|------|
| 2026-08-16 | §1~§7 `{{TODO}}` 골격을 실제 구성(EC2/ECR/S3/RDS/Redis/모니터링 exporter, 포트, 시크릿·Variable 키, 프로필 구조)으로 채움 — 전부 마스킹 유지 | 김동현 |
| 2026-08-12 | §8 신설 — 실시간 알림(SSE)이 리버스 프록시에 요구하는 설정(버퍼링 off · 유휴 타임아웃) | 이강욱 |
| 2026-07-28 | 문서 골격 생성 | 김동현 |
