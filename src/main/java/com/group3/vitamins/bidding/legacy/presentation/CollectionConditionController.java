package com.group3.vitamins.bidding.legacy.presentation;

import com.group3.vitamins.bidding.collectioncondition.application.result.CollectionConditionResult;
import com.group3.vitamins.bidding.collectioncondition.application.usecase.CollectionConditionUseCase;
import com.group3.vitamins.bidding.collectioncondition.presentation.api.request.CreateCollectionConditionRequest;
import com.group3.vitamins.bidding.collectioncondition.presentation.api.request.UpdateCollectionConditionRequest;
import com.group3.vitamins.bidding.collectioncondition.presentation.api.response.CollectionConditionListResponse;
import com.group3.vitamins.bidding.collectioncondition.presentation.api.response.CollectionConditionResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Bidding - 입찰 공고 수집 조건",
        description = "회사별 입찰 공고 수집 조건을 관리합니다."
)
@RestController
@RequestMapping("/api/v1/bidding/collection-conditions")
@RequiredArgsConstructor
public class CollectionConditionController {

    private final CollectionConditionUseCase collectionConditionUseCase;

    @Operation(
            summary = "수집 조건 목록 조회",
            description = "현재 회사의 삭제되지 않은 수집 조건을 최신 등록 순으로 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수집 조건 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED - 세션 없음 또는 만료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED - 입찰 관리 권한 없음"
            )
    })
    @GetMapping
    public ResponseEntity<ApiResponse<CollectionConditionListResponse>> getAll(
            Authentication authentication
    ) {
        CollectionConditionListResponse response =
                CollectionConditionListResponse.from(
                        collectionConditionUseCase.getAll(
                                authentication.getName(),
                                RequesterRole.from(authentication)
                        )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "입찰 공고 수집 조건 목록 조회 성공",
                        response
                )
        );
    }

    @Operation(
            summary = "수집 조건 등록",
            description = "현재 회사에 새로운 입찰 공고 수집 조건을 등록합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "수집 조건 등록 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BIDDING_INVALID_COLLECTION_CONDITION / "
                            + "BIDDING_COLLECTION_QUERY_LIMIT_EXCEEDED / "
                            + "BIDDING_UNSUPPORTED_SOURCE"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED - 세션 없음 또는 만료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED - 입찰 관리 권한 없음"
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CollectionConditionResponse>> create(
            @RequestBody CreateCollectionConditionRequest request,
            Authentication authentication
    ) {
        CollectionConditionResult result =
                collectionConditionUseCase.create(request.toCommand(
                        authentication.getName(),
                        RequesterRole.from(authentication)
                ));

        return ResponseEntity.status(201).body(
                ApiResponse.created(
                        "입찰 공고 수집 조건 등록 성공",
                        CollectionConditionResponse.from(result)
                )
        );
    }

    @Operation(
            summary = "수집 조건 수정",
            description = "수집 조건의 이름, 공고 유형, 필터 및 활성화 여부를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수집 조건 수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BIDDING_INVALID_COLLECTION_CONDITION / "
                            + "BIDDING_COLLECTION_QUERY_LIMIT_EXCEEDED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED - 세션 없음 또는 만료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED - 입찰 관리 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_COLLECTION_CONDITION_NOT_FOUND"
            )
    })
    @PatchMapping("/{conditionId}")
    public ResponseEntity<ApiResponse<CollectionConditionResponse>> update(
            @Parameter(description = "수정할 수집 조건 ID")
            @PathVariable Long conditionId,
            @RequestBody UpdateCollectionConditionRequest request,
            Authentication authentication
    ) {
        CollectionConditionResult result =
                collectionConditionUseCase.update(
                        request.toCommand(
                                conditionId,
                                authentication.getName(),
                                RequesterRole.from(authentication)
                        )
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "입찰 공고 수집 조건 수정 성공",
                        CollectionConditionResponse.from(result)
                )
        );
    }
}
