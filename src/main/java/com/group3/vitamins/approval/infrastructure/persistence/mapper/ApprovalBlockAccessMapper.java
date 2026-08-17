package com.group3.vitamins.approval.infrastructure.persistence.mapper;

import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalBlockPermissionRow;
import com.group3.vitamins.approval.infrastructure.persistence.row.ApprovalBlockSummaryRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 결재가 블록·스텝·프로젝트에 물어보는 판정 조회 — {@code BlockCatalogPort} 어댑터 전용.
 *
 * <p>블록·프로젝트 테이블은 결재 소유가 아니라 다른 애그리게이트 소유다. 그래서 리포지토리를
 * 여러 번 부르는 대신 MyBatis 조인으로 받는다({@code ARCHITECTURE.md} §2-1 — 다른 애그리게이트
 * 테이블은 {@code application/port} + {@code infrastructure/adapter}).
 *
 * <p>왜 조인으로 바꿨나: 결재 조회 권한 판정 1회가 리포지토리 호출 3종을 거쳐 <b>쿼리 8발</b>을
 * 냈다. {@code block → step} 해석만 2발이고 그게 한 요청 안에서 3번 반복됐다. 파생 쿼리
 * ({@code findByStepIdAndDeletedAtIsNull} 등)라 JPA 1차 캐시로 접히지도 않는다.
 * 선례는 {@code StepAccessQueryMapper}(리포지토리 3개 4조회 → 1조인 스냅샷)다.
 */
@Mapper
public interface ApprovalBlockAccessMapper {

    /** 블록의 소속(스텝·프로젝트) — 삭제된 블록·스텝은 없는 것으로 본다. */
    Optional<ApprovalBlockSummaryRow> findBlockSummary(@Param("blockId") Long blockId);

    /** 블록이 그 회사 소속인지 — 블록·스텝·프로젝트가 모두 살아 있고 프로젝트 회사가 일치할 때만 1행. */
    Optional<Integer> existsBlockInCompany(@Param("blockId") Long blockId,
                                           @Param("companyId") Long companyId);

    /**
     * 블록이 속한 스텝에 대한 요청자의 권한 재료.
     *
     * <p>⚠️ <b>{@code project_member}·{@code step_permission} 조건은 반드시 {@code ON} 절에 있어야
     * 한다.</b> {@code WHERE} 로 내리면 미참여자에게 행이 아예 안 나와서 <b>"블록이 없다"와 "권한이
     * 없다"가 구분되지 않는다.</b> 지금은 둘 다 {@code NONE} 이라 결과가 같지만, 나중에 404/403 을
     * 나누려는 순간 조용히 틀린다(`ARCHITECTURE.md` 가 경고하는 그 함정이다).
     */
    Optional<ApprovalBlockPermissionRow> findBlockPermission(@Param("blockId") Long blockId,
                                                              @Param("userId") String userId);
}
