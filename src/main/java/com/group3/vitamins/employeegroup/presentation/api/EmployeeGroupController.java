package com.group3.vitamins.employeegroup.presentation.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.group3.vitamins.employeegroup.application.command.DeleteGroupCommand;
import com.group3.vitamins.employeegroup.application.command.RemoveMemberCommand;
import com.group3.vitamins.employeegroup.application.command.UpdateGroupCommand;
import com.group3.vitamins.employeegroup.application.usecase.EmployeeGroupCommandUseCase;
import com.group3.vitamins.employeegroup.application.usecase.EmployeeGroupQueryUseCase;
import com.group3.vitamins.employeegroup.presentation.api.request.AddMembersRequest;
import com.group3.vitamins.employeegroup.presentation.api.request.CreateGroupRequest;
import com.group3.vitamins.employeegroup.presentation.api.response.AddMembersResponse;
import com.group3.vitamins.employeegroup.presentation.api.response.GroupCreateResponse;
import com.group3.vitamins.employeegroup.presentation.api.response.GroupItemResponse;
import com.group3.vitamins.employeegroup.presentation.api.response.GroupListResponse;
import com.group3.vitamins.employeegroup.presentation.api.response.GroupMembersResponse;
import com.group3.vitamins.employeegroup.presentation.api.response.RemoveMemberResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "EmployeeGroup - 사원 그룹", description = "그룹 CRUD·구성원 — 조회는 전체 사용자, 변경은 ADMIN. 담당: 김동현")
@RestController
@RequestMapping("/api/v1/employee-groups")
@RequiredArgsConstructor
public class EmployeeGroupController {

    private final EmployeeGroupQueryUseCase queryUseCase;
    private final EmployeeGroupCommandUseCase commandUseCase;

    @Operation(summary = "그룹 목록 조회 (전체 사용자)",
            description = "그룹은 프로젝트 멤버 선택·페이지 권한 부여의 선택 도구라 일반 사용자도 목록을 본다. "
                    + "페이징 없음, 구성원 목록 미포함. keyword 로 그룹명 부분검색.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED")
    })
    @GetMapping
    public ApiResponse<GroupListResponse> listGroups(
            @Parameter(description = "그룹명 부분검색") @RequestParam(required = false) String keyword) {
        GroupListResponse data = new GroupListResponse(
                queryUseCase.listGroups(keyword).stream().map(GroupItemResponse::from).toList());
        return ApiResponse.success(EmployeeGroupResponseMessage.LIST_SUCCESS, data);
    }

    @Operation(summary = "그룹 생성 (ADMIN)",
            description = "빈 그룹을 만든다(구성원은 생성 후 별도 추가). 그룹명은 전역 중복 불가.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "GRP_INVALID_REQUEST — 그룹명 비었거나 길이 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "GRP_NAME_DUPLICATED")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<GroupCreateResponse> createGroup(
            @RequestBody CreateGroupRequest request, Authentication authentication) {
        GroupCreateResponse data = GroupCreateResponse.from(
                commandUseCase.create(request.toCommand(RequesterRole.from(authentication), authentication.getName())));
        return ApiResponse.created(EmployeeGroupResponseMessage.CREATED, data);
    }

    @Operation(summary = "그룹 이름·설명 수정 (ADMIN)",
            description = "전달한 필드만 수정한다. 그룹명 변경은 기존 권한에 영향을 주지 않는다. 응답은 목록과 같은 구조.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "GRP_INVALID_REQUEST — 수정할 필드 없음 또는 길이 초과"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "GRP_NOT_FOUND"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "GRP_NAME_DUPLICATED")
    })
    @PatchMapping("/{groupId}")
    public ApiResponse<GroupItemResponse> updateGroup(
            @PathVariable Long groupId,
            @RequestBody JsonNode requestBody,
            Authentication authentication) {
        commandUseCase.update(toUpdateCommand(groupId, requestBody, RequesterRole.from(authentication)));
        // 응답은 목록 구조 — 커밋 후 조회로 다시 읽어 memberCount·createdByName 을 포함해 조립한다.
        return ApiResponse.success(EmployeeGroupResponseMessage.UPDATED,
                GroupItemResponse.from(queryUseCase.getGroup(groupId)));
    }

    @Operation(summary = "그룹 삭제 (ADMIN)",
            description = "구성원이 있어도 삭제된다(매핑은 CASCADE). 그룹을 지워도 이미 부여된 권한은 사라지지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "GRP_NOT_FOUND")
    })
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long groupId, Authentication authentication) {
        commandUseCase.delete(new DeleteGroupCommand(RequesterRole.from(authentication), groupId));
        return ApiResponse.success(EmployeeGroupResponseMessage.DELETED);
    }

    @Operation(summary = "구성원 목록 조회 (전체 사용자)",
            description = "그룹 구성원을 이름 오름차순으로 돌려준다. 시스템 계정·퇴사자는 제외된다. 부서 경로·직급명·추가일 포함.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "GRP_NOT_FOUND")
    })
    @GetMapping("/{groupId}/members")
    public ApiResponse<GroupMembersResponse> listMembers(@PathVariable Long groupId) {
        return ApiResponse.success(EmployeeGroupResponseMessage.MEMBERS_SUCCESS,
                GroupMembersResponse.from(queryUseCase.getMembers(groupId)));
    }

    @Operation(summary = "구성원 추가 (ADMIN)",
            description = "여러 사번을 한 번에 추가한다. 이미 소속인 사번은 조용히 건너뛴다(멱등). 존재하지 않는 사번이 "
                    + "하나라도 있으면 전체를 거부하고, 시스템 계정은 추가할 수 없다. 구성원 추가는 기존 권한을 늘리지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "GRP_INVALID_REQUEST — userIds 비어 있음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED · ACC_SYSTEM_ACCOUNT_NOT_ALLOWED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "GRP_NOT_FOUND · EMP_NOT_FOUND(존재하지 않는 사번 포함 → 전체 거부)")
    })
    @PostMapping("/{groupId}/members")
    public ApiResponse<AddMembersResponse> addMembers(
            @PathVariable Long groupId,
            @RequestBody AddMembersRequest request,
            Authentication authentication) {
        return ApiResponse.success(EmployeeGroupResponseMessage.MEMBERS_ADDED,
                AddMembersResponse.from(commandUseCase.addMembers(
                        request.toCommand(RequesterRole.from(authentication), groupId))));
    }

    @Operation(summary = "구성원 제거 (ADMIN)",
            description = "구성원을 한 명 제거한다. 제거해도 그 사원이 이미 받은 권한은 사라지지 않는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "ACC_ADMIN_REQUIRED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "GRP_NOT_FOUND · GRP_MEMBER_NOT_FOUND")
    })
    @DeleteMapping("/{groupId}/members/{userId}")
    public ApiResponse<RemoveMemberResponse> removeMember(
            @PathVariable Long groupId,
            @PathVariable String userId,
            Authentication authentication) {
        return ApiResponse.success(EmployeeGroupResponseMessage.MEMBER_REMOVED,
                RemoveMemberResponse.from(commandUseCase.removeMember(
                        new RemoveMemberCommand(RequesterRole.from(authentication), groupId, userId))));
    }

    /**
     * raw JSON 에서 필드 존재 여부(생략 vs null 전달)를 판별해 커맨드로 옮긴다 (employee 수정 선례).
     * description 은 null 로 지울 수 있어 "전달됨" 으로 취급한다. 문자열 아닌 타입이면 400.
     */
    private UpdateGroupCommand toUpdateCommand(Long groupId, JsonNode body, String role) {
        return new UpdateGroupCommand(
                role, groupId,
                body.has("name"), textOrNull(body, "name"),
                body.has("description"), textOrNull(body, "description"));
    }

    private String textOrNull(JsonNode body, String field) {
        if (!body.has(field)) {
            return null;
        }
        JsonNode node = body.get(field);
        if (node.isNull()) {
            return null;
        }
        if (!node.isTextual()) {
            throw new com.group3.vitamins.global.domain.common.error.exception.ValidationException(
                    com.group3.vitamins.employeegroup.domain.exception.EmployeeGroupErrorCode.GRP_INVALID_REQUEST);
        }
        return node.asText();
    }
}
