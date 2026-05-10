package org.example.spartacafe.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // Swagger UI 상단에 표시되는 문서 기본 정보
        Info info = new Info()
                .title("Sparta Cafe API")
                .description("스파르타 카페 오더 시스템 API 문서")
                .version("v1.0");

        // JWT Bearer 토큰 인증 방식 등록
        // Swagger UI 우측 상단 "Authorize" 버튼으로 토큰을 입력하면
        // 이후 모든 API 요청 헤더에 Authorization: Bearer {token} 이 자동으로 붙음
        String jwtScheme = "BearerAuth";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtScheme);
        Components components = new Components()
                .addSecuritySchemes(jwtScheme, new SecurityScheme()
                        .name(jwtScheme)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(info)
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
