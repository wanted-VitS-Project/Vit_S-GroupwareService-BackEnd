package com.group3.vitamins.project.application.service;

import com.group3.vitamins.global.domain.common.error.exception.ConflictException;
import com.group3.vitamins.global.domain.common.error.exception.ForbiddenException;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.project.application.command.AddMemberCommand;
import com.group3.vitamins.project.application.command.ChangeMemberPermissionCommand;
import com.group3.vitamins.project.application.command.RemoveMemberCommand;
import com.group3.vitamins.project.application.port.EmployeeLookupPort;
import com.group3.vitamins.project.application.result.MemberResult;
import com.group3.vitamins.project.application.usecase.ProjectAccessUseCase;
import com.group3.vitamins.project.application.usecase.ProjectMemberCommandUseCase;
import com.group3.vitamins.project.domain.exception.ProjectErrorCode;
import com.group3.vitamins.project.domain.model.MemberPermission;
import com.group3.vitamins.project.domain.model.ProjectMember;
import com.group3.vitamins.project.domain.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectMemberCommandService implements ProjectMemberCommandUseCase {

    private final ProjectAccessUseCase projectAccessUseCase;
    private final ProjectMemberRepository projectMemberRepository;
    private final EmployeeLookupPort employeeLookupPort;

    /** 참여자를 한 명 추가한다. 응답에 쓸 이름을 조회하면서 사원 존재 여부도 함께 판정한다. */
    @Override
    public MemberResult addMember(AddMemberCommand command) {
        //권한 검증
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        //enum 파싱
        MemberPermission permission = parsePermission(command.permission());

        //중복확인
        String name = employeeLookupPort.findNameByUserId(command.userId());
        if (name == null) {
            throw new NotFoundException(ProjectErrorCode.USER_NOT_FOUND);
        }
        if (projectMemberRepository.findPermission(command.projectId(), command.userId())
                .isPresent()) {
            throw new ConflictException(ProjectErrorCode.MEMBER_ALREADY_EXISTS);
        }

        //멤버 저장
        ProjectMember saved = projectMemberRepository.save(ProjectMember.create(
                command.projectId(), command.userId(), permission, LocalDateTime.now()));

        //리턴
        return new MemberResult(saved.getProjectMemberId(), saved.getUserId(),
                name, saved.getPermission().name());
    }

    /** 참여자 권한 등급을 바꾼다. */
    @Override
    public MemberResult changePermission(ChangeMemberPermissionCommand command) {
        //권한 검증
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        //멤버 조회
        ProjectMember member = findMember(command.projectId(), command.memberId());

        //자기 자신 권한 수정 불가
        requireNotSelf(member, command.requesterUserId());

        //멤버 수정
        ProjectMember updated = projectMemberRepository.save(
                member.changePermission(parsePermission(command.permission())));

        //리턴
        return new MemberResult(updated.getProjectMemberId(), updated.getUserId(),
                null, updated.getPermission().name());
    }

    /** 참여자를 프로젝트에서 제거한다. */
    @Override
    public void removeMember(RemoveMemberCommand command) {
        //권한 검증
        projectAccessUseCase.requireEditable(
                command.projectId(), command.requesterUserId(), command.role());

        //멤버 조회
        ProjectMember member = findMember(command.projectId(), command.memberId());

        //자기자신 권한 수정 불가
        requireNotSelf(member, command.requesterUserId());

        //프로젝트 제거
        projectMemberRepository.deleteById(member.getProjectMemberId());
    }

    /** 참여자 행을 찾고 경로의 프로젝트 소속인지 확인한다. 다른 프로젝트의 행이면 없는 것으로 본다. */
    private ProjectMember findMember(Long projectId, Long memberId) {
        return projectMemberRepository.findById(memberId)
                .filter(member -> member.getProjectId().equals(projectId))
                .orElseThrow(() -> new NotFoundException(ProjectErrorCode.MEMBER_NOT_FOUND));
    }

    /** 자기 자신의 권한 행은 변경·제거할 수 없다. */
    private void requireNotSelf(ProjectMember member, String requesterUserId) {
        if (member.getUserId().equals(requesterUserId)) {
            throw new ForbiddenException(ProjectErrorCode.MEMBER_SELF_EDIT_DENIED);
        }
    }

    /** 요청 문자열을 권한 등급으로 바꾼다. VIEWER·EDITOR·NONE 이 아니면 거부한다. */
    private MemberPermission parsePermission(String permission) {
        if (permission == null) {
            throw new ValidationException(ProjectErrorCode.MEMBER_PERMISSION_INVALID);
        }
        try {
            return MemberPermission.valueOf(permission);
        } catch (IllegalArgumentException e) {
            throw new ValidationException(ProjectErrorCode.MEMBER_PERMISSION_INVALID);
        }
    }
}