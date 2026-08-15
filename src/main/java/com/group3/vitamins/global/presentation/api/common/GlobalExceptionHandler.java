package com.group3.vitamins.global.presentation.api.common;

import com.group3.vitamins.global.domain.common.error.DomainException;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.util.DisconnectedClientHelper;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 전역 예외 → 명세 형식의 실패 응답 변환.
 *
 * <pre>
 *   { "httpStatus": 401, "message": "...", "code": "AUTH_UNAUTHENTICATED" }
 * </pre>
 *
 * <p>Spring Security 필터 단계의 401·403 도 {@code CustomAuthenticationEntryPoint} ·
 * {@code CustomAccessDeniedHandler} 가 {@code HandlerExceptionResolver} 로 넘겨 여기로 온다.
 * 덕분에 <b>응답 형식과 에러 코드가 이 파일 하나에서만 정의된다.</b>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 401 은 전 도메인이 같은 코드를 쓴다 (.ai/api/ 전 파일 공통)
    private static final String AUTH_UNAUTHENTICATED = "AUTH_UNAUTHENTICATED";
    private static final String AUTH_UNAUTHENTICATED_MESSAGE = "로그인이 필요합니다.";

    // 아래 5개는 명세에 대응 코드가 없는 프레임워크 레벨 오류의 폴백이다.
    // 도메인 에러는 반드시 명세의 코드(ACC_NOT_FOUND · EMP_INVALID_REQUEST …)를 쓴다.
    private static final String COMMON_FORBIDDEN = "COMMON_FORBIDDEN";
    private static final String COMMON_FORBIDDEN_MESSAGE = "접근 권한이 없습니다.";
    private static final String COMMON_INVALID_REQUEST = "COMMON_INVALID_REQUEST";
    private static final String COMMON_INVALID_REQUEST_MESSAGE = "잘못된 요청입니다.";
    private static final String COMMON_NOT_FOUND = "COMMON_NOT_FOUND";
    private static final String COMMON_NOT_FOUND_MESSAGE = "요청한 경로를 찾을 수 없습니다.";
    private static final String COMMON_METHOD_NOT_ALLOWED = "COMMON_METHOD_NOT_ALLOWED";
    private static final String COMMON_METHOD_NOT_ALLOWED_MESSAGE = "지원하지 않는 요청 방식입니다.";
    private static final String COMMON_INTERNAL_ERROR = "COMMON_INTERNAL_ERROR";
    private static final String COMMON_INTERNAL_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";

    /**
     * Bean Validation 메시지에 {@code "ERROR_CODE|사용자 문구"} 형태를 허용한다.
     *
     * <p>기본 동작으로는 {@code @NotBlank}·{@code @Size} 위반이 전부 {@code COMMON_INVALID_REQUEST} 로
     * 나가 <b>명세가 정한 도메인 에러코드를 내려줄 수 없었다</b>. 그래서 검증을 애노테이션으로 못 쓰고
     * 서비스에서 손으로 던져 왔다 ({@code .ai/docs/domain/관리자/BCT-V1-API.md} §3-5 B2).
     *
     * <p>구분자가 없는 기존 메시지("사번을 입력해 주세요.", "must not be blank")는 이 패턴에 걸리지 않아
     * <b>지금까지와 완전히 동일하게</b> {@code COMMON_INVALID_REQUEST} 로 나간다.
     */
    private static final Pattern CODED_MESSAGE = Pattern.compile(
            "^([A-Z][A-Z0-9_]{2,})\\|(.+)$", Pattern.DOTALL);

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(
            DomainException e,
            HttpServletRequest request
    ) {
        log.warn("[{}] {} {} - {} : {}",
                e.getHttpStatus(), request.getMethod(), request.getRequestURI(),
                e.getErrorCode().getCode(), e.getMessage());

        // e.getMessage() 를 쓴다 — 잠금 해제 시각처럼 상황별 메시지를 담는 경우가 있다 (auth.md)
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiErrorResponse.of(e.getHttpStatus(), e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException e,
            HttpServletRequest request
    ) {
        Optional<ResponseEntity<ApiErrorResponse>> coded = e.getBindingResult().getFieldErrors().stream()
                .map(error -> codedBadRequest(error.getDefaultMessage(), request))
                .flatMap(Optional::stream)
                .findFirst();
        if (coded.isPresent()) {
            return coded.get();
        }

        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(COMMON_INVALID_REQUEST_MESSAGE);

        return badRequest(message, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e,
            HttpServletRequest request
    ) {
        Optional<ResponseEntity<ApiErrorResponse>> coded = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .map(message -> codedBadRequest(message, request))
                .flatMap(Optional::stream)
                .findFirst();
        if (coded.isPresent()) {
            return coded.get();
        }

        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .filter(this::hasText)
                .collect(Collectors.joining(", "));

        return badRequest(hasText(message) ? message : COMMON_INVALID_REQUEST_MESSAGE, request);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            ServletRequestBindingException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequestException(
            Exception e,
            HttpServletRequest request
    ) {
        return badRequest(COMMON_INVALID_REQUEST_MESSAGE, request);
    }

    /** 미인증 — Security 필터에서 넘어온다 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException e,
            HttpServletRequest request
    ) {
        log.debug("[401] {} {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());

        return ResponseEntity.status(401).body(ApiErrorResponse.of(
                401, AUTH_UNAUTHENTICATED, AUTH_UNAUTHENTICATED_MESSAGE));
    }

    /** 인증은 됐으나 권한 부족 — 도메인별 403 은 DomainException 으로 처리된다 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDeniedException(
            AccessDeniedException e,
            HttpServletRequest request
    ) {
        log.warn("[403] {} {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(403).body(ApiErrorResponse.of(
                403, COMMON_FORBIDDEN, COMMON_FORBIDDEN_MESSAGE));
    }

    /**
     * 존재하지 않는 경로 — {@code 404}.
     *
     * <p>🚨 <b>이 핸들러가 없으면 아래 {@code Exception} 폴백이 삼켜 500 이 나간다.</b>
     * 프론트가 "URL 오타" 와 "서버 장애" 를 구분하지 못하고, 정상 상황인데 {@code ERROR} 로그가 쌓인다.
     *
     * <p>Spring Boot 3 은 매핑 없는 요청을 정적 리소스로 넘겨 {@link NoResourceFoundException} 을 던진다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(
            NoResourceFoundException e,
            HttpServletRequest request
    ) {
        log.warn("[404] {} {}", request.getMethod(), request.getRequestURI());

        return ResponseEntity.status(404).body(ApiErrorResponse.of(
                404, COMMON_NOT_FOUND, COMMON_NOT_FOUND_MESSAGE));
    }

    /**
     * 경로는 맞지만 HTTP 메서드가 없는 경우 — {@code 405}.
     *
     * <p>🚨 <b>{@link NoResourceFoundException} 과 같은 이유로 필요하다.</b> 이 핸들러가 없으면 아래
     * {@code Exception} 폴백이 삼켜 <b>500 이 나가고</b>, 프론트가 "메서드를 잘못 썼다"(프론트 버그)와
     * "서버가 터졌다"(장애)를 구분하지 못한다. 실제로 {@code PATCH /api/v1/notifications} 처럼
     * GET 만 있는 경로에 다른 메서드를 쓰면 500 이 나가고 있었다 (2026-08-07 발견).
     *
     * <p>{@code Allow} 헤더에 지원 메서드를 담아 준다 — RFC 9110 이 405 응답에 요구하는 헤더다.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request
    ) {
        log.warn("[405] {} {} - 지원 메서드: {}",
                request.getMethod(), request.getRequestURI(), e.getSupportedHttpMethods());

        // RFC 9110 §15.5.6 — 405 응답은 Allow 헤더를 반드시 포함해야 한다.
        // 지원 메서드를 모르는 경우(예외가 목록 없이 만들어진 경우)에도 빈 Allow 를 내보낸다 —
        // 없는 메서드를 지어내지 않으면서 "모든 405 에 Allow 가 있다"는 계약은 지킨다.
        Set<HttpMethod> supported = e.getSupportedHttpMethods();
        return ResponseEntity.status(405)
                .allow(supported == null ? new HttpMethod[0] : supported.toArray(new HttpMethod[0]))
                .body(ApiErrorResponse.of(
                        405, COMMON_METHOD_NOT_ALLOWED, COMMON_METHOD_NOT_ALLOWED_MESSAGE));
    }

    /**
     * 클라이언트가 끊어 버린 응답에 쓰다 난 I/O 오류 — <b>장애가 아니다</b>.
     *
     * <p>🚨 <b>이 핸들러가 없으면 아래 {@code Exception} 폴백이 삼켜 ERROR 500 이 쌓인다.</b>
     * 브라우저가 알림 SSE 스트림을 끊을 때마다(페이지 이동·새로고침·탭 닫기) 컨테이너가 그 오류를
     * 비동기 디스패치로 되돌려 보내기 때문이다. 정상 종료 한 건이 ERROR 한 줄이 되면 진짜 장애가 묻힌다.
     *
     * <p>⚠️ <b>{@code DisconnectedClientHelper} 에만 의존하면 안 된다.</b> Spring 의 "끊긴 클라이언트"
     * 판별은 예외 메시지가 {@code "broken pipe"} · {@code "connection reset by peer"} 인지를 보는데,
     * <b>OS 로케일이 영어가 아니면 그 문자열이 아니다</b>(한글 윈도우: "현재 연결은 사용자의 호스트
     * 시스템의 소프트웨어에 의해 중단되었습니다"). 그러면 Spring 이 감싸주지 않아 원본
     * {@code IOException} 이 그대로 올라온다 — 로컬(윈도우)에서만 500 이 뜨고 배포 서버(리눅스)에선
     * 안 뜨는, 재현이 갈리는 증상이 된다. 그래서 <b>디스패치 타입</b>을 1차 기준으로 삼는다(2026-08-14).
     *
     * <p>⚠️ 끊긴 연결에는 <b>아무것도 쓰지 않는다</b>({@code null} 반환). 응답 헤더가 이미
     * {@code text/event-stream} 으로 굳어 있어 {@link ApiErrorResponse} 를 쓰려 하면
     * {@code HttpMessageNotWritableException} 이 한 번 더 난다. {@code null} 은 Spring 이
     * "처리 완료, 쓸 것 없음" 으로 받는다({@code HttpEntityMethodProcessor} 확인).
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiErrorResponse> handleClientDisconnect(
            IOException e,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (!cannotReportError(e, request, response)) {
            // 에러 응답을 정상적으로 내려보낼 수 있는 상황이면 진짜 장애다 — 기존대로 500 JSON
            return handleException(e, request);
        }

        log.debug("[스트림 종료] {} {} - {}",
                request.getMethod(), request.getRequestURI(), e.getMessage());
        return null;
    }

    /**
     * 이 오류를 <b>에러 응답으로 알려줄 수 없는 상황</b>인지 판단한다. 그런 상황에서만 조용히 끝낸다.
     *
     * <p>기준이 "클라이언트가 끊겼나" 가 아니라 "에러를 내려보낼 수 있나" 인 이유: 알려줄 수 없는데
     * 500 을 만들어 봐야 <b>로그만 남고 클라이언트는 아무것도 못 받는다</b>. 반대로 알려줄 수 있는
     * 상황이면 그것은 삼키면 안 되는 장애다.
     *
     * <p>⚠️ <b>{@code DispatcherType.ASYNC} 하나만으로 판단하면 안 된다.</b> 이 핸들러는 전역이라,
     * 앞으로 추가될 다른 비동기 엔드포인트가 파일·네트워크 I/O 로 실패했을 때까지 조용히 삼켜
     * <b>클라이언트가 빈 200 을 받는다</b>(CodeRabbit 지적, 2026-08-14). 지금은 비동기 엔드포인트가
     * 알림 SSE 하나뿐이라 드러나지 않을 뿐이다. 그래서 <b>아직 응답을 쓰지 않은 비동기 요청은
     * 500 으로 보낸다</b> — 그때는 에러를 정상적으로 알려줄 수 있다.
     */
    private boolean cannotReportError(
            IOException e, HttpServletRequest request, HttpServletResponse response) {

        // 클라이언트가 끊긴 것이 확실한 경우(영문 로케일 등) — 디스패치 타입과 무관하게 알릴 방법이 없다
        if (DisconnectedClientHelper.isClientDisconnectedException(e)) {
            return true;
        }

        if (request.getDispatcherType() != DispatcherType.ASYNC) {
            return false;
        }

        // 이미 커밋됐으면 상태코드·바디를 못 바꾸고, SSE 응답이면 ApiErrorResponse 컨버터가 없다.
        // 두 경우 모두 "쓸 수 있는 방법이 없는" 상태다.
        return response.isCommitted() || isEventStream(response);
    }

    private boolean isEventStream(HttpServletResponse response) {
        String contentType = response.getContentType();
        return contentType != null
                && contentType.startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("[500] {} {}", request.getMethod(), request.getRequestURI(), e);

        return ResponseEntity.status(500).body(ApiErrorResponse.of(
                500, COMMON_INTERNAL_ERROR, COMMON_INTERNAL_ERROR_MESSAGE));
    }

    /**
     * 검증 메시지가 {@code "ERROR_CODE|문구"} 형태면 그 코드로 400 응답을 만든다.
     * 형태가 아니면 empty 를 돌려주고 호출부가 기존 폴백을 그대로 탄다.
     */
    private Optional<ResponseEntity<ApiErrorResponse>> codedBadRequest(
            String rawMessage, HttpServletRequest request) {
        if (rawMessage == null) {
            return Optional.empty();
        }
        Matcher matcher = CODED_MESSAGE.matcher(rawMessage);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String code = matcher.group(1);
        String message = matcher.group(2);
        log.warn("[400] {} {} - {} : {}",
                request.getMethod(), request.getRequestURI(), code, message);

        return Optional.of(ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, code, message)));
    }

    private ResponseEntity<ApiErrorResponse> badRequest(String message, HttpServletRequest request) {
        log.warn("[400] {} {} - {}", request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, COMMON_INVALID_REQUEST, message));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
