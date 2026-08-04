package com.group3.vitamins.text.infrastructure.catalog;

import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.text.domain.exception.TextErrorCode;
import com.group3.vitamins.text.domain.model.Text;
import com.group3.vitamins.text.domain.repository.TextRepository;
import com.group3.vitamins.text.infrastructure.persistence.SpringDataTextRepository;
import com.group3.vitamins.text.infrastructure.persistence.TextJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * text 행 생성은 Block 도메인(동훈님) 쪽에서 처리한다 — 여기서는 기존 행을 찾아 수정만 한다.
 *
 * <p>updateContent/markDeleted 를 분리한 이유: 하나의 save() 가 content 와 deletedAt 을 같이
 * 덮어쓰면, 수정 흐름이 오래전에 읽어둔 deletedAt(=null)을 그대로 다시 써서 동시에 삭제된
 * 행을 되살릴 수 있다. 각 메서드는 자기 컬럼만 조회 직전에 새로 읽어 갱신한다.
 */
@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class CatalogTextAdapter implements TextRepository {

    private final SpringDataTextRepository springDataTextRepository;

    @Override
    @Transactional
    public Text updateContent(Long txtId, String content) {
        // deleted_at IS NULL 조건을 UPDATE 문 자체에 걸어서 "확인 후 쓰기" 사이의 틈을 없앤다.
        // 그 틈에 삭제됐으면 0건 갱신되고, 그걸 404로 처리한다.
        int updated = springDataTextRepository.updateContentIfActive(txtId, content);
        if (updated == 0) {
            throw new NotFoundException(TextErrorCode.BLOCK_NOT_FOUND);
        }

        TextJpaEntity entity = springDataTextRepository.findById(txtId)
                .orElseThrow(() -> new IllegalStateException("text not found after update: " + txtId));
        return toDomain(entity);
    }

    @Override
    @Transactional
    public void markDeleted(Long txtId, LocalDateTime deletedAt) {
        TextJpaEntity entity = springDataTextRepository.findById(txtId)
                .orElseThrow(() -> new IllegalStateException("text not found: " + txtId));
        entity.applyDeletedAt(deletedAt);

        springDataTextRepository.save(entity);
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
                entity.getBlockId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
