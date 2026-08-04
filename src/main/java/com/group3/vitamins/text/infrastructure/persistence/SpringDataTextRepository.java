package com.group3.vitamins.text.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

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

    /**
     * 같은 이유로 삭제도 조건부 UPDATE — 이미 삭제된 행이면 0을 반환한다.
     * 이걸로 중복 삭제 이벤트를 구분하고(0=이미 삭제됨), 동시 삭제 시 최초 삭제 시각이
     * 나중 이벤트로 덮어써지는 것도 막는다(조건에 안 맞으면 애초에 안 씀).
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE TextJpaEntity t SET t.deletedAt = :deletedAt "
            + "WHERE t.txtId = :txtId AND t.deletedAt IS NULL")
    int markDeletedIfActive(@Param("txtId") Long txtId, @Param("deletedAt") LocalDateTime deletedAt);
}
