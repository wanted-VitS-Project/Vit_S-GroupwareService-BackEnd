# 📂 도메인 문서 인덱스

**최종 업데이트**
- 2026-08-16 — 인덱스 정합성 정리: PRODUCT/USECASE 등 실제 경로(`global/`) 반영, `DOMAIN.md`·`ERD*`·`HANDOFF.md` 등 유령 항목 제거, `domain/` 섹션을 실제 폴더(`인사/`·`파일/`)로 재작성 + gitignore(비커밋) 명시, `docs/` 직속 문서·`global/` 누락 4종(`CONCURRENCY`·`DELETE`·`MYBATIS`·`PERSISTENCE`) 등재
- 2026-08-10 — 결재 삭제 전파 1차 핵심 확정·구현, 후속 UX/파기 항목은 `APR-DELETE-DRAFT.md`에 초안 유지
- 2026-08-08 — 삭제 데이터 정리 컨벤션(`CLEANUP.md`) — `global` 하드 딜리트 SPI 등록 + CASCADE 판단 기준 확장

> 이 폴더가 **도메인 문서의 집**이다. AI 는 작업 전에 여기서 해당 문서를 찾아 읽는다.
> 규칙은 `AGENTS.md`, 운영 문서는 `.ai/` 루트, **도메인 지식은 여기.**

---

## 🗺️ 구조

```
.ai/docs/
├── README.md                이 파일
├── DEMO-SCENARIO.md          더미 시나리오 — 비타웨어 무신사 입점(company 2)
├── DEMO-SCENARIO-KDT.md      더미 시나리오 — 비타에듀 K-디지털 심사 신청(company 3, 위와 병존)
├── DEMO-DUMP-PLAYBOOK.md     데모 덤프(`src/main/resources/db/demo/*.sql`) 제작 플레이북
├── PRESENTATION.md           발표용 정리 — 문제→기능→화면→권한→최소범위
├── global/        전 도메인 공통 — 어느 도메인을 하든 걸린다 (11개)
│   ├── PRODUCT.md       제품 정의 (무엇을 왜 만드나 · 범위 밖)
│   ├── PAGE.md          페이지 체계 (탭 8개 · 화면 P-00~P-70 역할·담당)
│   ├── PERMISSION.md    권한 체계 (3층 판정 · ADMIN/MASTER/MEMBER 차이)
│   ├── BLOCK.md         블록 정보 (공통 규칙 · 다형성 규약 · 배치 · 카탈로그)
│   ├── FLYWAY.md        DB 마이그레이션 컨벤션
│   ├── CLEANUP.md       삭제 데이터 정리 컨벤션 (하드 딜리트 스케줄러 SPI + CASCADE 판단 기준)
│   ├── CONCURRENCY.md   동시성 제어 컨벤션
│   ├── DELETE.md        삭제 정책 — 전 도메인 공통 규칙 (soft/hard 판정)
│   ├── MYBATIS.md       MyBatis Mapper·XML 컨벤션
│   ├── PERSISTENCE.md   영속성 계층 공통 규칙
│   └── USECASE.md       전 과정 흐름 (공고 → 실적)
└── domain/        ⚠️ gitignore — 개인 로컬 정본(비커밋). 도메인별 구현 계약 + 유스케이스
    ├── 인사/       HR-V1 · HR-V1-USECASE                        (동훈)
    └── 파일/       COMPANY-DOC-V1 · FILE-V1(+USECASE)            (동훈)
```

> ⚠️ **`domain/` 은 `.gitignore` 로 커밋되지 않는다** — `.ai/local/` 과 동급으로 **개인 로컬 전용**이다.
> 클론 직후에는 이 폴더 자체가 존재하지 않을 수 있다. `AGENTS.md` §3 이 "로컬 문서가 정본"이라 부르는
> 그 로컬은 **이 문서를 쓰는 작성자의 로컬**을 뜻한다 — 다른 팀원 환경엔 다른(또는 아예 없는) `domain/`
> 트리가 있을 수 있으므로, 실제 존재 여부는 각자 로컬에서 확인할 것.

---

## 📘 global — 먼저 읽어야 하는 것

| 문서 | 역할 | 읽어야 하는 시점 |
|------|------|-----------------|
| [PRODUCT.md](global/PRODUCT.md) | 제품 정의 · **범위 밖** | 기능 범위를 다툴 때 |
| [PERMISSION.md](global/PERMISSION.md) | 3층 판정 · role 차이표 | 권한 검사 코드 · 관리자 화면 ⚠️ **`MASTER` 는 최상위가 아니다** |
| [PAGE.md](global/PAGE.md) | 화면 P-00~P-70 역할 · 백엔드/프론트 담당 | 화면·라우팅 설계 · "이 페이지가 무슨 역할인가" |
| [BLOCK.md](global/BLOCK.md) | 공통 규칙 · **다형성 규약** · 배치 · 카탈로그 · 삭제 전파/이동 | 블록이 걸린 **모든** 작업 |
| [FLYWAY.md](global/FLYWAY.md) | DB 마이그레이션 컨벤션 · RDS 반영 규칙 | 테이블·컬럼·인덱스·FK·seed 변경 전 |
| [CLEANUP.md](global/CLEANUP.md) | 삭제 데이터 정리 — 스케줄러(`global`에 하나) vs `CASCADE` 판단 기준 | soft delete되는 테이블을 새로 만들 때, 그 종속 데이터를 어떻게 정리할지 정할 때 |
| [CONCURRENCY.md](global/CONCURRENCY.md) | 동시성 제어 컨벤션 | 동시 수정·경쟁 조건이 걸리는 기능을 설계할 때 |
| [DELETE.md](global/DELETE.md) | 삭제 정책 — soft/hard 판정 3질문 · 규칙 D-1~D-7 | 삭제 기능을 만들거나 "이 테이블은 soft 인가 hard 인가"를 판단할 때 |
| [MYBATIS.md](global/MYBATIS.md) | MyBatis Mapper·XML 컨벤션 | 복잡한 조회 SQL·Mapper 작성할 때 |
| [PERSISTENCE.md](global/PERSISTENCE.md) | 영속성 계층 공통 규칙 | Repository·Entity 설계를 판단할 때 |
| [USECASE.md](global/USECASE.md) | 공고 → 실적 전 과정 흐름 | 기능 구현 전 흐름 확인 |

---

## 📁 domain — 구현 계약 (⚠️ 개인 로컬, 비커밋)

각 도메인은 **2종 1세트**다.

| 종류 | 무엇 | 쓰임 |
|------|------|------|
| `{도메인}-V1.md` | 요구사항 + **수용 기준** + 불변식 | **이게 구현 계약이다.** 기준을 통과하면 그 항목은 끝 |
| `{도메인}-V1-USECASE.md` | 테이블 단위 시나리오 목록 | 무엇을 만들어야 하는지 **빠짐없이 세는** 용도 |

> ⛔ **입찰관리 · 비타메이트 문서는 이 레포에 없다** (2026-08-03 제거 · 정현이 별도 관리).
> 단 **스키마는 Flyway 마이그레이션에 있다** — `bid_notice*`·`vitamate_*` 테이블, `block.type` 의 `AI` →
> `src/main/resources/db/migration`.

### 인사 — 동훈

| 문서 | 범위 |
|------|------|
| [HR-V1.md](domain/인사/HR-V1.md) · [USECASE](domain/인사/HR-V1-USECASE.md) | 인사(HR) 6개 하위 영역 통합 명세 — 계정·부서·직급·직원그룹·페이지권한 |

### 파일 — 동훈

| 문서 | 범위 | 상태 |
|------|------|------|
| [FILE-V1.md](domain/파일/FILE-V1.md) · [USECASE](domain/파일/FILE-V1-USECASE.md) | 파일·파일 버전 — 업로드·버전·귀속(입찰 검토 파일 승격 흐름 포함) | 구현 계약 |
| [COMPANY-DOC-V1.md](domain/파일/COMPANY-DOC-V1.md) | 사내 문서함 — 전사 파일 관리 탭 | ✅ 확정 — 구현 게이트 개방(2026-08-13) |

> 🔖 위 목록은 **이 문서를 쓰는 작성자 로컬의 `domain/` 스냅샷**(2026-08-16 확인)이다. `domain/` 이
> 비커밋이라 다른 팀원 로컬엔 다른 도메인 문서(예: 프로젝트·재무관리·결재·알림·관리자 영역)가 있을 수
> 있다 — 실제 보유 목록은 각자 로컬에서 `find .ai/docs/domain -type f` 로 확인할 것.

---

## ⚠️ 도메인 작업 전 알아야 하는 4가지

1. **`MASTER` 는 최상위가 아니다.** 실권 최상위는 `ADMIN` 이다. 부여·회수는 전부 `ADMIN` — [PERMISSION.md](global/PERMISSION.md)
2. **`page_permission` 행이 생기는(부여 대상) `page_code` 는 2개다** (`BIDDING` · `FINANCE`) — 카탈로그 전체는 6개이고 나머지 4개는 role 로 열려 행이 없다. `EXECUTIVE` 는 폐기됐다
3. **삭제가 전면 soft 가 아니다** (2026-08-03) — 실물(프로젝트·스텝·블록·파일·원장)은 **soft**, 순수 연결·권한 행은 **하드 `DELETE`**다. `deleted_at`을 무조건 달면 UNIQUE를 시체가 점유해 재연결이 죽는다. 삭제 사실은 `activity_log`가 갖고 **`activity_log` 자체는 지우지 않는다** → [DELETE.md](global/DELETE.md).<br/>블록 삭제 잠금은 2026-08-09 폐기됐다. 살릴 블록은 삭제 전에 이동하고, 삭제 대상의 타입별 상세는 같은 트랜잭션에서 정리한다 → [BLOCK.md](global/BLOCK.md) §8
4. ⭐ **블록 ↔ 상세는 다형성 양방향 ID 다** — `block.type`(판별자) + `block.type_id`(상세 PK) + `{상세}.block_id`(역방향). **양쪽 다 FK 가 없고** `UNIQUE(block_id)` 만 건다. 생성은 **3단계 한 트랜잭션** → [BLOCK.md](global/BLOCK.md)

## 🔗 여기 없는 것

| 찾는 것 | 어디 |
|---------|------|
| API 명세 이탈 방지 규칙 · 도메인별 명세 사본 | [`.ai/API.md`](../API.md) · [`.ai/api/`](../api/) |
| 아키텍처 컨벤션 · 헥사고날 계층·패키지 네이밍 | [`.ai/ARCHITECTURE.md`](../ARCHITECTURE.md) |
| 브랜치 · 커밋 · PR 규칙 | [`.ai/CONVENTION.md`](../CONVENTION.md) |
| 인프라 · CI/CD | [`.ai/INFRA.md`](../INFRA.md) · [`.ai/PIPELINE.md`](../PIPELINE.md) |
| 데모 시나리오 · 발표 자료 | [`DEMO-SCENARIO.md`](DEMO-SCENARIO.md) · [`DEMO-SCENARIO-KDT.md`](DEMO-SCENARIO-KDT.md) · [`DEMO-DUMP-PLAYBOOK.md`](DEMO-DUMP-PLAYBOOK.md) · [`PRESENTATION.md`](PRESENTATION.md) |
| 내 진행 상황 · 완료 기록 | `.ai/local/STATE.md` · `.ai/local/WORKLOG.md` (🔒 gitignore) |
