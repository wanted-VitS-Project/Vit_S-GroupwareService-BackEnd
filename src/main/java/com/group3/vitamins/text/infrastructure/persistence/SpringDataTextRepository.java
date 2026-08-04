package com.group3.vitamins.text.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataTextRepository extends JpaRepository<TextJpaEntity, Long> {

    /**
     * deleted_at 조건을 UPDATE 문 자체에 걸어서, "확인 후 쓰기" 2단계 사이의 틈을 없앤다.
     * 이미 삭제된 행이면 0을 반환한다. clearAutomatically 로 벌크 업데이트 후 영속성 컨텍스트의
     * 캐시된(오래된) 엔티티를 지워서, 이어지는 조회가 DB 최신값을 다시 읽게 한다.
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TextJpaEntity t SET t.content = :content, t.updatedAt = CURRENT_TIMESTAMP "
            + "WHERE t.txtId = :txtId AND t.deletedAt IS NULL")
    int updateContentIfActive(@Param("txtId") Long txtId, @Param("content") String content);
}
