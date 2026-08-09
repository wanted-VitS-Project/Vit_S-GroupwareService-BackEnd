package com.group3.vitamins.employeegroup.application;

import com.group3.vitamins.account.domain.exception.AccountErrorCode;
import com.group3.vitamins.employee.domain.exception.EmployeeErrorCode;
import com.group3.vitamins.employeegroup.application.command.AddMembersCommand;
import com.group3.vitamins.employeegroup.application.command.CreateGroupCommand;
import com.group3.vitamins.employeegroup.application.command.DeleteGroupCommand;
import com.group3.vitamins.employeegroup.application.command.RemoveMemberCommand;
import com.group3.vitamins.employeegroup.application.command.UpdateGroupCommand;
import com.group3.vitamins.employeegroup.application.policy.EmployeeGroupAdminPolicy;
import com.group3.vitamins.employeegroup.application.port.EmployeeGroupQueryPort;
import com.group3.vitamins.employeegroup.application.result.AddMembersResult;
import com.group3.vitamins.employeegroup.application.result.EmployeeRefRow;
import com.group3.vitamins.employeegroup.application.result.GroupCreateResult;
import com.group3.vitamins.employeegroup.application.result.RemoveMemberResult;
import com.group3.vitamins.employeegroup.application.service.EmployeeGroupCommandService;
import com.group3.vitamins.employeegroup.domain.exception.EmployeeGroupErrorCode;
import com.group3.vitamins.employeegroup.domain.model.EmployeeGroup;
import com.group3.vitamins.employeegroup.domain.repository.EmployeeGroupMemberRepository;
import com.group3.vitamins.employeegroup.domain.repository.EmployeeGroupRepository;
import com.group3.vitamins.global.domain.common.error.DomainException;
import com.group3.vitamins.global.application.tenant.CurrentCompanyIdProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("EmployeeGroupCommandService")
class EmployeeGroupCommandServiceTest {

    private static final String ADMIN = "ADMIN";
    private static final Long GROUP_ID = 7L;

    private EmployeeGroupRepository groupRepository;
    private EmployeeGroupMemberRepository memberRepository;
    private EmployeeGroupQueryPort queryPort;
    private CurrentCompanyIdProvider currentCompanyIdProvider;
    private EmployeeGroupCommandService service;

    @BeforeEach
    void setUp() {
        groupRepository = Mockito.mock(EmployeeGroupRepository.class);
        memberRepository = Mockito.mock(EmployeeGroupMemberRepository.class);
        queryPort = Mockito.mock(EmployeeGroupQueryPort.class);
        // 생성 스탬핑이 읽는 회사 ID는 앱 포트로 주입 — 세션(SecurityContext) 세팅 불필요.
        currentCompanyIdProvider = Mockito.mock(CurrentCompanyIdProvider.class);
        when(currentCompanyIdProvider.currentCompanyId()).thenReturn(1L);
        service = new EmployeeGroupCommandService(
                groupRepository, memberRepository, queryPort, new EmployeeGroupAdminPolicy(),
                currentCompanyIdProvider);
    }

    private EmployeeGroup group() {
        return EmployeeGroup.restore(GROUP_ID, 1L, "개발팀", null, "ADMIN001");
    }

    private Consumer<Throwable> hasCode(Object expected) {
        return t -> assertThat(t).isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode()).isEqualTo(expected);
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("ADMIN 아니면 ACC_ADMIN_REQUIRED")
        void nonAdmin() {
            assertThatThrownBy(() -> service.create(new CreateGroupCommand("MASTER", "u", "g", null)))
                    .satisfies(hasCode(AccountErrorCode.ACC_ADMIN_REQUIRED));
        }

        @Test
        @DisplayName("그룹명이 비면 GRP_INVALID_REQUEST")
        void blankName() {
            assertThatThrownBy(() -> service.create(new CreateGroupCommand(ADMIN, "u", "  ", null)))
                    .satisfies(hasCode(EmployeeGroupErrorCode.GRP_INVALID_REQUEST));
        }

        @Test
        @DisplayName("중복 그룹명이면 GRP_NAME_DUPLICATED")
        void duplicate() {
            when(groupRepository.existsByName("개발팀", 1L)).thenReturn(true);
            assertThatThrownBy(() -> service.create(new CreateGroupCommand(ADMIN, "u", "개발팀", null)))
                    .satisfies(hasCode(EmployeeGroupErrorCode.GRP_NAME_DUPLICATED));
        }

        @Test
        @DisplayName("성공하면 memberCount 0 으로 돌려준다")
        void success() {
            when(groupRepository.existsByName("개발팀", 1L)).thenReturn(false);
            when(groupRepository.save(any())).thenReturn(group());
            GroupCreateResult r = service.create(new CreateGroupCommand(ADMIN, "ADMIN001", "개발팀", "설명"));
            assertThat(r.groupId()).isEqualTo(GROUP_ID);
            assertThat(r.memberCount()).isZero();
            // 저장된 도메인 객체에 현재 회사 ID(1)가 스탬핑되는지 검증
            ArgumentCaptor<EmployeeGroup> captor = ArgumentCaptor.forClass(EmployeeGroup.class);
            verify(groupRepository).save(captor.capture());
            assertThat(captor.getValue().getCompanyId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @Test
        @DisplayName("수정할 필드가 없으면 GRP_INVALID_REQUEST")
        void noFields() {
            assertThatThrownBy(() -> service.update(
                    new UpdateGroupCommand(ADMIN, GROUP_ID, false, null, false, null)))
                    .satisfies(hasCode(EmployeeGroupErrorCode.GRP_INVALID_REQUEST));
        }

        @Test
        @DisplayName("그룹이 없으면 GRP_NOT_FOUND")
        void notFound() {
            when(groupRepository.findById(GROUP_ID, 1L)).thenReturn(java.util.Optional.empty());
            assertThatThrownBy(() -> service.update(
                    new UpdateGroupCommand(ADMIN, GROUP_ID, true, "새이름", false, null)))
                    .satisfies(hasCode(EmployeeGroupErrorCode.GRP_NOT_FOUND));
        }

        @Test
        @DisplayName("이름 중복이면 GRP_NAME_DUPLICATED")
        void nameDup() {
            when(groupRepository.findById(GROUP_ID, 1L)).thenReturn(java.util.Optional.of(group()));
            when(groupRepository.existsByNameExcludingSelf("중복", GROUP_ID, 1L)).thenReturn(true);
            assertThatThrownBy(() -> service.update(
                    new UpdateGroupCommand(ADMIN, GROUP_ID, true, "중복", false, null)))
                    .satisfies(hasCode(EmployeeGroupErrorCode.GRP_NAME_DUPLICATED));
        }

        @Test
        @DisplayName("설명만 수정하면 이름 중복검사를 하지 않는다")
        void descOnly() {
            when(groupRepository.findById(GROUP_ID, 1L)).thenReturn(java.util.Optional.of(group()));
            when(groupRepository.save(any())).thenReturn(group());
            service.update(new UpdateGroupCommand(ADMIN, GROUP_ID, false, null, true, "새 설명"));
            verify(groupRepository, never()).existsByNameExcludingSelf(anyString(), anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("삭제")
    class Delete {

        @Test
        @DisplayName("그룹이 없으면 GRP_NOT_FOUND")
        void notFound() {
            when(groupRepository.findById(GROUP_ID, 1L)).thenReturn(java.util.Optional.empty());
            assertThatThrownBy(() -> service.delete(new DeleteGroupCommand(ADMIN, GROUP_ID)))
                    .satisfies(hasCode(EmployeeGroupErrorCode.GRP_NOT_FOUND));
        }

        @Test
        @DisplayName("성공하면 delete 를 호출한다(구성원은 CASCADE)")
        void success() {
            when(groupRepository.findById(GROUP_ID, 1L)).thenReturn(java.util.Optional.of(group()));
            service.delete(new DeleteGroupCommand(ADMIN, GROUP_ID));
            verify(groupRepository).delete(any());
        }
    }

    @Nested
    @DisplayName("구성원 추가")
    class AddMembers {

        private AddMembersCommand cmd(List<String> userIds) {
            return new AddMembersCommand(ADMIN, GROUP_ID, userIds);
        }

        @Test
        @DisplayName("userIds 가 비면 GRP_INVALID_REQUEST")
        void empty() {
            assertThatThrownBy(() -> service.addMembers(cmd(List.of())))
                    .satisfies(hasCode(EmployeeGroupErrorCode.GRP_INVALID_REQUEST));
        }

        @Test
        @DisplayName("존재하지 않는 사번이 있으면 EMP_NOT_FOUND 로 전체 거부")
        void missingUser() {
            when(groupRepository.findByIdForUpdate(GROUP_ID, 1L)).thenReturn(java.util.Optional.of(group()));
            // EMP2 는 결과에 없음 → 없는 사번
            when(queryPort.findEmployeeRefs(any(), anyLong())).thenReturn(List.of(new EmployeeRefRow("EMP1", false)));
            assertThatThrownBy(() -> service.addMembers(cmd(List.of("EMP1", "EMP2"))))
                    .satisfies(hasCode(EmployeeErrorCode.EMP_NOT_FOUND));
            verify(memberRepository, never()).addMembers(anyLong(), any());
        }

        @Test
        @DisplayName("시스템 계정이 섞이면 ACC_SYSTEM_ACCOUNT_NOT_ALLOWED")
        void systemAccount() {
            when(groupRepository.findByIdForUpdate(GROUP_ID, 1L)).thenReturn(java.util.Optional.of(group()));
            when(queryPort.findEmployeeRefs(any(), anyLong()))
                    .thenReturn(List.of(new EmployeeRefRow("EMP1", false), new EmployeeRefRow("ADMIN001", true)));
            assertThatThrownBy(() -> service.addMembers(cmd(List.of("EMP1", "ADMIN001"))))
                    .satisfies(hasCode(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED));
        }

        @Test
        @DisplayName("이미 소속은 건너뛰고 신규만 추가한다(멱등·집계)")
        void idempotent() {
            when(groupRepository.findByIdForUpdate(GROUP_ID, 1L)).thenReturn(java.util.Optional.of(group()));
            when(queryPort.findEmployeeRefs(any(), anyLong())).thenReturn(List.of(
                    new EmployeeRefRow("EMP1", false), new EmployeeRefRow("EMP2", false)));
            when(memberRepository.findExistingMemberUserIds(eq(GROUP_ID), any())).thenReturn(Set.of("EMP1")); // EMP1 이미 소속
            when(queryPort.countMembers(GROUP_ID)).thenReturn(2);

            AddMembersResult r = service.addMembers(cmd(List.of("EMP1", "EMP2")));

            assertThat(r.requestedCount()).isEqualTo(2);
            assertThat(r.addedCount()).isEqualTo(1);
            assertThat(r.alreadyMemberCount()).isEqualTo(1);
            assertThat(r.memberCount()).isEqualTo(2);

            ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
            verify(memberRepository).addMembers(eq(GROUP_ID), captor.capture());
            assertThat(captor.getValue()).containsExactly("EMP2");
        }
    }

    @Nested
    @DisplayName("구성원 제거")
    class RemoveMember {

        @Test
        @DisplayName("삭제 0건이면(구성원 아님) GRP_MEMBER_NOT_FOUND")
        void notMember() {
            when(groupRepository.findById(GROUP_ID, 1L)).thenReturn(java.util.Optional.of(group()));
            when(memberRepository.removeMember(GROUP_ID, "EMP9")).thenReturn(0);
            assertThatThrownBy(() -> service.removeMember(new RemoveMemberCommand(ADMIN, GROUP_ID, "EMP9")))
                    .satisfies(hasCode(EmployeeGroupErrorCode.GRP_MEMBER_NOT_FOUND));
            verify(memberRepository).removeMember(GROUP_ID, "EMP9");    // 원자 삭제를 실제로 호출했고
            verify(queryPort, never()).countMembers(anyLong());        // 실패 후 집계는 하지 않는다
        }

        @Test
        @DisplayName("삭제 1건이면 처리 후 구성원 수를 돌려준다")
        void success() {
            when(groupRepository.findById(GROUP_ID, 1L)).thenReturn(java.util.Optional.of(group()));
            when(memberRepository.removeMember(GROUP_ID, "EMP1")).thenReturn(1);
            when(queryPort.countMembers(GROUP_ID)).thenReturn(3);
            RemoveMemberResult r = service.removeMember(new RemoveMemberCommand(ADMIN, GROUP_ID, "EMP1"));
            assertThat(r.memberCount()).isEqualTo(3);
            verify(memberRepository).removeMember(GROUP_ID, "EMP1");
        }
    }
}
