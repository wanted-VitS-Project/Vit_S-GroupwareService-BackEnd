package com.group3.vitamins.vitamate.fileindex.application.usecase;

import com.group3.vitamins.vitamate.fileindex.application.query.GetVitamateFileIndexSourceQuery;
import com.group3.vitamins.vitamate.fileindex.application.result.VitamateFileIndexSourceResult;

// 파일 인덱싱 소스 정보를 조회하는 유스케이스
public interface GetVitamateFileIndexSourceUseCase {

    VitamateFileIndexSourceResult handle(GetVitamateFileIndexSourceQuery query);
}