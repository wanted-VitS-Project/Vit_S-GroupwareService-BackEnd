package com.group3.vitamins.text.domain.repository;

import com.group3.vitamins.text.domain.model.Text;

import java.util.Optional;

/**
 * 텍스트 도메인이 바라보는 영속성 포트. 구현체는 infrastructure/persistence 에 있다.
 */
public interface TextRepository {

    Text save(Text text);

    Optional<Text> findActiveByTxtId(Long txtId);
}
