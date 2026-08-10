package com.group3.vitamins.account.application.port;

import com.group3.vitamins.account.application.result.AccountTargetRow;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 계정 관리 화면용 조회 아웃바운드 포트. <b>account 와 employee 를 가로지르므로</b> MyBatis 로 읽는다
 * (실제 조회는 {@code infrastructure/adapter} 어댑터가 처리한다). 쓰기는 {@code AccountEntity}(JPA)가 맡는다.
 */
public interface AccountQueryPort {

    /**
     * role · status 변경 대상 1건의 검증용 스냅샷.
     * 계정 존재({@code ACC_NOT_FOUND})와 시스템 계정 여부({@code ACC_SYSTEM_ACCOUNT_NOT_ALLOWED})를 한 번에 판정한다.
     */
    Optional<AccountTargetRow> findTarget(String userId, Long companyId);

    /**
     * 비밀번호 재설정 대상들의 스냅샷을 한 번에 조회한다(회사 범위).
     * 반환 개수가 요청 개수보다 적으면 존재하지 않거나 타사 사번이 섞인 것 → 전체 거부({@code ACC_NOT_FOUND}).
     */
    List<AccountTargetRow> findTargets(Collection<String> userIds, Long companyId);
}
