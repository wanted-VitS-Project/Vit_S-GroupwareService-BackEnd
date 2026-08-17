package com.group3.vitamins.checklist.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpringDataChecklistRepository extends JpaRepository<ChecklistJpaEntity, Long> {

    /**
     * deleted_at 조건을 UPDATE 문 자체에 걸어서 "확인 후 쓰기" 사이의 틈을 없앤다.
     * 이미 삭제된 행이면 0을 반환한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistJpaEntity c SET c.content = :content, c.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE c.chkId = :chkId AND c.deletedAt IS NULL")
    int updateContentIfActive(@Param("chkId") Long chkId, @Param("content") String content);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistJpaEntity c SET c.completed = :completed, c.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE c.chkId = :chkId AND c.deletedAt IS NULL")
    int updateCompletionIfActive(@Param("chkId") Long chkId, @Param("completed") boolean completed);

    /**
     * 단건 삭제 — 이미 삭제된 행이면 0을 반환한다 (직접 삭제 API의 경합 판별용).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistJpaEntity c SET c.deletedAt = :deletedAt "
            + "WHERE c.chkId = :chkId AND c.deletedAt IS NULL")
    int markDeletedIfActive(@Param("chkId") Long chkId, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 블록 삭제 이벤트로 인한 일괄 삭제 — 그 블록의 활성 항목 전부를 소프트 삭제한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChecklistJpaEntity c SET c.deletedAt = :deletedAt "
            + "WHERE c.chkBlockId = :chkBlockId AND c.deletedAt IS NULL")
    int markAllDeletedByBlockIfActive(@Param("chkBlockId") Long chkBlockId, @Param("deletedAt") LocalDateTime deletedAt);

    /**
     * 전체 개수·완료 개수를 쿼리 1번으로 함께 집계한다 (2026-08-16 — 기존엔 두 카운트를 쿼리 2번으로
     * 따로 호출했다). 집계 쿼리(GROUP BY 없음)라 결과는 항상 정확히 1행이지만, 다중 컬럼 프로젝션을
     * Object[]로 직접 받으면 Spring Data가 "행 목록"으로 감싸서 돌려주므로 List&lt;Object[]&gt;로 받아
     * 그 1행(get(0))을 꺼내 써야 한다 — Object[]로 바로 선언하면 배열 안에 배열이 들어있는 형태가 되어
     * ClassCastException이 난다(2026-08-16, 실제 통합 테스트로 발견).
     *
     * <p>행의 [0]=전체 개수(Long), [1]=완료 개수(Long) — 항목이 0건이면 COUNT(c)는 0, SUM(...)은
     * SQL 표준상 NULL이 나오므로 호출부에서 null 방어가 필요하다.
     */
    @Query("SELECT COUNT(c), SUM(CASE WHEN c.completed = true THEN 1L ELSE 0L END) "
            + "FROM ChecklistJpaEntity c WHERE c.chkBlockId = :chkBlockId AND c.deletedAt IS NULL")
    List<Object[]> countTotalAndCompleted(@Param("chkBlockId") Long chkBlockId);
}
