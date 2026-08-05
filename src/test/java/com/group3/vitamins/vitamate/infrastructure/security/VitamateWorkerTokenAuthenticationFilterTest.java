package com.group3.vitamins.vitamate.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("VitamateWorkerTokenAuthenticationFilter")
class VitamateWorkerTokenAuthenticationFilterTest {

    private static final String HEADER_NAME = "X-Vitamate-Worker-Token";
    private static final String WORKER_TOKEN = "local-test-worker-token";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("worker token 인증")
    class AuthenticateWorkerToken {

        @Test
        @DisplayName("토큰이 없으면 401 응답을 반환하고 다음 필터로 넘기지 않는다")
        void rejectsMissingToken() throws Exception {
            MockHttpServletRequest request = requestWithoutToken();
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            filter().doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString())
                    .contains("VITAMATE_WORKER_UNAUTHORIZED");
            assertThat(filterChain.getRequest()).isNull();
        }

        @Test
        @DisplayName("토큰이 다르면 401 응답을 반환하고 다음 필터로 넘기지 않는다")
        void rejectsWrongToken() throws Exception {
            MockHttpServletRequest request = requestWithoutToken();
            request.addHeader(HEADER_NAME, "wrong-token");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            filter().doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentAsString())
                    .contains("VITAMATE_WORKER_UNAUTHORIZED");
            assertThat(filterChain.getRequest()).isNull();
        }

        @Test
        @DisplayName("토큰이 일치하면 worker 권한으로 다음 필터에 넘긴다")
        void acceptsMatchingToken() throws Exception {
            MockHttpServletRequest request = requestWithoutToken();
            request.addHeader(HEADER_NAME, WORKER_TOKEN);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            filter().doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(filterChain.getRequest()).isSameAs(request);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    // 테스트용 내부 API 요청을 만든다.
    private MockHttpServletRequest requestWithoutToken() {
        return new MockHttpServletRequest(
                "GET",
                "/internal/v1/vitamate/analyses/1/jobs/attempt-1"
        );
    }

    // 테스트용 worker token 필터를 만든다.
    private VitamateWorkerTokenAuthenticationFilter filter() {
        return new VitamateWorkerTokenAuthenticationFilter(
                new VitamateWorkerAuthProperties(WORKER_TOKEN)
        );
    }
}
