package com.group3.vitamins.image.presentation;

import com.group3.vitamins.image.application.command.CreateImageItemsCommand;
import com.group3.vitamins.image.application.command.DeleteImageItemCommand;
import com.group3.vitamins.image.application.command.PurgeImageItemsCommand;
import com.group3.vitamins.image.application.command.RestoreImageItemsCommand;
import com.group3.vitamins.image.application.command.UpdateImageItemsCommand;
import com.group3.vitamins.image.application.query.GetImageDownloadQuery;
import com.group3.vitamins.image.application.query.GetImageItemQuery;
import com.group3.vitamins.image.application.query.ImageNavigationDirection;
import com.group3.vitamins.image.application.usecase.ImageCommandUseCase;
import com.group3.vitamins.image.application.usecase.ImageCommandUseCase.CreateImageItemsView;
import com.group3.vitamins.image.application.usecase.ImageCommandUseCase.RestoreImageItemsView;
import com.group3.vitamins.image.application.usecase.ImageCommandUseCase.UpdateImageItemsView;
import com.group3.vitamins.image.application.usecase.ImageQueryUseCase;
import com.group3.vitamins.image.application.usecase.ImageQueryUseCase.ImageDownloadView;
import com.group3.vitamins.image.application.usecase.ImageQueryUseCase.ImageItemView;
import com.group3.vitamins.image.domain.exception.ImageErrorCode;
import com.group3.vitamins.image.presentation.api.request.ImageItemCreateRequest;
import com.group3.vitamins.image.presentation.api.request.ImageItemPurgeRequest;
import com.group3.vitamins.image.presentation.api.request.ImageItemRestoreRequest;
import com.group3.vitamins.image.presentation.api.request.ImageItemUpdateRequest;
import com.group3.vitamins.image.presentation.api.response.CreateImageItemsResponse;
import com.group3.vitamins.image.presentation.api.response.ImageItemQueryResponse;
import com.group3.vitamins.image.presentation.api.response.ImageItemResponse;
import com.group3.vitamins.image.presentation.api.response.RestoreImageItemsResponse;
import com.group3.vitamins.image.presentation.api.response.RestoredImageItemResponse;
import com.group3.vitamins.image.presentation.api.response.UpdateImageItemsResponse;
import com.group3.vitamins.image.presentation.api.response.UpdatedImageOrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import com.group3.vitamins.global.presentation.api.common.ApiResponse;
import com.group3.vitamins.global.presentation.api.common.RequesterRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

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
    private final ImageQueryUseCase imageQueryUseCase;
    private final ObjectMapper objectMapper;

    @Operation(summary = "이미지 항목 조회", description = "현재 이미지의 정렬 번호(currentOrderIndex) 기준으로 "
            + "다음/이전 이미지 한 장을 조회한다. 마지막에서 다음을 누르면 첫 이미지로, 처음에서 이전을 누르면 "
            + "마지막 이미지로 순환한다. 프론트는 이 응답을 캐싱하지 말고 다음/이전 클릭마다 매번 호출할 것 "
            + "(presigned URL이 1시간마다 만료되고, 그 사이 추가된 이미지도 놓치지 않기 위함).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 항목 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "접근 권한이 없습니다. (IMG-007) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 항목입니다. (IMG-006) / 존재하지 않는 블록입니다. (IMG-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @GetMapping("/{imgBlockId}/items/{currentOrderIndex}")
    public ResponseEntity<ApiResponse<ImageItemQueryResponse>> getItem(
            @Parameter(description = "조회할 이미지 블록 ID", example = "1")
            @PathVariable Long imgBlockId,
            @Parameter(description = "현재 보고 있는 이미지의 정렬 번호", example = "2")
            @PathVariable int currentOrderIndex,
            @Parameter(description = "다음(next) 또는 이전(prev)", example = "next")
            @RequestParam String direction,
            Authentication authentication
    ) {
        ImageItemView view = imageQueryUseCase.getItem(new GetImageItemQuery(
                authentication.getName(), imgBlockId, currentOrderIndex,
                ImageNavigationDirection.valueOf(direction.toUpperCase()),
                RequesterRole.from(authentication)));

        ImageItemQueryResponse data = new ImageItemQueryResponse(
                view.imgId(), view.originalName(), view.imageUrl(),
                view.caption(), view.orderIndex(), view.totalCount());

        return ResponseEntity.ok(ApiResponse.success("이미지 항목 조회 성공", data));
    }

    @Operation(summary = "이미지 다운로드", description = "imgId를 지정하면 그 이미지 한 장을 원본 파일명으로 응답하고, "
            + "생략하면 그 블록에 속한 활성 이미지 전체를 블록명.zip으로 압축해서 응답한다. "
            + "JSON이 아니라 파일 바이너리를 응답 바디에 직접 담아 내려준다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 다운로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "접근 권한이 없습니다. (IMG-007) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 항목입니다. (IMG-006) / 다운로드할 이미지가 없습니다. (IMG-008) / 존재하지 않는 블록입니다. (IMG-003)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @GetMapping("/{imgBlockId}/download")
    public ResponseEntity<byte[]> download(
            @Parameter(description = "다운로드할 이미지 블록 ID(블록 이름으로 zip 생성)", example = "1")
            @PathVariable Long imgBlockId,
            @Parameter(description = "다운로드할 이미지 ID. 생략하면 블록 전체를 zip으로 다운로드", example = "10")
            @RequestParam(required = false) Long imgId,
            Authentication authentication
    ) {
        ImageDownloadView view = imageQueryUseCase.getDownload(new GetImageDownloadQuery(
                authentication.getName(), imgBlockId, imgId, RequesterRole.from(authentication)));

        // HTTP 헤더 값은 ISO-8859-1로 인코딩되어 나가서 한글을 filename="..."에 그대로 넣으면 깨진다
        // (심하면 헤더 자체가 망가져 filename*= 까지 못 읽힐 수 있다). 그래서 filename=에는 ASCII
        // 폴백만 넣고, 실제 한글 이름은 퍼센트 인코딩된 filename*=UTF-8''...에만 싣는다 (RFC 5987/6266).
        int dotIndex = view.fileName().lastIndexOf('.');
        String extension = dotIndex >= 0 ? view.fileName().substring(dotIndex) : "";
        String asciiFallbackName = "download" + extension;
        String encodedFileName = URLEncoder.encode(view.fileName(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(view.contentType()))
                // 브라우저가 Content-Type을 무시하고 내용을 추측(MIME sniffing)해서 실행 가능한
                // 콘텐츠로 잘못 해석하는 걸 막는다 — 업로드 검증을 통과한 파일이어도 방어적으로 둔다.
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiFallbackName + "\"; filename*=UTF-8''" + encodedFileName)
                .body(view.content());
    }

    @Operation(summary = "이미지 항목 생성", description = "이미지 블록에 새 이미지들을 추가한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "이미지 항목 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "지원하지 않는 파일 형식입니다. (IMG-001) / 이미지 개수와 캡션 개수가 일치하지 않습니다. (IMG-004) / "
                            + "한 번에 업로드할 수 있는 파일 개수를 초과했습니다. (IMG-010, 최대 20장)"),
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
            Authentication authentication
    ) {
        // @RequestPart(ImageItemCreateRequest) 로 바로 받으면, 클라이언트가 이 파트에
        // Content-Type을 안 붙였을 때(Swagger UI·일부 클라이언트의 기본 동작) Spring이
        // application/octet-stream으로 간주해 JSON 컨버터를 못 찾고 415로 거부한다.
        // 문자열로 받아 직접 파싱하면 파트의 Content-Type과 무관하게 항상 읽힌다.
        List<String> captions = parseCaptions(requestJson);

        CreateImageItemsView view = imageCommandUseCase.create(
                new CreateImageItemsCommand(authentication.getName(), imgBlockId, files, captions,
                        RequesterRole.from(authentication)));

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
            Authentication authentication
    ) {
        // 배열 자체가 없거나(null), 배열 안에 항목 하나가 null로 온 경우 — 후자는 이 뒤 map()에서
        // entry.imgId() 호출 시 NPE(500)로 새는데, 클라이언트가 보낼 수 있는 형식 오류라 500이 아니라
        // 문서화된 IMG-005로 거부해야 한다(2026-08-06, 코드 리뷰로 발견).
        if (request.images() == null || request.images().stream().anyMatch(Objects::isNull)) {
            throw new ValidationException(ImageErrorCode.INVALID_IMAGE_LIST);
        }
        List<UpdateImageItemsCommand.Entry> entries = request.images().stream()
                .map(entry -> new UpdateImageItemsCommand.Entry(entry.imgId(), entry.caption()))
                .toList();

        UpdateImageItemsView view = imageCommandUseCase.updateItems(
                new UpdateImageItemsCommand(authentication.getName(), imgBlockId, entries,
                        RequesterRole.from(authentication)));

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
            Authentication authentication
    ) {
        imageCommandUseCase.delete(new DeleteImageItemCommand(authentication.getName(), imgId,
                RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("이미지 항목 삭제 성공"));
    }

    @Operation(summary = "이미지 복구", description = "휴지통에 있는(소프트 삭제된) 이미지를 복구한다. "
            + "복구된 이미지는 원래 순서가 아니라 각자 속한 블록의 현재 활성 목록 맨 뒤에 붙는다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 복구 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청한 이미지 목록이 유효하지 않습니다. (IMG-005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (IMG-002, 블록이 삭제된 이미지는 STEP_EDIT_DENIED로 나갈 수 있음) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "존재하지 않는 항목입니다. (IMG-006) / 상위 블록이 삭제되어 하위 항목을 복구할 수 없습니다. (IMG-009)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @PatchMapping("/items/restore")
    public ResponseEntity<ApiResponse<RestoreImageItemsResponse>> restoreItems(
            @RequestBody ImageItemRestoreRequest request,
            Authentication authentication
    ) {
        RestoreImageItemsView view = imageCommandUseCase.restore(
                new RestoreImageItemsCommand(authentication.getName(), request.imgIds(),
                        RequesterRole.from(authentication)));

        List<RestoredImageItemResponse> images = view.images().stream()
                .map(image -> new RestoredImageItemResponse(
                        image.imgBlockId(), image.imgId(), image.originalName(), image.orderIndex()))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("이미지 복구 성공", new RestoreImageItemsResponse(images)));
    }

    @Operation(summary = "이미지 영구 삭제", description = "휴지통에 있는(소프트 삭제된) 이미지를 영구(하드 삭제) 삭제한다. "
            + "DB 행과 S3 객체가 모두 제거되며 되돌릴 수 없다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이미지 영구 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청한 이미지 목록이 유효하지 않습니다. (IMG-005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "편집 권한이 없습니다. (IMG-002, 블록이 삭제된 이미지는 STEP_EDIT_DENIED로 나갈 수 있음) / 초기 비밀번호를 먼저 변경해 주세요. (AUTH_PASSWORD_RESET_REQUIRED)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 항목입니다. (IMG-006, 휴지통에 없는 이미지 포함)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_UNAUTHENTICATED — 세션 없음/만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "COMMON_INTERNAL_ERROR — 서버 내부 오류입니다.")
    })
    @DeleteMapping("/items/hard")
    public ResponseEntity<ApiResponse<Void>> purgeItems(
            @RequestBody ImageItemPurgeRequest request,
            Authentication authentication
    ) {
        imageCommandUseCase.purge(new PurgeImageItemsCommand(authentication.getName(), request.imgIds(),
                RequesterRole.from(authentication)));

        return ResponseEntity.ok(ApiResponse.success("이미지 영구 삭제 성공"));
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
