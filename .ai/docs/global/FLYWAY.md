# Flyway 마이그레이션 컨벤션

**최종 업데이트**: 2026-08-16 (§2 버전 번호 전역 유일 규칙 보강 — 하위 폴더 재귀 스캔 시 실제 충돌 사례 링크 추가)
**최종 업데이트**: 2026-08-04 (최초 작성)
**대상**: DB 스키마 변경, 초기 데이터, 마스터 데이터, RDS 반영 작업

> 이 문서는 AI와 팀원이 DB 변경 작업을 할 때 반드시 따라야 하는 Flyway 기준이다.
> 테이블·컬럼·인덱스·FK·초기 데이터 변경 전 이 문서를 먼저 확인한다.

---

## 1. 기본 원칙

- 모든 DB 스키마 변경은 `src/main/resources/db/migration` 아래 Flyway SQL 파일로 관리한다.
- RDS에 직접 `CREATE`, `ALTER`, `DROP`을 실행하지 않는다.
- 한 번 RDS에 성공 적용된 마이그레이션 파일은 수정하지 않는다.
- 이미 적용된 테이블을 바꿔야 하면 기존 SQL을 수정하지 않고 새 마이그레이션 파일을 추가한다.
- Flyway는 애플리케이션이 DB에 연결되어 실행될 때 자동 적용된다.
  - `compile` 또는 `build`만으로는 DB에 적용되지 않는다.
- 마이그레이션 실패 시 원인을 해결한 뒤 로컬 DB에서 재검증한다.
- `flyway_schema_history`는 Flyway가 관리하는 테이블이므로 직접 생성·수정·삭제하지 않는다.

---

## 2. 파일 위치와 파일명 규칙

기본 경로:

```text
src/main/resources/db/migration
```

도메인 또는 목적별 하위 폴더를 둘 수 있다.

```text
src/main/resources/db/migration/init
src/main/resources/db/migration/bid
src/main/resources/db/migration/vitamate
```

단, 새 하위 폴더를 추가하면 애플리케이션 실행 전 `spring.flyway.locations` 설정과 실제 Flyway 탐지 여부를 확인한다.

파일명 형식:

```text
VyyyyMMddHHmm__description.sql
```

예시:

```text
V202608031000__create_user_tables.sql
V202608031010__create_project_tables.sql
V202608031020__create_payment_tables.sql
```

규칙:

- `V`는 대문자로 작성한다.
- 버전과 설명 사이에는 언더스코어 2개 `__`를 사용한다.
- 설명은 영문 `snake_case`로 작성한다.
- 버전 숫자는 절대 중복되면 안 된다.
- 새 파일의 버전은 현재 `develop` 또는 RDS에 적용된 최신 버전보다 커야 한다.
- 여러 명이 동시에 SQL을 작성할 때는 버전 번호를 사전에 배정한다.
- ⚠️ 버전 번호는 `db/migration` **하위 폴더 전체를 통틀어 전역으로 유일**해야 한다 — Flyway는 하위 폴더를
  재귀적으로 스캔하므로, 폴더가 달라도(`init`/`bid`/`vitamate` 등) 번호가 겹치면 앱이 기동조차 하지 못한다.
  실제 충돌 사례는 [CONCURRENCY.md](CONCURRENCY.md) §7-2 참고.

---

## 3. 작성 규칙

- 테이블 생성은 `CREATE TABLE`을 사용한다.
- 이미 존재할 수 있는 보조 객체가 아니라면 `CREATE TABLE IF NOT EXISTS`는 남용하지 않는다.
- FK가 있는 테이블은 참조 대상 테이블이 먼저 생성된 뒤 생성한다.
- 초기 대량 스키마 적용 시에는 FK 제약조건을 마지막 마이그레이션에서 추가할 수 있다.
- 인덱스, 유니크 제약조건, FK 이름은 명시적으로 작성한다.
- 컬럼명, 테이블명은 ERD와 동일하게 작성한다.
- SQL 파일에는 민감 정보, 실제 계정, 비밀번호, RDS 엔드포인트를 적지 않는다.
- 마스터 데이터 또는 기본 데이터는 스키마 DDL과 분리할 수 있다.
- 중복 가능성이 있는 seed 데이터는 `UNIQUE` 기준을 정한 뒤 `INSERT IGNORE` 또는 `ON DUPLICATE KEY UPDATE`를 검토한다.

---

## 4. 금지 사항

- RDS에서 직접 테이블을 수정하지 않는다.
- 이미 적용된 `V...sql` 파일을 수정하지 않는다.
- `flyway_schema_history` 테이블을 직접 생성·수정·삭제하지 않는다.
- 다른 팀원이 작성한 마이그레이션 버전보다 낮은 버전의 파일을 뒤늦게 추가하지 않는다.
- 같은 버전 번호를 여러 PR에서 사용하지 않는다.
- 전체 테이블을 비우는 `DELETE`, `TRUNCATE`, `DROP`을 일반 마이그레이션에 넣지 않는다.
- `SET FOREIGN_KEY_CHECKS = 0`은 기본적으로 사용하지 않는다.
- 민감 정보, 더미 비밀번호, Access Key, Secret Key, 실제 엔드포인트를 SQL에 작성하지 않는다.

---

## 5. 데이터 변경 규칙

마이그레이션에서 데이터 변경이 필요한 경우, 전체 삭제가 아니라 정확한 대상만 변경한다.

허용 가능 예시:

```sql
UPDATE business_category
SET name = '공공입찰'
WHERE code = 'PUBLIC_BID';
```

신중히 검토해야 하는 예시:

```sql
DELETE FROM business_category
WHERE code = 'OLD_UNUSED_CODE';
```

금지 예시:

```sql
DELETE FROM business_category;
TRUNCATE TABLE business_category;
DROP TABLE business_category;
```

개발용 더미 데이터를 갈아엎어야 한다면 Flyway에 넣지 않고, 운영 DBA가 별도 수동 reset 절차로 관리한다.

---

## 6. 작업 흐름

1. 최신 `develop`을 기준으로 브랜치를 생성한다.
2. 도메인별 마이그레이션 SQL을 작성한다.
3. 로컬 DB에서 애플리케이션을 실행해 Flyway 적용을 확인한다.
4. PR을 올리기 전 최신 `develop`을 다시 반영하고 버전 중복을 확인한다.
5. PR 머지 후 `develop` 환경의 애플리케이션을 실행 또는 배포하여 RDS에 적용한다.
6. RDS의 `flyway_schema_history`와 `SHOW TABLES`로 적용 결과를 확인한다.

---

## 7. PR 전 체크리스트

- [ ] 최신 `develop` 기준으로 작성했다.
- [ ] 마이그레이션 버전 번호가 중복되지 않는다.
- [ ] 이미 RDS에 적용된 SQL 파일을 수정하지 않았다.
- [ ] 새 변경은 새 `V...sql` 파일로 추가했다.
- [ ] FK 참조 대상 테이블이 먼저 생성된다.
- [ ] FK, 인덱스, UNIQUE 이름을 명시했다.
- [ ] ERD의 테이블명·컬럼명·타입과 일치한다.
- [ ] `DROP`, `TRUNCATE`, 전체 `DELETE`가 없다.
- [ ] `flyway_schema_history`를 직접 건드리지 않는다.
- [ ] 민감 정보가 없다.
- [ ] 로컬 애플리케이션 실행으로 Flyway 적용을 확인했다.

---

## 8. AI 작업 규칙

AI는 DB 마이그레이션 작업을 도울 때 아래 순서를 지킨다.

1. 이 문서를 먼저 읽는다.
2. 관련 도메인 문서와 ERD 기준을 확인한다.
3. 기존 마이그레이션 파일과 RDS 적용 이력 충돌 가능성을 먼저 설명한다.
4. 사용자가 명시적으로 요청하기 전까지 파일을 직접 수정하지 않고 대안을 먼저 제시한다.
5. 파일 수정 요청을 받은 경우에도 기존에 적용된 `V...sql` 수정 여부를 먼저 확인한다.
6. 수정 후에는 로컬 적용 방법과 검증 쿼리를 함께 안내한다.
