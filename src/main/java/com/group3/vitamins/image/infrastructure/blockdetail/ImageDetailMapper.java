package com.group3.vitamins.image.infrastructure.blockdetail;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/** 블록 조회용 — 블록당 order_index가 가장 작은 이미지 1장만 배치 조회한다. 쓰기는 JPA(ImageBlockRepository)가 담당한다. */
@Mapper
public interface ImageDetailMapper {

    List<ImageDetailRow> findFirstImagesByImgBlockIds(@Param("imgBlockIds") Collection<Long> imgBlockIds);
}
