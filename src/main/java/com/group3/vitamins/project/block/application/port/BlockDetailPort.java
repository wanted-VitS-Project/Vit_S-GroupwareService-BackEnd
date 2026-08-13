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
     * 상세 행이 없는 타입(FILE·TAX_INVOICE_VIEW)은 null 을 돌려주고
     * 호출자가 block.type_id 를 NULL 로 둔다.
     */
    Long createDetail(Long blockId);

    /**
     * 이 상세를 <b>블록 직접 삭제</b>로 없애도 되는지 판정한다 (DEL-016·DEL-018).
     *
     * <p>기본은 no-op — 대부분의 타입은 상태 개념이 없어 언제든 삭제할 수 있다. 되물어야 하는 타입만
     * 오버라이드한다(현재 APPROVAL 뿐).
     *
     * <p>{@code blockTitle} 은 안내 문구용이다 — 상세가 자기 제목을 못 찾았을 때 대체어로 쓴다
     * ({@code deleteDetail} 과 같은 이유로 넘긴다).
     *
     * <p>{@code confirmed} 는 <b>사용자가 부작용을 이미 확인했다</b>는 뜻이다. 오버라이드하는 어댑터는
     * {@code false} 일 때만 409 를 던지고, {@code true} 면 통과시켜야 한다 — 막는 것이 목적이 아니라
     * <b>모르고 지우는 것을 막는 것</b>이 목적이다. 확인의 의미는 타입이 정한다(결재는 "결재 취소 동의").
     *
     * <p>⛔ <b>cascade 경로(스텝 삭제)는 이 메서드를 부르지 않는다</b> (DEL-017). 거기서 막으면 결재 1건
     * 때문에 스텝 삭제 전체가 롤백돼, 폐기된 BLK-008 잠금과 같은 실패로 돌아간다.
     */
    default void assertDeletable(Long typeId, String blockTitle, boolean confirmed) {
    }

    /** 상세 행을 논리 삭제한다. 블록 삭제와 같은 트랜잭션에서 호출된다. */
    void deleteDetail(Long typeId, String userId, String blockTitle, LocalDateTime deletedAt);

    /** 상세 PK 목록으로 배치 조회한다. 키는 typeId 다. 목록에 없는 PK 는 응답에서 detail 이 null 이 된다. */
    Map<Long, BlockDetail> loadDetails(Collection<Long> typeIds);
}
