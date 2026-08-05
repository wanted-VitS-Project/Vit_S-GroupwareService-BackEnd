package com.group3.vitamins.jobposition.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.jobposition.application.command.CreateJobPositionCommand;
import com.group3.vitamins.jobposition.application.command.DeleteJobPositionCommand;
import com.group3.vitamins.jobposition.application.command.UpdateJobPositionCommand;
import com.group3.vitamins.jobposition.application.policy.JobPositionAdminPolicy;
import com.group3.vitamins.jobposition.application.port.JobPositionEmployeeCountPort;
import com.group3.vitamins.jobposition.application.result.JobPositionResult;
import com.group3.vitamins.jobposition.application.service.JobPositionCommandService;
import com.group3.vitamins.jobposition.domain.exception.JobPositionErrorCode;
import com.group3.vitamins.jobposition.domain.model.JobPosition;
import com.group3.vitamins.jobposition.domain.repository.JobPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("JobPositionCommandService 직급 생성·수정·삭제")
class JobPositionCommandServiceTest {

    private JobPositionRepository jobPositionRepository;
    private JobPositionEmployeeCountPort employeeCountPort;
    private JobPositionCommandService commandService;

    @BeforeEach
    void setUp() {
        jobPositionRepository = Mockito.mock(JobPositionRepository.class);
        employeeCountPort = Mockito.mock(JobPositionEmployeeCountPort.class);
        // ADMIN 판정은 순수 컴포넌트라 실제 인스턴스를 그대로 쓴다 (mock 불필요).
        commandService = new JobPositionCommandService(
                jobPositionRepository, employeeCountPort, new JobPositionAdminPolicy());
    }

    /** id 가 설정된 직급 도메인 객체를 만든다 (JPA 가 채우는 jobPositionId 를 흉내낸다). */
    private JobPosition position(Long id, String name, int sortOrder) {
        return JobPosition.restore(id, name, sortOrder);
    }

    @Nested
    @DisplayName("직급 생성")
    class Create {

        @Test
        @DisplayName("sortOrder 를 지정하면 그대로 저장되고 사용 인원은 0 이다")
        void createsWithExplicitSortOrder() {
            when(jobPositionRepository.findByName("대리")).thenReturn(Optional.empty());
            when(jobPositionRepository.save(any())).thenReturn(position(10L, "대리", 2));

            JobPositionResult result = commandService.createJobPosition(
                    new CreateJobPositionCommand("대리", 2, "ADMIN"));

            assertThat(result.jobPositionId()).isEqualTo(10L);
            assertThat(result.name()).isEqualTo("대리");
            assertThat(result.sortOrder()).isEqualTo(2);
            assertThat(result.employeeCount()).isZero();
            verify(jobPositionRepository, never()).nextSortOrder();
            verify(jobPositionRepository, times(1)).save(any(JobPosition.class));
        }

        @Test
        @DisplayName("sortOrder 를 생략하면 마지막 순서 + 1 을 붙인다")
        void createsWithNextSortOrderWhenOmitted() {
            when(jobPositionRepository.findByName("사원")).thenReturn(Optional.empty());
            when(jobPositionRepository.nextSortOrder()).thenReturn(5);
            when(jobPositionRepository.save(any())).thenReturn(position(11L, "사원", 5));

            JobPositionResult result = commandService.createJobPosition(
                    new CreateJobPositionCommand("사원", null, "ADMIN"));

            assertThat(result.sortOrder()).isEqualTo(5);
            verify(jobPositionRepository, times(1)).nextSortOrder();
        }

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED — 조회·저장 이전에 막는다")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> commandService.createJobPosition(
                    new CreateJobPositionCommand("대리", 2, "MASTER")))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verify(jobPositionRepository, never()).save(any());
            verify(jobPositionRepository, never()).findByName(anyString());
        }

        @Test
        @DisplayName("직급명이 비어 있으면 POS_INVALID_REQUEST")
        void rejectsBlankName() {
            assertThatThrownBy(() -> commandService.createJobPosition(
                    new CreateJobPositionCommand("  ", 1, "ADMIN")))
                    .satisfies(hasCode(JobPositionErrorCode.POS_INVALID_REQUEST));
            verify(jobPositionRepository, never()).save(any());
        }

        @Test
        @DisplayName("직급명이 30자를 초과하면 POS_INVALID_REQUEST")
        void rejectsTooLongName() {
            String tooLong = "가".repeat(31);
            assertThatThrownBy(() -> commandService.createJobPosition(
                    new CreateJobPositionCommand(tooLong, 1, "ADMIN")))
                    .satisfies(hasCode(JobPositionErrorCode.POS_INVALID_REQUEST));
        }

        @Test
        @DisplayName("이미 존재하는 직급명이면 POS_NAME_DUPLICATED")
        void rejectsDuplicateName() {
            when(jobPositionRepository.findByName("사원")).thenReturn(Optional.of(position(1L, "사원", 1)));

            assertThatThrownBy(() -> commandService.createJobPosition(
                    new CreateJobPositionCommand("사원", null, "ADMIN")))
                    .satisfies(hasCode(JobPositionErrorCode.POS_NAME_DUPLICATED));
            verify(jobPositionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("직급 수정")
    class Update {

        @Test
        @DisplayName("직급명만 수정하면 이름이 바뀌고 사용 인원이 응답에 담긴다")
        void updatesNameOnly() {
            when(jobPositionRepository.findById(4L)).thenReturn(Optional.of(position(4L, "대리", 2)));
            when(jobPositionRepository.findByName("과장")).thenReturn(Optional.empty());
            when(jobPositionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(employeeCountPort.countByJobPositionId(4L)).thenReturn(3L);

            JobPositionResult result = commandService.updateJobPosition(
                    new UpdateJobPositionCommand(4L, true, "과장", false, null, "ADMIN"));

            assertThat(result.name()).isEqualTo("과장");
            assertThat(result.sortOrder()).isEqualTo(2);
            assertThat(result.employeeCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("정렬 순서만 수정하면 이름은 그대로고 중복 검사는 하지 않는다")
        void updatesSortOrderOnly() {
            when(jobPositionRepository.findById(4L)).thenReturn(Optional.of(position(4L, "대리", 2)));
            when(jobPositionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(employeeCountPort.countByJobPositionId(4L)).thenReturn(0L);

            JobPositionResult result = commandService.updateJobPosition(
                    new UpdateJobPositionCommand(4L, false, null, true, 9, "ADMIN"));

            assertThat(result.name()).isEqualTo("대리");
            assertThat(result.sortOrder()).isEqualTo(9);
            verify(jobPositionRepository, never()).findByName(anyString());
        }

        @Test
        @DisplayName("같은 이름으로 수정(자기 자신)하면 중복이 아니다")
        void allowsRenameToSameName() {
            when(jobPositionRepository.findById(4L)).thenReturn(Optional.of(position(4L, "대리", 2)));
            when(jobPositionRepository.findByName("대리")).thenReturn(Optional.of(position(4L, "대리", 2)));
            when(jobPositionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(employeeCountPort.countByJobPositionId(4L)).thenReturn(0L);

            JobPositionResult result = commandService.updateJobPosition(
                    new UpdateJobPositionCommand(4L, true, "대리", false, null, "ADMIN"));

            assertThat(result.name()).isEqualTo("대리");
        }

        @Test
        @DisplayName("수정할 필드가 하나도 없으면 POS_INVALID_REQUEST")
        void rejectsNoFields() {
            assertThatThrownBy(() -> commandService.updateJobPosition(
                    new UpdateJobPositionCommand(4L, false, null, false, null, "ADMIN")))
                    .satisfies(hasCode(JobPositionErrorCode.POS_INVALID_REQUEST));
            verify(jobPositionRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("직급이 없으면 POS_NOT_FOUND")
        void rejectsMissing() {
            when(jobPositionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.updateJobPosition(
                    new UpdateJobPositionCommand(99L, true, "부장", false, null, "ADMIN")))
                    .satisfies(hasCode(JobPositionErrorCode.POS_NOT_FOUND));
        }

        @Test
        @DisplayName("다른 직급과 이름이 겹치면 POS_NAME_DUPLICATED")
        void rejectsDuplicateName() {
            when(jobPositionRepository.findById(4L)).thenReturn(Optional.of(position(4L, "대리", 2)));
            when(jobPositionRepository.findByName("과장")).thenReturn(Optional.of(position(7L, "과장", 3)));

            assertThatThrownBy(() -> commandService.updateJobPosition(
                    new UpdateJobPositionCommand(4L, true, "과장", false, null, "ADMIN")))
                    .satisfies(hasCode(JobPositionErrorCode.POS_NAME_DUPLICATED));
            verify(jobPositionRepository, never()).save(any());
        }

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> commandService.updateJobPosition(
                    new UpdateJobPositionCommand(4L, true, "과장", false, null, "MEMBER")))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verify(jobPositionRepository, never()).findById(anyLong());
        }
    }

    @Nested
    @DisplayName("직급 삭제")
    class Delete {

        @Test
        @DisplayName("사용 인원이 없으면 삭제된다")
        void deletesWhenUnused() {
            JobPosition target = position(5L, "인턴", 6);
            when(jobPositionRepository.findById(5L)).thenReturn(Optional.of(target));
            when(employeeCountPort.countByJobPositionId(5L)).thenReturn(0L);

            commandService.deleteJobPosition(new DeleteJobPositionCommand(5L, "ADMIN"));

            verify(jobPositionRepository, times(1)).delete(target);
        }

        @Test
        @DisplayName("직급이 없으면 POS_NOT_FOUND")
        void rejectsMissing() {
            when(jobPositionRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commandService.deleteJobPosition(
                    new DeleteJobPositionCommand(99L, "ADMIN")))
                    .satisfies(hasCode(JobPositionErrorCode.POS_NOT_FOUND));
        }

        @Test
        @DisplayName("사용 인원이 있으면 POS_IN_USE — 메시지에 인원 수를 담는다")
        void rejectsWhenInUse() {
            when(jobPositionRepository.findById(1L)).thenReturn(Optional.of(position(1L, "사원", 1)));
            when(employeeCountPort.countByJobPositionId(1L)).thenReturn(14L);

            assertThatThrownBy(() -> commandService.deleteJobPosition(
                    new DeleteJobPositionCommand(1L, "ADMIN")))
                    .satisfies(hasCode(JobPositionErrorCode.POS_IN_USE))
                    .hasMessageContaining("14");
            verify(jobPositionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("ADMIN 이 아니면 ACC_ADMIN_REQUIRED")
        void rejectsNonAdmin() {
            assertThatThrownBy(() -> commandService.deleteJobPosition(
                    new DeleteJobPositionCommand(5L, "MASTER")))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
            verify(jobPositionRepository, never()).findById(anyLong());
        }
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return throwable -> assertThat(throwable)
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
