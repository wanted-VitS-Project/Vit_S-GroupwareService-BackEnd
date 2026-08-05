package com.group3.vitamins.text.infrastructure.blockdetail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** 블록 조회용 text 배치 조회. 쓰기는 JPA(TextRepository) 가 담당한다. */
@Mapper
public interface TextDetailMapper {

    List<TextDetailRow> findByTxtIds(@Param("txtIds") Collection<Long> txtIds);
}
