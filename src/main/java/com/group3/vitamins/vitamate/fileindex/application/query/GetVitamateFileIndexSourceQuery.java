package com.group3.vitamins.vitamate.fileindex.application.query;

// 파일 인덱싱 대상 파일 버전의 다운로드 정보를 조회하기 위한 query
public record GetVitamateFileIndexSourceQuery(
        Long fileVersionId
) {
}