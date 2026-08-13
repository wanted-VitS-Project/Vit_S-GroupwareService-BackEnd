package com.group3.vitamins.major.domain.repository;

import com.group3.vitamins.major.domain.model.Major;

import java.util.Optional;

/**
 * 전공 마스터 영속성 아웃바운드 포트 (쓰기·단건 조회).
 *
 * <p>목록(사용 사원 수 포함)은 MyBatis 조회 포트({@code MajorQueryPort})가 담당한다.
 * 삭제는 hard delete 라 {@code deleteById} 가 있다(business_category 의 soft delete 와 다름).
 */
public interface MajorRepository {

    Major save(Major major);

    /** 회사 범위 단건 조회. 타사 전공은 404 취급. */
    Optional<Major> findById(Long majorId, Long companyId);

    /** 회사 범위에서 이름으로 조회(중복 검사용). */
    Optional<Major> findByName(String name, Long companyId);

    /** 물리 삭제. 참조 차단은 서비스가 먼저 판정한다(INV-18). */
    void deleteById(Long majorId);
}
