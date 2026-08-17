package com.group3.vitamins.global.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger(springdoc-openapi) 문서 메타 정보.
 *
 * <p>Swagger 는 <b>명세의 원본이 아니다.</b> 명세 원본은 노션이며,
 * 이 문서는 구현 결과를 확인하고 프론트가 직접 호출해보는 용도다.
 * 자세한 규칙은 {@code .ai/API.md} 참고.
 *
 * <p><b>TODO — 인증 스킴</b>: 현재 프로젝트는 {@code spring-session}(Redis/JDBC) 기반이므로
 * 세션 쿠키 인증으로 보인다. 인증 방식이 확정되면 아래 중 하나를 추가한다.
 * <ul>
 *   <li>세션 쿠키 방식 → {@code SecurityScheme.In.COOKIE} 로 {@code SESSION} 쿠키 정의</li>
 *   <li>JWT 방식으로 변경 시 → {@code bearerAuth} (HTTP bearer, JWT) 정의</li>
 * </ul>
 * 확정 전까지는 스킴을 정의하지 않는다. 잘못된 스킴은 프론트를 오도한다.
 *
 * <p><b>TODO — Spring Security 설정</b>: SecurityConfig 작성 시 아래 경로를
 * 인증 없이 접근 가능하도록 허용해야 Swagger UI 가 열린다.
 * <pre>
 *   /swagger-ui.html, /swagger-ui/**, /v3/api-docs/**
 * </pre>
 * 단 운영 프로필에서는 springdoc 자체가 비활성화되므로 허용해도 노출되지 않는다.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("VitaminS API")
                        .description("""
                                그룹웨어 서비스 백엔드 API 문서.

                                ⚠️ 이 문서는 구현 결과를 보여줄 뿐, 명세의 원본이 아닙니다.
                                명세 원본은 레포의 .ai/api/{도메인}.md 입니다. 두 문서가 다르면 구현이 잘못된 것입니다.
                                """)
                        .version("v1"));
    }
}
