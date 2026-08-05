package com.group3.vitamins.checklist.infrastructure.blockdetail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** 블록 조회용 checklist 항목 배치 조회. 쓰기는 JPA(ChecklistBlockRepository) 가 담당한다. */
@Mapper
public interface ChecklistDetailMapper {

    List<ChecklistItemRow> findItemsByChkBlockIds(@Param("chkBlockIds") Collection<Long> chkBlockIds);
}
