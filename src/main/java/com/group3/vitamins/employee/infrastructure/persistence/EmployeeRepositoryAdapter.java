package com.group3.vitamins.employee.infrastructure.persistence;

import com.group3.vitamins.employee.domain.model.Employee;
import com.group3.vitamins.employee.domain.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * {@link EmployeeRepository} 포트의 JPA 어댑터.
 *
 * <p>{@link #save} 는 {@code saveAndFlush} 로 즉시 반영해, PK({@code user_id}) 중복을 커밋까지 미루지 않고
 * 이 시점에 드러낸다 — 등록 유스케이스가 그 위반을 명세의 409({@code EMP_USER_ID_DUPLICATED})로 변환할 수
 * 있게 한다 (department 어댑터의 {@code saveAndFlush} 선례).
 */
@Repository
@RequiredArgsConstructor
public class EmployeeRepositoryAdapter implements EmployeeRepository {

    private final SpringDataEmployeeRepository springDataRepository;

    @Override
    public Employee save(Employee employee) {
        EmployeeJpaEntity saved =
                springDataRepository.saveAndFlush(EmployeePersistenceMapper.toEntity(employee));
        return EmployeePersistenceMapper.toDomain(saved);
    }

    @Override
    public boolean existsById(String userId) {
        return springDataRepository.existsById(userId);
    }

    @Override
    public Optional<Employee> findById(String userId) {
        return springDataRepository.findById(userId).map(EmployeePersistenceMapper::toDomain);
    }

    /**
     * 정보 수정 — 저장된 엔티티를 불러 정보 컬럼만 갱신한다({@code isNew=false} → UPDATE). 퇴사일은 건드리지
     * 않고 {@code @DynamicUpdate} 가 바뀐 컬럼만 쓰므로 동시 퇴사 처리를 덮어쓰지 않는다.
     */
    @Override
    public void updateInfo(Employee employee) {
        EmployeeJpaEntity entity = load(employee.getUserId());
        entity.applyInfo(employee.getName(), employee.getDepartmentId(), employee.getJobPositionId(),
                employee.getEmail(), employee.getPhone(), employee.getHiredAt());
        springDataRepository.saveAndFlush(entity);
    }

    /** 퇴사 처리 — resigned_at 만 갱신한다. 정보 컬럼은 건드리지 않아 동시 정보 수정과 충돌하지 않는다. */
    @Override
    public void resign(String userId, LocalDate resignedAt) {
        EmployeeJpaEntity entity = load(userId);
        entity.resign(resignedAt);
        springDataRepository.saveAndFlush(entity);
    }

    /** 프로필 사진 키 조회 — 사원 없음/사진 없음 모두 empty (호출자가 existsById 로 구분). */
    @Override
    public Optional<String> findProfileImageKey(String userId) {
        return springDataRepository.findById(userId).map(EmployeeJpaEntity::getProfileImageKey);
    }

    /** 프로필 사진 키 갱신 — profile_image_key 만 갱신한다(null 이면 삭제). @DynamicUpdate 로 다른 작업과 충돌 없음. */
    @Override
    public void updateProfileImageKey(String userId, String profileImageKey) {
        EmployeeJpaEntity entity = load(userId);
        entity.changeProfileImageKey(profileImageKey);
        springDataRepository.saveAndFlush(entity);
    }

    private EmployeeJpaEntity load(String userId) {
        return springDataRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "수정 대상 사원이 조회 직후 사라졌습니다: " + userId));
    }
}
