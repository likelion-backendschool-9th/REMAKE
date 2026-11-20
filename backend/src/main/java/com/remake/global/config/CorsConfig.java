package com.remake.global.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.remake.global.common.TraceIdFilter;

/**
 * 로컬 개발용 CORS. 로컬에서는 프론트(http://localhost:3000)가 백엔드(http://localhost:8080)를 직접 부르므로 허용한다.
 * 허용할 주소는 remake.cors.allowed-origins로 정하고, 값이 없으면(운영) 아무 주소도 허용하지 않는다.
 * 운영은 프록시 덕분에 프론트와 백엔드가 같은 도메인이라 CORS가 필요 없다.
 * Spring Security를 추가할 때는 Security 설정에서도 CORS를 켜야(http.cors) 이 설정이 Security보다 먼저 적용된다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final List<String> allowedOrigins;

	public CorsConfig(@Value("${remake.cors.allowed-origins:}") List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins.stream().filter(origin -> !origin.isBlank()).toList();
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (allowedOrigins.isEmpty()) {
			return;
		}
		registry.addMapping("/api/**")
				.allowedOrigins(allowedOrigins.toArray(String[]::new))
				.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.exposedHeaders(TraceIdFilter.HEADER)
				// 리프레시 토큰이 HttpOnly 쿠키라 쿠키를 같이 보내야 한다.
				.allowCredentials(true);
	}
}
