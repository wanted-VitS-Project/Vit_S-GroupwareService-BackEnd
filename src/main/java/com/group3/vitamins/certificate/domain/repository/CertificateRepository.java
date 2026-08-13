package com.group3.vitamins.certificate.domain.repository;

import com.group3.vitamins.certificate.domain.model.Certificate;

import java.util.Optional;

/**
 * 자격증 마스터 영속성 아웃바운드 포트 (쓰기·단건 조회).
 *
 * <p>목록(사용 사원 수 포함)은 MyBatis 조회 포트({@code CertificateQueryPort})가 담당한다.
 * 삭제는 hard delete 라 {@code deleteById} 가 있다(business_category 의 soft delete 와 다름).
 */
public interface CertificateRepository {

    Certificate save(Certificate certificate);

    /** 회사 범위 단건 조회. 타사 자격증은 404 취급. */
    Optional<Certificate> findById(Long certificateId, Long companyId);

    /** 회사 범위에서 이름으로 조회(중복 검사용). */
    Optional<Certificate> findByName(String name, Long companyId);

    /** 물리 삭제. 참조 차단은 서비스가 먼저 판정한다(INV-18). */
    void deleteById(Long certificateId);
}
