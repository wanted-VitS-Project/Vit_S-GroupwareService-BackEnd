package com.group3.vitamins.account.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 계정 관리(ADMIN) 화면용 조회. <b>account 와 employee 를 가로지르므로</b> MyBatis 를 쓴다.
 *
 * <p>쓰기는 {@link AccountEntity}(JPA)가 담당한다. 역할을 섞지 마라.
 *
 * <p>⛔ SQL 은 애노테이션이 아니라 XML 에 둔다 (팀 MyBatis 컨벤션) —
 * {@code src/main/resources/mapper/account/AccountQueryMapper.xml}.
 */
@Mapper
public interface AccountQueryMapper {

    /**
     * role · status 변경 대상 1건의 검증용 스냅샷.
     *
     * <p>계정 존재({@code ACC_NOT_FOUND})와 시스템 계정 여부({@code ACC_SYSTEM_ACCOUNT_NOT_ALLOWED})를
     * 한 번에 판정하기 위한 조회다. 실제 변경은 JPA 로 로드해 수행한다.
     */
    Optional<AccountTargetRow> findTarget(@Param("userId") String userId);

    /**
     * 비밀번호 재설정 대상들의 스냅샷을 한 번에 조회한다.
     *
     * <p>반환 개수가 요청 개수보다 적으면 존재하지 않는 사번이 섞인 것이다 →
     * 명세상 <b>전체 거부</b>({@code ACC_NOT_FOUND})다. ADMIN 포함 여부도 {@code role} 로 판정한다.
     */
    List<AccountTargetRow> findTargets(@Param("userIds") Collection<String> userIds);
}
