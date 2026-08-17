# MyBatis Convention

**최종 업데이트**: 2026-08-16 ("언제 MyBatis를 쓰는가" 판단 기준을 PERSISTENCE.md §1로 이관 — 이 문서는 MyBatis 문법·컨벤션 정본으로 축약)
**최종 업데이트**: 2026-08-04 (최초 작성)

**목적**: MyBatis SQL 위치와 작성 방식을 통일해서 팀원이 코드를 쉽게 찾고 유지보수할 수 있게 한다.

---

## 1. 언제 MyBatis를 쓰는가

**JPA/MyBatis 중 무엇을 쓸지의 판단 기준(단순 CRUD → JPA, 조인·동적조건·목록/상세/통계 조회 → MyBatis)은
[PERSISTENCE.md](PERSISTENCE.md) §1 이 정본이다.** 이 문서는 MyBatis로 쓰기로 정해진 뒤의
**문법·파일 위치·namespace·금지사항**만 다룬다.

---

## 2. 기본 규칙

- SQL은 XML 파일에 작성한다.
- Mapper 인터페이스에는 메서드 선언만 둔다.
- `@Select`, `@Insert`, `@Update`, `@Delete`는 사용하지 않는다.
- XML의 `namespace`는 Mapper 인터페이스 전체 경로와 같게 작성한다.
- XML의 `id`는 Mapper 메서드명과 같게 작성한다.

---

## 3. 파일 위치

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

---

## 4. Mapper 인터페이스 작성 예시

```java
package com.group3.vitamins.vitamate.infrastructure.persistence.mapper;

import com.group3.vitamins.vitamate.infrastructure.persistence.row.VitamateAnalysisRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 비타메이트 분석 조회 SQL을 호출하는 Mapper
@Mapper
public interface VitamateAnalysisMapper {

    List<VitamateAnalysisRow> findAnalysisHistory(
            @Param("blockId") Long blockId,
            @Param("userId") String userId
    );
}
```

---

## 5. Mapper XML 작성 예시

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.group3.vitamins.vitamate.infrastructure.persistence.mapper.VitamateAnalysisMapper">

    <select id="findAnalysisHistory"
            resultType="com.group3.vitamins.vitamate.infrastructure.persistence.row.VitamateAnalysisRow">
        SELECT
            va.vitamate_analysis_id AS analysisId,
            va.prompt AS prompt,
            va.analysis_status AS analysisStatus,
            va.created_at AS createdAt,
            va.completed_at AS completedAt
        FROM vitamate_analysis va
        JOIN vitamate_block vb
            ON vb.vitamate_block_id = va.vitamate_block_id
        WHERE vb.block_id = #{blockId}
          AND va.deleted_at IS NULL
        ORDER BY va.created_at DESC
    </select>

</mapper>
```

---

## 6. 파라미터 규칙

- 파라미터가 2개 이상이면 `@Param`을 사용한다.
- 검색 조건이 많으면 `SearchCondition`, `Criteria` 같은 객체로 묶는다.
- `HashMap` 파라미터는 사용하지 않는다.

권장:

```java
List<ProjectRow> findProjects(
        @Param("userId") String userId,
        @Param("status") String status
);
```

비권장:

```java
List<ProjectRow> findProjects(Map<String, Object> params);
```

---

## 7. 동적 SQL 규칙

- 조건 검색은 `<where>`를 사용한다.
- 선택 수정은 `<set>`을 사용한다.
- 리스트 조건은 `<foreach>`를 사용한다.
- 조건 분기는 `<if>` 또는 `<choose>`를 사용한다.

예시:

```xml
<where>
    <if test="status != null">
        AND p.status = #{status}
    </if>
    <if test="keyword != null and keyword != ''">
        AND p.name LIKE CONCAT('%', #{keyword}, '%')
    </if>
</where>
```

---

## 8. 금지 사항

- `HashMap` 파라미터 사용 금지
- XML에서 Java static 메서드 호출 금지
- XML에 비즈니스 로직 작성 금지
- `${}` 직접 사용 금지
- `SELECT *` 사용 금지

---

## 9. 왜 이렇게 쓰는가

- SQL은 XML에서 바로 찾을 수 있다.
- Mapper는 어떤 SQL을 호출하는지만 알 수 있다.
- Service는 비즈니스 흐름에 집중할 수 있다.
- 파라미터와 조회 결과가 명확해서 팀원이 이해하기 쉽다.
- 오타와 SQL Injection 위험을 줄일 수 있다.

---

## 10. PR 전 체크리스트

- [ ] Mapper 인터페이스와 XML namespace가 일치한다.
- [ ] Mapper 메서드명과 XML id가 일치한다.
- [ ] SQL을 XML에 작성했다.
- [ ] 파라미터 2개 이상이면 `@Param`을 사용했다.
- [ ] `SELECT *`를 사용하지 않았다.
- [ ] `${}`를 직접 사용하지 않았다.
- [ ] 조회 결과가 복잡하면 Row 클래스를 만들었다.
