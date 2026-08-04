# 📂 도메인 문서 인덱스

**최종 업데이트**
- 2026-08-04 — Flyway 마이그레이션 컨벤션 추가
- 2026-08-03 — **블록 10종**(`BID_NOTICE` 신설) · 다형성 규약 · 삭제 정책 · 입찰·비타메이트 문서 제거
- 2026-08-03 — 비타메이트 AI 문서 추가 · 입찰관리 노션 정리본 반영
- 2026-08-01 — 도메인 문서 인덱스 작성

> 이 폴더가 **도메인 문서의 집**이다. AI 는 작업 전에 여기서 해당 문서를 찾아 읽는다.
> 규칙은 `AGENTS.md`, 운영 문서는 `.ai/` 루트, **도메인 지식은 여기.**

---

## 🗺️ 구조

```
.ai/docs/
├── README.md      이 파일
├── global/        전 도메인 공통 — 어느 도메인을 하든 걸린다
│   ├── PRODUCT.md      제품 정의 (무엇을 왜 만드나 · 범위 밖)
│   ├── PAGE.md         페이지 체계 (탭 8개 · 화면 P-00~P-70 역할·담당)
│   ├── PERMISSION.md   권한 체계 (3층 판정 · ADMIN/MASTER/MEMBER 차이)
│   ├── BLOCK.md        블록 정보 (공통 규칙 · 배치 · 10종 카탈로그)
│   ├── FLYWAY.md       DB 마이그레이션 컨벤션
│   └── USECASE.md      전 과정 흐름 (공고 → 실적)
└── domain/        도메인별 구현 계약 + 유스케이스
    ├── ERD.md            ⭐ 전 도메인 DDL 정본 (51테이블)     (동훈)
    ├── ERD-CLOUD-DIFF.md ERD Cloud 수정 체크리스트 (일회성)   (동훈)
    ├── HANDOFF.md        인계·미결 (도메인 밖 / 팀 합의 필요) (동훈)
    ├── 프로젝트/   ERD · PRJ-V1 · PRJ-V1-API(+DETAIL/FLOW)  (동훈)
    ├── 재무관리/   ERD · PAY/TAX/STL-V1 · FIN-V1-API(+DETAIL/FLOW)  (동훈)
    └── 관리자/     BCT-V1 · BCT-V1-USECASE · BCT-V1-API      (동훈)
```

### 🗄️ 전체 스키마 — 먼저 볼 것

| 문서 | 역할 |
|------|------|
| **[ERD.md](domain/ERD.md)** | ⭐ **전 도메인 DDL 정본 (51테이블).** 블록 **다형성 규약**(§0) · **삭제 정책**(§0-5) · `ALTER` 구문. **도메인 문서와 어긋나면 이게 이긴다** |
| [ERD-CLOUD-DIFF.md](domain/ERD-CLOUD-DIFF.md) | 🔧 ERD Cloud 를 정본에 맞추는 **일회성 체크리스트.** 다 고치면 삭제한다 |
| [HANDOFF.md](domain/HANDOFF.md) | 도메인 밖 · 팀 합의가 필요한 인계 항목 |

---

## 📘 global — 먼저 읽어야 하는 것

| 문서 | 역할 | 읽어야 하는 시점 |
|------|------|-----------------|
| [PRODUCT.md](global/PRODUCT.md) | 제품 정의 · **범위 밖** | 기능 범위를 다툴 때 |
| [PERMISSION.md](global/PERMISSION.md) | 3층 판정 · role 차이표 | 권한 검사 코드 · 관리자 화면 ⚠️ **`MASTER` 는 최상위가 아니다** |
| [PAGE.md](global/PAGE.md) | 화면 P-00~P-70 역할 · 백엔드/프론트 담당 | 화면·라우팅 설계 · "이 페이지가 무슨 역할인가" |
| [BLOCK.md](global/BLOCK.md) | 공통 규칙 · 배치 · 10종 카탈로그 · 삭제 잠금 | 블록이 걸린 **모든** 작업 |
| [FLYWAY.md](global/FLYWAY.md) | DB 마이그레이션 컨벤션 · RDS 반영 규칙 | 테이블·컬럼·인덱스·FK·seed 변경 전 |
| [USECASE.md](global/USECASE.md) | 공고 → 실적 전 과정 흐름 | 기능 구현 전 흐름 확인 |

---

## 📁 domain — 구현 계약

각 도메인은 **2종 1세트**다.

| 종류 | 무엇 | 쓰임 |
|------|------|------|
| `{도메인}-V1.md` | 요구사항 + **수용 기준** + 불변식 | **이게 구현 계약이다.** 기준을 통과하면 그 항목은 끝 |
| `{도메인}-V1-USECASE.md` | 테이블 단위 시나리오 목록 | 무엇을 만들어야 하는지 **빠짐없이 세는** 용도 |

> ⛔ **입찰관리 · 비타메이트 문서는 이 레포에 없다** (2026-08-03 제거 · 정현이 별도 관리).
> 단 **스키마는 정본에 있다** — `bid_notice*` 8테이블 · `vitamate_*` 4테이블 · `block.type` 의 `BID_NOTICE`·`AI` → [ERD.md](domain/ERD.md) §3.

### 프로젝트 — 동훈

| 문서 | 범위 |
|------|------|
| [PRJ-V1.md](domain/프로젝트/PRJ-V1.md) · [USECASE](domain/프로젝트/PRJ-V1-USECASE.md) | 프로젝트 > 스테이지 > 스텝 > 블록 **계층** · 진척 · 삭제 |

> 이슈 본체 · 알림은 범위 밖이다. 여기서 정하는 건 **스텝 소유 · 진척률 분모 · `issue_block` 검증** 셋뿐.

### 관리자 (마스터 데이터) — 동훈

| 문서 | 범위 | 화면 |
|------|------|------|
| [BCT-V1.md](domain/관리자/BCT-V1.md) · [USECASE](domain/관리자/BCT-V1-USECASE.md) · [API](domain/관리자/BCT-V1-API.md) | **사업 카테고리** — `ADMIN` 전용 CRUD · 사용 중 삭제 차단 · 선택용 조회 | P-63 |

### 재무관리 — 동훈

| 문서 | 범위 | 화면 |
|------|------|------|
| [PAY-V1.md](domain/재무관리/PAY-V1.md) · [USECASE](domain/재무관리/PAY-V1-USECASE.md) | 입금 직접 등록 · **CSV 수집** · 2단 매칭 · 입금확인 블록(정산 회차) | P-40 · P-41 · P-22A |
| [TAX-V1.md](domain/재무관리/TAX-V1.md) · [USECASE](domain/재무관리/TAX-V1-USECASE.md) | 세금계산서 **수집·조회** — 발행은 홈택스에서 한다 | P-42 · P-22A |
| [STL-V1.md](domain/재무관리/STL-V1.md) · [USECASE](domain/재무관리/STL-V1-USECASE.md) | 정산 현황 **조회 전용** — 회차·지연·미연결 추적 | P-44 · P-25(후순위) |

---

## ⚠️ 도메인 작업 전 알아야 하는 4가지

1. **`MASTER` 는 최상위가 아니다.** 실권 최상위는 `ADMIN` 이다. 부여·회수는 전부 `ADMIN` — [PERMISSION.md](global/PERMISSION.md)
2. **`page_code` 는 2개다** (`BIDDING` · `FINANCE`). `EXECUTIVE` 는 폐기됐다
3. **삭제가 전면 soft 가 아니다** (2026-08-03) — 실물(프로젝트·스텝·블록·파일·원장)은 **soft**, 순수 연결·권한 행은 **하드 `DELETE` 12** 다 (soft 38). `deleted_at` 을 달면 **UNIQUE 를 시체가 점유해 재연결이 죽는다.** 삭제 사실은 `activity_log` 가 갖고 **`activity_log` 자체는 지우지 않는다** → [ERD.md](domain/ERD.md) §0-5.<br/>블록 **잠금은 4개뿐** — 입금확인 블록 · 세금계산서 조회 블록(둘은 스텝까지) · 진행 중 결재 블록 · 결재 대상 파일 블록
4. ⭐ **블록 ↔ 상세는 다형성 양방향 ID 다** — `block.type`(판별자) + `block.type_id`(상세 PK) + `{상세}.block_id`(역방향). **양쪽 다 FK 가 없고** `UNIQUE(block_id)` 만 건다. 생성은 **3단계 한 트랜잭션** → [ERD.md](domain/ERD.md) §0 · [BLOCK.md](global/BLOCK.md)

## 🔗 여기 없는 것

| 찾는 것 | 어디 |
|---------|------|
| API 명세 이탈 방지 규칙 · 도메인별 명세 사본 | [`.ai/API.md`](../API.md) · [`.ai/api/`](../api/) |
| 브랜치 · 커밋 · PR 규칙 | [`.ai/CONVENTION.md`](../CONVENTION.md) |
| 인프라 · CI/CD | [`.ai/INFRA.md`](../INFRA.md) · [`.ai/PIPELINE.md`](../PIPELINE.md) |
| 내 진행 상황 · 완료 기록 | `.ai/local/STATE.md` · `.ai/local/WORKLOG.md` (🔒 gitignore) |
