package com.group3.vitamins.employeegroup.application.service;

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
import com.group3.vitamins.employeegroup.application.usecase.EmployeeGroupCommandUseCase;
import com.group3.vitamins.employeegroup.domain.exception.EmployeeGroupErrorCode;
import com.group3.vitamins.employeegroup.domain.model.EmployeeGroup;
import com.group3.vitamins.employeegroup.domain.repository.EmployeeGroupMemberRepository;
import com.group3.vitamins.employeegroup.domain.repository.EmployeeGroupRepository;
import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 그룹 생성·수정·삭제 (§2·§3·§4). 전부 ADMIN 전용. 쓰기는 JPA({@link EmployeeGroupRepository}) 담당.
 * 그룹명은 전역 유니크 — 검사~커밋 틈의 동시 중복은 {@code saveAndFlush} 위반을 409 로 변환해 막는다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EmployeeGroupCommandService implements EmployeeGroupCommandUseCase {

    private static final int MAX_NAME = 50;
    private static final int MAX_DESCRIPTION = 500;

    private final EmployeeGroupRepository groupRepository;
    private final EmployeeGroupMemberRepository memberRepository;
    private final EmployeeGroupQueryPort queryPort;
    private final EmployeeGroupAdminPolicy adminPolicy;

    @Override
    public GroupCreateResult create(CreateGroupCommand command) {
        adminPolicy.assertAdmin(command.role());
        String name = validateName(command.name());
        String description = validateDescription(command.description());

        if (groupRepository.existsByName(name)) {
            throw new ConflictException(EmployeeGroupErrorCode.GRP_NAME_DUPLICATED);
        }

        EmployeeGroup saved;
        try {
            saved = groupRepository.save(EmployeeGroup.create(name, description, command.createdBy()));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(EmployeeGroupErrorCode.GRP_NAME_DUPLICATED, e);
        }
        log.info("그룹 생성 - groupId={} name={}", saved.getGroupId(), name);
        return new GroupCreateResult(saved.getGroupId(), saved.getName(), saved.getDescription(), 0);
    }

    @Override
    public void update(UpdateGroupCommand command) {
        adminPolicy.assertAdmin(command.role());
        if (command.hasNoFields()) {
            throw new ValidationException(EmployeeGroupErrorCode.GRP_INVALID_REQUEST);
        }

        EmployeeGroup group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new NotFoundException(EmployeeGroupErrorCode.GRP_NOT_FOUND));

        if (command.nameProvided()) {
            String name = validateName(command.name());
            if (groupRepository.existsByNameExcludingSelf(name, command.groupId())) {
                throw new ConflictException(EmployeeGroupErrorCode.GRP_NAME_DUPLICATED);
            }
            group.rename(name);
        }
        if (command.descriptionProvided()) {
            group.changeDescription(validateDescription(command.description()));
        }

        try {
            groupRepository.save(group);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(EmployeeGroupErrorCode.GRP_NAME_DUPLICATED, e);
        }
        log.info("그룹 수정 - groupId={}", command.groupId());
    }

    @Override
    public void delete(DeleteGroupCommand command) {
        adminPolicy.assertAdmin(command.role());
        EmployeeGroup group = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new NotFoundException(EmployeeGroupErrorCode.GRP_NOT_FOUND));
        groupRepository.delete(group); // 구성원은 FK CASCADE 로 함께 제거된다
        log.info("그룹 삭제 - groupId={}", command.groupId());
    }

    /**
     * 구성원 다건 추가 (§6). 멱등 — 이미 소속인 사번은 조용히 건너뛰고 집계에만 반영한다.
     * ⚠️ 요청 사번 중 <b>하나라도 존재하지 않으면 전체 거부</b>(EMP_NOT_FOUND)하고, 시스템 계정이 섞이면
     * ACC_SYSTEM_ACCOUNT_NOT_ALLOWED. 이후에야 신규만 INSERT 한다. 요청 건수는 중복·공백 제거 후 기준이다.
     */
    @Override
    public AddMembersResult addMembers(AddMembersCommand command) {
        adminPolicy.assertAdmin(command.role());

        // 중복·공백 제거 후 요청 집합. 비면 400.
        Set<String> requested = new LinkedHashSet<>();
        if (command.userIds() != null) {
            for (String id : command.userIds()) {
                if (id != null && !id.isBlank()) {
                    requested.add(id.trim());
                }
            }
        }
        if (requested.isEmpty()) {
            throw new ValidationException(EmployeeGroupErrorCode.GRP_INVALID_REQUEST);
        }

        groupRepository.findById(command.groupId())
                .orElseThrow(() -> new NotFoundException(EmployeeGroupErrorCode.GRP_NOT_FOUND));

        // 존재·시스템 계정 검증 (배치 1회).
        List<EmployeeRefRow> refs = queryPort.findEmployeeRefs(requested);
        Set<String> found = refs.stream().map(EmployeeRefRow::userId).collect(Collectors.toSet());
        if (!found.containsAll(requested)) {
            throw new NotFoundException(EmployeeErrorCode.EMP_NOT_FOUND); // 없는 사번 → 전체 거부
        }
        if (refs.stream().anyMatch(EmployeeRefRow::isSystem)) {
            throw new ForbiddenException(AccountErrorCode.ACC_SYSTEM_ACCOUNT_NOT_ALLOWED);
        }

        // 이미 소속은 건너뛰고 신규만 추가(멱등).
        Set<String> existing = memberRepository.findMemberUserIds(command.groupId());
        List<String> toAdd = requested.stream().filter(id -> !existing.contains(id)).toList();
        memberRepository.addMembers(command.groupId(), toAdd);

        int alreadyMember = requested.size() - toAdd.size();
        int memberCount = queryPort.countMembers(command.groupId());
        log.info("그룹 구성원 추가 - groupId={} added={} already={}", command.groupId(), toAdd.size(), alreadyMember);
        return new AddMembersResult(command.groupId(), requested.size(), toAdd.size(), alreadyMember, memberCount);
    }

    /** 구성원 단건 제거 (§7). 그룹·구성원 존재를 확인한 뒤 매핑만 제거한다(권한 스냅샷은 그대로). */
    @Override
    public RemoveMemberResult removeMember(RemoveMemberCommand command) {
        adminPolicy.assertAdmin(command.role());

        groupRepository.findById(command.groupId())
                .orElseThrow(() -> new NotFoundException(EmployeeGroupErrorCode.GRP_NOT_FOUND));
        if (!memberRepository.existsMember(command.groupId(), command.userId())) {
            throw new NotFoundException(EmployeeGroupErrorCode.GRP_MEMBER_NOT_FOUND);
        }

        memberRepository.removeMember(command.groupId(), command.userId());
        int memberCount = queryPort.countMembers(command.groupId());
        log.info("그룹 구성원 제거 - groupId={} userId={}", command.groupId(), command.userId());
        return new RemoveMemberResult(command.groupId(), memberCount);
    }

    /** 그룹명 — 비었거나 50자 초과면 GRP_INVALID_REQUEST. 앞뒤 공백 제거. */
    private String validateName(String name) {
        String trimmed = name == null ? null : name.trim();
        if (trimmed == null || trimmed.isEmpty() || trimmed.length() > MAX_NAME) {
            throw new ValidationException(EmployeeGroupErrorCode.GRP_INVALID_REQUEST);
        }
        return trimmed;
    }

    /** 설명 — 선택값. 빈 문자열은 null 로 눕히고, 500자 초과면 GRP_INVALID_REQUEST. */
    private String validateDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_DESCRIPTION) {
            throw new ValidationException(EmployeeGroupErrorCode.GRP_INVALID_REQUEST);
        }
        return trimmed;
    }
}
