package com.remake.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI 문서 설정. 문서는 /v3/api-docs, 화면은 /swagger-ui.html.
 * 프론트는 로컬 백엔드의 /v3/api-docs로 API 클라이언트를 만든다(frontend의 pnpm gen:api).
 * 운영 프록시는 /api/만 백엔드로 보내므로 이 주소들은 운영 도메인에서 열리지 않는다.
 */
@Configuration
public class OpenApiConfig {

	/** 로그인이 필요한 API에 붙일 쿠키 인증 스킴 이름(#42). */
	public static final String COOKIE_AUTH = "cookieAuth";

	@Bean
	public OpenAPI remakeOpenApi(AuthProperties authProperties) {
		return new OpenAPI()
				.info(new Info().title("Remake API").version("v1"))
				.components(new Components().addSecuritySchemes(COOKIE_AUTH,
						new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.COOKIE)
								.name(authProperties.accessCookieName())));
	}
}
