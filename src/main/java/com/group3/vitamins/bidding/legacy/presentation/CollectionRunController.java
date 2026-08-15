package com.group3.vitamins.bidding.legacy.presentation;

import com.group3.vitamins.bidding.collectionrun.application.query.GetCollectionRunQuery;
import com.group3.vitamins.bidding.collectionrun.application.result.CollectionRunResult;
import com.group3.vitamins.bidding.collectionrun.application.usecase.CollectionRunUseCase;
import com.group3.vitamins.bidding.collectionrun.presentation.api.request.StartCollectionRunRequest;
import com.group3.vitamins.bidding.collectionrun.presentation.api.response.CollectionRunResponse;
import com.group3.vitamins.bidding.collectionrun.presentation.api.response.StartCollectionRunResponse;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Bidding - 입찰 공고 수집 실행",
        description = "입찰 공고 수집을 실행하고 처리 결과를 조회합니다."
)
@RestController
@RequestMapping("/api/v1/bidding")
@RequiredArgsConstructor
public class CollectionRunController {

    private final CollectionRunUseCase collectionRunUseCase;

    @Operation(
            summary = "입찰 공고 수동 수집",
            description = "선택한 수집 조건으로 입찰 공고 수집 작업을 요청합니다. "
                    + "요청 본문의 startedAt·endedAt을 함께 지정하면 그 구간을 우선 조회하고, "
                    + "생략하면 조건에 저장된 lookbackPeriod로 자동 계산한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "202",
                    description = "수집 요청 접수 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BIDDING_INACTIVE_COLLECTION_CONDITION · "
                            + "BIDDING_COLLECTION_RUN_RANGE_INVALID · "
                            + "BIDDING_COLLECTION_RUN_RANGE_TOO_WIDE"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED - 세션 없음 또는 만료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_COLLECTION_CONDITION_NOT_FOUND"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "BIDDING_COLLECTION_RUN_ALREADY_PROCESSING"
            )
    })
    @PostMapping("/collection-conditions/{conditionId}/runs")
    public ResponseEntity<ApiResponse<StartCollectionRunResponse>> start(
            @AuthenticationPrincipal String userId,
            @Parameter(description = "실행할 수집 조건 ID")
            @PathVariable Long conditionId,
            @RequestBody(required = false) StartCollectionRunRequest request
    ) {
        StartCollectionRunRequest safeRequest =
                request == null ? StartCollectionRunRequest.EMPTY : request;

        CollectionRunResult result = collectionRunUseCase.start(
                safeRequest.toCommand(conditionId, userId)
        );

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                ApiResponse.of(
                        HttpStatus.ACCEPTED.value(),
                        "입찰 공고 수집 요청이 접수되었습니다.",
                        StartCollectionRunResponse.from(result)
                )
        );
    }

    @Operation(
            summary = "수집 실행 결과 조회",
            description = "수집 작업의 처리 상태, 수집 건수 및 오류 내용을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수집 실행 결과 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "BIDDING_INVALID_COLLECTION_RUN_REQUEST"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "AUTH_UNAUTHENTICATED - 세션 없음 또는 만료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "BIDDING_ACCESS_PERMISSION_REQUIRED"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "BIDDING_COLLECTION_RUN_NOT_FOUND"
            )
    })
    @GetMapping("/collection-runs/{runId}")
    public ResponseEntity<ApiResponse<CollectionRunResponse>> get(
            @Parameter(description = "수집 실행 ID")
            @PathVariable Long runId
    ) {
        CollectionRunResult result = collectionRunUseCase.get(
                new GetCollectionRunQuery(runId)
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "입찰 공고 수집 결과 조회 성공",
                        CollectionRunResponse.from(result)
                )
        );
    }
}
