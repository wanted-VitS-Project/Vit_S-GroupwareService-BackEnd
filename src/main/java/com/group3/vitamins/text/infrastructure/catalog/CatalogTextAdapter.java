package com.group3.vitamins.text.infrastructure.catalog;

import com.group3.vitamins.text.domain.model.Text;
import com.group3.vitamins.text.domain.repository.TextRepository;
import com.group3.vitamins.text.infrastructure.persistence.SpringDataTextRepository;
import com.group3.vitamins.text.infrastructure.persistence.TextJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * text 행 생성은 Block 도메인(동훈님) 쪽에서 처리한다 — 여기서는 기존 행을 찾아 수정만 한다.
 */
@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CatalogTextAdapter implements TextRepository {

    private final SpringDataTextRepository springDataTextRepository;

    @Override
    @Transactional
    public Text save(Text text) {
        TextJpaEntity entity = springDataTextRepository.findById(text.getTxtId())
                .orElseThrow(() -> new IllegalStateException("text not found: " + text.getTxtId()));
        entity.applyContent(text.getContent());
        entity.applyDeletedAt(text.getDeletedAt());

        return toDomain(entity);
    }

    @Override
    public Optional<Text> findActiveByTxtId(Long txtId) {
        return springDataTextRepository.findById(txtId)
                .filter(entity -> entity.getDeletedAt() == null)
                .map(this::toDomain);
    }

    private Text toDomain(TextJpaEntity entity) {
        return Text.reconstruct(
                entity.getTxtId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
