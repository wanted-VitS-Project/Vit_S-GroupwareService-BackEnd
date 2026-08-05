package com.group3.vitamins.text.infrastructure.blockdetail;

/** text 테이블 조회 행. MyBatis 가 생성자로 매핑한다. */
public record TextDetailRow(Long txtId, String content) {
}