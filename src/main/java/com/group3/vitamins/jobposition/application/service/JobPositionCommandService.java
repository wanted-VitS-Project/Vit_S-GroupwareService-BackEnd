package com.group3.vitamins.jobposition.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.jobposition.application.command.CreateJobPositionCommand;
import com.group3.vitamins.jobposition.application.command.DeleteJobPositionCommand;
import com.group3.vitamins.jobposition.application.command.UpdateJobPositionCommand;
import com.group3.vitamins.jobposition.application.policy.JobPositionAdminPolicy;
import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeCountPort;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import com.group3.vitamins.jobposition.application.usecase.JobPositionCommandUseCase;
import com.group3.vitamins.jobposition.domain.exception.JobPositionErrorCode;
import com.group3.vitamins.jobposition.domain.model.JobPosition;
import com.group3.vitamins.jobposition.domain.repository.JobPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class JobPositionCommandService implements JobPositionCommandUseCase {

    private static final int NAME_MAX_LENGTH = 30;

    private final JobPositionRepository jobPositionRepository;
    private final JobPositionEmployeeCountPort jobPositionEmployeeCountPort;
    private final JobPositionAdminPolicy jobPositionAdminPolicy;

    @Override
    public JobPositionResult createJobPosition(CreateJobPositionCommand command) {
        jobPositionAdminPolicy.assertAdmin(command.role());
        validateName(command.name());
        checkNameDuplicate(command.name(), null);

        // sortOrder 생략 시 마지막 순서 뒤에 붙인다 (§2)
        int sortOrder = command.sortOrder() != null
                ? command.sortOrder()
                : jobPositionRepository.nextSortOrder();

        JobPosition saved = saveHandlingNameConflict(JobPosition.create(command.name(), sortOrder));

        // 생성 직후이므로 사용 인원은 항상 0
        return JobPositionResult.of(saved, 0);
    }

    @Override
    public JobPositionResult updateJobPosition(UpdateJobPositionCommand command) {
        jobPositionAdminPolicy.assertAdmin(command.role());

        if (!command.nameProvided() && !command.sortOrderProvided()) {
            throw new ValidationException(JobPositionErrorCode.POS_INVALID_REQUEST,
                    "수정할 항목이 없습니다.");
        }

        JobPosition jobPosition = jobPositionRepository.findById(command.jobPositionId())
                .orElseThrow(() -> new NotFoundException(JobPositionErrorCode.POS_NOT_FOUND));

        if (command.nameProvided()) {
            validateName(command.name());
            checkNameDuplicate(command.name(), jobPosition.getJobPositionId());
            jobPosition.rename(command.name());
        }
        if (command.sortOrderProvided()) {
            jobPosition.changeSortOrder(command.sortOrder());
        }

        JobPosition saved = saveHandlingNameConflict(jobPosition);
        long employeeCount = jobPositionEmployeeCountPort.countByJobPositionId(saved.getJobPositionId());

        return JobPositionResult.of(saved, (int) employeeCount);
    }

    @Override
    public void deleteJobPosition(DeleteJobPositionCommand command) {
        jobPositionAdminPolicy.assertAdmin(command.role());

        JobPosition jobPosition = jobPositionRepository.findById(command.jobPositionId())
                .orElseThrow(() -> new NotFoundException(JobPositionErrorCode.POS_NOT_FOUND));

        // ⚠️ 표시용 사용 인원(countByJobPositionId)이 아니라 전체 참조 수로 판정한다.
        // 퇴사자·삭제 사원도 FK 로 직급을 참조하므로, 이들만 남은 직급을 삭제하면 FK 위반 500 이 난다 (§4).
        long referencingCount = jobPositionEmployeeCountPort.countAllReferencing(jobPosition.getJobPositionId());
        if (referencingCount > 0) {
            // 명세상 메시지에 인원 수를 담는다 (§4). 코드가 바뀌면 프론트 분기가 깨지므로 메시지만 덮는다.
            throw new ConflictException(JobPositionErrorCode.POS_IN_USE,
                    "사용 인원이 있어 삭제할 수 없습니다. (사원 " + referencingCount + "명)");
        }

        try {
            jobPositionRepository.delete(jobPosition);
        } catch (DataIntegrityViolationException e) {
            // 게이트 통과와 삭제 사이에 사원이 배정된 경합 — FK 위반을 500 이 아니라 POS_IN_USE 로 변환한다.
            throw new ConflictException(JobPositionErrorCode.POS_IN_USE, e);
        }
    }

    /**
     * 저장 중 이름 UNIQUE 제약 위반을 {@link ConflictException}({@code POS_NAME_DUPLICATED})으로 변환한다.
     *
     * <p>{@link #checkNameDuplicate}(findByName)로 선검사하지만, 동시 요청은 검사와 저장 사이에 겹칠 수 있다.
     * DB UNIQUE 제약이 최종 방어선이고, 그 위반을 전역 핸들러가 500 으로 흘리지 않도록 여기서 409 로 바꾼다 (department 선례).
     */
    private JobPosition saveHandlingNameConflict(JobPosition jobPosition) {
        try {
            return jobPositionRepository.save(jobPosition);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(JobPositionErrorCode.POS_NAME_DUPLICATED, e);
        }
    }

    /** 직급명 형식 검증 — null·공백·30자 초과를 막는다 (§2 · §3). */
    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException(JobPositionErrorCode.POS_INVALID_REQUEST);
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new ValidationException(JobPositionErrorCode.POS_INVALID_REQUEST);
        }
    }

    /** 직급명 중복 검사. excludeId 는 수정 중인 자기 자신 — 있으면 건너뛴다. DB UNIQUE 와 이중 방어. */
    private void checkNameDuplicate(String name, Long excludeId) {
        jobPositionRepository.findByName(name)
                .filter(existing -> !existing.getJobPositionId().equals(excludeId))
                .ifPresent(existing -> {
                    throw new ConflictException(JobPositionErrorCode.POS_NAME_DUPLICATED);
                });
    }
}
