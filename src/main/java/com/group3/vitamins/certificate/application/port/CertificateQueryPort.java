package com.group3.vitamins.certificate.application.port;

import com.group3.vitamins.certificate.application.result.CertificateListProjection;

import java.util.List;

/**
 * 자격증 마스터 화면용 조회 아웃바운드 포트 (MyBatis).
 * 목록은 사용 사원 수(활성)를 함께 세고, 삭제 검사는 참조 수를 센다. 구현은 {@code infrastructure/adapter}.
 */
public interface CertificateQueryPort {

    /** 자격증 목록 + 사용 사원 수(시스템·퇴사 제외, MAJ-003). 이름 오름차순. */
    List<CertificateListProjection> findCertificatesWithCount(Long companyId, String keyword);

    /** 이 자격증을 참조하는 활성 사원 학력 수(삭제 차단 판정, MAJ-002). */
    long countReferences(Long certificateId, Long companyId);
}
