package com.group3.vitamins.image.presentation;

import com.group3.vitamins.image.application.command.CreateImageItemsCommand;
import com.group3.vitamins.image.application.command.DeleteImageItemCommand;
import com.group3.vitamins.image.application.command.UpdateImageItemsCommand;
import com.group3.vitamins.image.application.usecase.ImageCommandUseCase;
import com.group3.vitamins.image.application.usecase.ImageCommandUseCase.CreateImageItemsView;
import com.group3.vitamins.image.application.usecase.ImageCommandUseCase.UpdateImageItemsView;
import com.group3.vitamins.image.presentation.api.request.ImageItemCreateRequest;
import com.group3.vitamins.image.presentation.api.request.ImageItemUpdateRequest;
import com.group3.vitamins.image.presentation.api.response.CreateImageItemsResponse;
import com.group3.vitamins.image.presentation.api.response.ImageItemResponse;
import com.group3.vitamins.image.presentation.api.response.UpdateImageItemsResponse;
import com.group3.vitamins.image.presentation.api.response.UpdatedImageOrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 이미지 항목 API.
 *
 * <p>이미지 블록 자체의 생성·삭제는 이 컨트롤러에 없다 — 블록 생성·삭제는 Block 도메인(동훈님)이 전담한다.
 * 여기는 항목(내부 데이터) 생성·수정·삭제만 다룬다.
 */
@Tag(name = "Image", description = "이미지 블록 API")
@RestController
@RequestMapping("/api/v1/blocks/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageCommandUseCase imageCommandUseCase;
    private final ObjectMapper objectMapper;

    @Operation(summary = "이미지 항목 생성", description = "이미지 블록에 새 이미지들을 추가한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "이미지 항목 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "지원하지 않는 파일 형식입니다. (IMG-001) / 이미지 개수와 캡션 개수가 일치하지 않습니다. (IMG-004)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (IMG-002) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 블록입니다. (IMG-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @PostMapping(value = "/{imgBlockId}/items", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<CreateImageItemsResponse>> createItems(
            @Parameter(description = "이미지 항목을 생성할 블록의 ID", example = "1")
            @PathVariable Long imgBlockId,
            @Parameter(description = "업로드할 이미지 파일들 (정렬된 순서 그대로 전송)")
            @RequestPart("files") List<MultipartFile> files,
            @Parameter(description = "{ \"captions\": [\"...\"] } 형태의 JSON 문자열",
                    example = "{\"captions\": [\"회의실 전경\", \"\", \"화이트보드\"]}")
            @RequestPart(value = "request", required = false) String requestJson,
            @AuthenticationPrincipal String userId
    ) {
        // @RequestPart(ImageItemCreateRequest) 로 바로 받으면, 클라이언트가 이 파트에
        // Content-Type을 안 붙였을 때(Swagger UI·일부 클라이언트의 기본 동작) Spring이
        // application/octet-stream으로 간주해 JSON 컨버터를 못 찾고 415로 거부한다.
        // 문자열로 받아 직접 파싱하면 파트의 Content-Type과 무관하게 항상 읽힌다.
        List<String> captions = parseCaptions(requestJson);

        CreateImageItemsView view = imageCommandUseCase.create(
                new CreateImageItemsCommand(userId, imgBlockId, files, captions));

        List<ImageItemResponse> images = view.images().stream()
                .map(image -> new ImageItemResponse(
                        image.imgId(),
                        image.originalName(),
                        image.imageUrl(),
                        image.caption(),
                        image.orderIndex(),
                        image.createdAt()
                ))
                .toList();

        CreateImageItemsResponse data = new CreateImageItemsResponse(view.imgBlockId(), images);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("이미지 항목 생성 성공", data));
    }

    @Operation(summary = "이미지 항목 수정", description = "이미지 블록의 항목 순서와 캡션을 한 번에 수정한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청한 이미지 목록이 유효하지 않습니다. (IMG-005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (IMG-002) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 블록입니다. (IMG-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @PatchMapping("/items/{imgBlockId}")
    public ResponseEntity<ApiResponse<UpdateImageItemsResponse>> updateItems(
            @Parameter(description = "수정할 이미지 블록 ID", example = "1")
            @PathVariable Long imgBlockId,
            @RequestBody ImageItemUpdateRequest request,
            @AuthenticationPrincipal String userId
    ) {
        List<UpdateImageItemsCommand.Entry> entries = request.images().stream()
                .map(entry -> new UpdateImageItemsCommand.Entry(entry.imgId(), entry.caption()))
                .toList();

        UpdateImageItemsView view = imageCommandUseCase.updateItems(
                new UpdateImageItemsCommand(userId, imgBlockId, entries));

        List<UpdatedImageOrderResponse> images = view.images().stream()
                .map(image -> new UpdatedImageOrderResponse(image.imgId(), image.orderIndex(), image.caption()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("이미지 수정 성공", new UpdateImageItemsResponse(images)));
    }

    @Operation(summary = "이미지 항목 삭제", description = "이미지 항목을 삭제한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 항목 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (IMG-002) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 항목입니다. (IMG-006)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @DeleteMapping("/items/{imgId}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @Parameter(description = "삭제할 이미지 항목 ID", example = "1")
            @PathVariable Long imgId,
            @AuthenticationPrincipal String userId
    ) {
        imageCommandUseCase.delete(new DeleteImageItemCommand(userId, imgId));

        return ResponseEntity.ok(ApiResponse.success("이미지 항목 삭제 성공"));
    }

    private List<String> parseCaptions(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(requestJson, ImageItemCreateRequest.class).captions();
        } catch (JsonProcessingException e) {
            throw new HttpMessageNotReadableException("request 파트의 JSON 형식이 올바르지 않습니다.", e, null);
        }
    }
}
