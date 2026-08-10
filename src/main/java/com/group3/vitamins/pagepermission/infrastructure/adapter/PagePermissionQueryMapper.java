package com.group3.vitamins.pagepermission.infrastructure.adapter;

import com.group3.vitamins.pagepermission.application.result.EmployeeRoleRow;
import com.group3.vitamins.pagepermission.application.result.PageAccessMemberRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 페이지 권한 화면용 조회 MyBatis 매퍼 (§2·§3·§4·§5). page_permission ⋈ employee ⋈ account 를 가로지른다.
 * SQL 은 {@code src/main/resources/mapper/pagepermission/PagePermissionQueryMapper.xml} 에 둔다(팀 컨벤션).
 */
@Mapper
public interface PagePermissionQueryMapper {

    List<PageAccessMemberRow> findGrantedMembers(@Param("pageCode") String pageCode, @Param("companyId") Long companyId);

    List<PageAccessMemberRow> findMasterMembers(@Param("companyId") Long companyId);

    long countGrants(@Param("pageCode") String pageCode, @Param("companyId") Long companyId);

    long countMasters(@Param("companyId") Long companyId);

    LocalDate findLastGrantedDate(@Param("pageCode") String pageCode, @Param("companyId") Long companyId);

    List<EmployeeRoleRow> findEmployeeRoles(@Param("userIds") Collection<String> userIds, @Param("companyId") Long companyId);
}
