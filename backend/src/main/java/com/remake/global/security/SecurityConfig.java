package com.remake.global.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.remake.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final AuthCookieFilter authCookieFilter;
	private final SecurityErrorWriter securityErrorWriter;
	private final ObjectProvider<OptionalAuthPreFilter> optionalAuthPreFilter;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(Customizer.withDefaults())
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
						.requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
						.permitAll()
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers("/api/v1/public/**").permitAll()
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
						.requestMatchers("/api/v1/user/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint((request, response, authException) -> {
							ErrorCode code = Boolean.TRUE.equals(request.getAttribute(AuthCookieFilter.TOKEN_EXPIRED_ATTR))
									? ErrorCode.TOKEN_EXPIRED
									: ErrorCode.UNAUTHORIZED;
							securityErrorWriter.write(response, code);
						})
						.accessDeniedHandler((request, response, accessDeniedException) ->
								securityErrorWriter.write(response, ErrorCode.FORBIDDEN)))
				.addFilterBefore(authCookieFilter, UsernamePasswordAuthenticationFilter.class);
		optionalAuthPreFilter.ifAvailable(filter ->
				http.addFilterBefore(filter, AuthCookieFilter.class));
		return http.build();
	}
}
