package com.group3.vitamins.text.infrastructure.catalog;

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
 * text 행을 만들 시점은 Block 도메인(동훈님)이 판단하고, 실제 INSERT 는 여기서 한다 — Block 도메인은
 * BlockDetailPort 로 요청만 보낸다.
 *
 * <p>updateContentIfVersionMatches/markDeleted 를 분리한 이유: 하나의 save() 가 content 와 deletedAt 을 같이
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
    public Long create(Long blockId) {
        // IDENTITY 라 save() 시점에 INSERT 가 나가고 PK 가 채워져 돌아온다 — 되찾기 조회가 필요없다.
        return springDataTextRepository.save(new TextJpaEntity(blockId)).getTxtId();
    }

    @Override
    @Transactional
    public int updateContentIfVersionMatches(Long txtId, String content, LocalDateTime updatedAt, int expectedVersion) {
        return springDataTextRepository.updateContentIfVersionMatches(txtId, content, updatedAt, expectedVersion);
    }

    @Override
    @Transactional
    public boolean markDeleted(Long txtId, LocalDateTime deletedAt) {
        // 조건부 UPDATE: 이미 삭제된 행이면 0건 갱신 → 중복 이벤트로 간주하고 false 반환.
        // 이걸로 "확인 후 쓰기" 틈도 없애고, 동시 삭제 시 최초 삭제 시각이 나중 이벤트로
        // 덮어써지는 것도 막는다 (조건에 안 맞으면 애초에 UPDATE 자체가 안 나간다).
        int updated = springDataTextRepository.markDeletedIfActive(txtId, deletedAt);
        return updated > 0;
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
                entity.getDeletedAt(),
                entity.getVersion()
        );
    }
}
