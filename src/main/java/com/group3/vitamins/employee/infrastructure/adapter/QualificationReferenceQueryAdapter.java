package com.group3.vitamins.employee.infrastructure.adapter;

import com.group3.vitamins.employee.application.port.QualificationReferenceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;

/**
 * {@link QualificationReferenceQueryPort} 의 MyBatis 어댑터. 실제 SQL 은
 * {@link QualificationReferenceQueryMapper} 와 그 XML 이 갖는다.
 *
 * <p>빈 컬렉션은 조회하지 않는다 — {@code IN ()} 은 SQL 문법 오류라 foreach 에 넘기면 안 된다.
 */
@Component
@RequiredArgsConstructor
public class QualificationReferenceQueryAdapter implements QualificationReferenceQueryPort {

    private final QualificationReferenceQueryMapper mapper;

    @Override
    public Set<Long> findExistingMajorIds(Collection<Long> majorIds, Long companyId) {
        if (majorIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(mapper.findExistingMajorIds(majorIds, companyId));
    }

    @Override
    public Set<Long> findExistingCertificateIds(Collection<Long> certificateIds, Long companyId) {
        if (certificateIds.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(mapper.findExistingCertificateIds(certificateIds, companyId));
    }
}
