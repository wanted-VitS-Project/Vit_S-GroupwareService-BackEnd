package com.group3.vitamins.employeegroup.infrastructure.adapter;

import com.group3.vitamins.employeegroup.application.result.EmployeeRefRow;
import com.group3.vitamins.employeegroup.application.result.GroupListRow;
import com.group3.vitamins.employeegroup.application.result.MemberRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 그룹 목록·단건 조회 (MyBatis · 조회 전용). SQL 은
 * {@code src/main/resources/mapper/employeegroup/EmployeeGroupQueryMapper.xml} 에 둔다(팀 컨벤션).
 */
@Mapper
public interface EmployeeGroupQueryMapper {

    List<GroupListRow> findGroups(@Param("keyword") String keyword, @Param("companyId") Long companyId);

    GroupListRow findGroup(@Param("groupId") Long groupId, @Param("companyId") Long companyId);

    List<MemberRow> findMembers(@Param("groupId") Long groupId);

    int countMembers(@Param("groupId") Long groupId);

    List<EmployeeRefRow> findEmployeeRefs(
            @Param("userIds") Collection<String> userIds, @Param("companyId") Long companyId);
}
