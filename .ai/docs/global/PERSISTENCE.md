# Persistence Convention

**최종 업데이트**: 2026-08-04  
**목적**: JPA와 MyBatis를 함께 사용할 때 역할을 명확히 나누고, 팀원이 SQL 위치와 코드 흐름을 쉽게 찾을 수 있게 한다.

---

## 1. JPA / MyBatis 사용 기준

### 기본 원칙

- JPA와 MyBatis를 함께 사용한다.
- 단순 저장/수정/삭제는 JPA를 기본으로 한다.
- 복잡한 조회 SQL이나 동적 조건 검색은 MyBatis를 사용한다.
- 한 기능 안에서 JPA와 MyBatis를 함께 사용할 수 있지만, 역할은 분리한다.

### JPA를 사용하는 경우

- 단일 테이블 저장
- 단일 테이블 수정
- 단일 테이블 삭제 처리
- 엔티티 상태 변경
- 트랜잭션 안에서 생성/수정 흐름을 관리해야 하는 경우

예시:

- 분석 요청 생성
- 파일 업로드 상태 변경
- 프로젝트 상태 변경
- 이슈 상태 변경

### MyBatis를 사용하는 경우

- 여러 테이블을 조인해서 조회해야 하는 경우
- 검색 조건이 여러 개라 동적 SQL이 필요한 경우
- 목록 조회, 상세 조회, 통계 조회
- 페이징/정렬 조건이 복잡한 경우
- 권한 검증처럼 여러 테이블을 함께 확인해야 하는 경우

예시:

- 프로젝트 목록 조회
- 블록별 파일 목록 조회
- 비타메이트 분석 이력 조회
- 입찰 공고 검색
- 사용자 권한 검증 조회

### 함께 사용할 때 규칙

- 저장은 JPA, 조회/검증은 MyBatis로 나눌 수 있다.
- Service는 JPA와 MyBatis를 직접 의식하지 않고 Port를 통해 호출한다.
- JPA Entity를 MyBatis 결과 타입으로 직접 사용하지 않는다.
- MyBatis 조회 결과는 전용 Row 또는 DTO로 받는다.
- JPA로 저장한 직후 MyBatis로 바로 조회해야 하면 flush 필요 여부를 확인한다.

---

## 2. MyBatis XML 컨벤션

### 기본 원칙

- MyBatis는 복잡한 조회 SQL이나 동적 조건 검색에 사용한다.
- SQL은 XML에 작성하고, Mapper 인터페이스에는 메서드 선언만 둔다.
- `@Select`, `@Insert`, `@Update`, `@Delete`는 사용하지 않는다.

### 파일 위치

```text
Java Mapper
src/main/java/com/group3/vitamins/{domain}/infrastructure/persistence/mapper/{Domain}Mapper.java

XML Mapper
src/main/resources/mapper/{domain}/{Domain}Mapper.xml
```

예시:

```text
src/main/java/com/group3/vitamins/vitamate/infrastructure/persistence/mapper/VitamateAnalysisMapper.java
src/main/resources/mapper/vitamate/VitamateAnalysisMapper.xml
```

### 작성 규칙

- XML의 `namespace`는 Mapper 인터페이스 전체 경로와 같게 작성한다.
- XML의 `id`는 Mapper 메서드명과 같게 작성한다.
- `SELECT *`는 사용하지 않고 필요한 컬럼만 조회한다.
- 파라미터가 2개 이상이면 `@Param`을 사용한다.
- 검색 조건이 많으면 `SearchCondition`, `Criteria` 같은 객체로 묶는다.
- 조회 결과가 복잡하면 전용 Row 클래스를 만든다.

### 동적 SQL 규칙

- 조건 검색은 `<where>`를 사용한다.
- 선택 수정은 `<set>`을 사용한다.
- 리스트 조건은 `<foreach>`를 사용한다.
- 조건 분기는 `<if>` 또는 `<choose>`를 사용한다.

### 금지 사항

- `HashMap` 파라미터 사용 금지
- XML에서 Java static 메서드 호출 금지
- XML에 비즈니스 로직 작성 금지
- `${}` 직접 사용 금지
- `SELECT *` 사용 금지

---

## 3. 이 기준을 쓰는 이유

- JPA는 데이터 변경 흐름을 객체 중심으로 관리하기 좋다.
- MyBatis는 복잡한 조회 SQL을 명확하게 작성하기 좋다.
- SQL 위치가 XML로 통일되어 팀원이 찾기 쉽다.
- Service는 비즈니스 흐름에 집중할 수 있다.
- 파라미터와 조회 결과가 명확해서 유지보수하기 쉽다.
