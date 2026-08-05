package com.group3.vitamins.project.block.application.port;

import com.group3.vitamins.project.block.application.result.BlockDetail;
import com.group3.vitamins.project.block.domain.model.BlockType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * 타입별 상세 테이블을 다루는 확장점. 타입 추가는 이 인터페이스를 구현한 어댑터를 추가하는 것으로 끝난다.
 * 블록 도메인은 구현체를 컴파일 타임에 알지 않는다 — 스프링이 주입한 목록에서 supportedType 으로 고른다.
 */
public interface BlockDetailPort {

    /** 이 어댑터가 담당하는 블록 타입. 중복되면 기동 시점에 실패한다. */
    BlockType supportedType();

    /**
     * 상세 빈 행을 만들고 그 PK 를 돌려준다 (생성 3단계 중 ②). 내용은 타입별 수정 API 가 나중에 채운다.
     * 상세 행이 없는 타입(FILE·PERFORMANCE_VIEW·TAX_INVOICE_VIEW)은 null 을 돌려주고
     * 호출자가 block.type_id 를 NULL 로 둔다.
     */
    Long createDetail(Long blockId, String userId);

    /** 상세 행을 논리 삭제한다. 블록 삭제와 같은 트랜잭션에서 호출된다. */
    void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt);

    /** 상세 PK 목록으로 배치 조회한다. 키는 typeId 다. 목록에 없는 PK 는 응답에서 detail 이 null 이 된다. */
    Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds);
}
