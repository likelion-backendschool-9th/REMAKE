package com.remake.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import com.remake.auth.domain.SocialProfile;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;
import com.remake.user.domain.AuthProvider;

import jakarta.servlet.http.Cookie;

class AuthApiTest extends IntegrationTestSupport {

	@Test
	void 소셜_로그인_성공_시_쿠키를_심고_온보딩_여부를_돌려준다() throws Exception {
		when(socialOAuthAdapter.exchange(eq(AuthProvider.KAKAO), eq("auth-code"),
				eq("http://localhost:3000/callback"), isNull()))
				.thenReturn(new SocialProfile(AuthProvider.KAKAO, "kakao-1", "a@example.com"));

		MockHttpServletResponse response = mockMvc.perform(post("/api/v1/public/auth/oauth/kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "auth-code", "redirectUri": "http://localhost:3000/callback" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onboardingCompleted").value(false))
				.andExpect(cookie().exists("ACCESS_TOKEN"))
				.andExpect(cookie().httpOnly("ACCESS_TOKEN", true))
				.andExpect(cookie().exists("REFRESH_TOKEN"))
				.andReturn()
				.getResponse();

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refresh_tokens", Integer.class)).isEqualTo(1);

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'signup_completed'", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'login_succeeded'", Integer.class)).isEqualTo(1);

		String access = response.getCookie("ACCESS_TOKEN").getValue();
		mockMvc.perform(get("/api/v1/user/ingredient-categories").cookie(new Cookie("ACCESS_TOKEN", access)))
				.andExpect(status().isOk());
	}

	@Test
	void 같은_소셜_계정으로_다시_로그인하면_신규_가입_이벤트가_없다() throws Exception {
		when(socialOAuthAdapter.exchange(any(), any(), any(), any()))
				.thenReturn(new SocialProfile(AuthProvider.NAVER, "naver-1", null));

		String body = """
				{ "code": "c1", "redirectUri": "http://localhost:3000/cb", "state": "s1" }
				""";
		mockMvc.perform(post("/api/v1/public/auth/oauth/naver").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk());
		awaitEventWrites();

		mockMvc.perform(post("/api/v1/public/auth/oauth/naver").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.onboardingCompleted").value(false));
		awaitEventWrites();

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'signup_completed'", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'login_succeeded'", Integer.class)).isEqualTo(2);
	}

	@Test
	void 지원하지_않는_제공자면_OAUTH_PROVIDER_UNSUPPORTED다() throws Exception {
		mockMvc.perform(post("/api/v1/public/auth/oauth/facebook")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "c", "redirectUri": "http://localhost:3000/cb" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("OAUTH_PROVIDER_UNSUPPORTED"));
	}

	@Test
	void 소셜_교환_실패면_OAUTH_FAILED와_login_failed_이벤트다() throws Exception {
		when(socialOAuthAdapter.exchange(any(), any(), any(), any()))
				.thenThrow(new BusinessException(ErrorCode.OAUTH_FAILED));

		mockMvc.perform(post("/api/v1/public/auth/oauth/kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "bad", "redirectUri": "http://localhost:3000/cb" }
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("OAUTH_FAILED"));

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'login_failed'", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT properties->>'error_code' FROM events WHERE event_name = 'login_failed'", String.class))
				.isEqualTo("OAUTH_FAILED");
	}

	@Test
	void 정지_계정은_ACCOUNT_SUSPENDED다() throws Exception {
		when(socialOAuthAdapter.exchange(any(), any(), any(), any()))
				.thenReturn(new SocialProfile(AuthProvider.KAKAO, "suspended-1", null));

		LocalDateTime now = LocalDateTime.now(clock);
		jdbcTemplate.update("""
				INSERT INTO users (provider, provider_user_id, email, role, status, plan, onboarding_completed,
				                   recipe_supplement_enabled, notification_enabled, created_at, updated_at)
				VALUES ('KAKAO', 'suspended-1', NULL, 'USER', 'SUSPENDED', 'BASIC', false, false, true, ?, ?)
				""", now, now);

		mockMvc.perform(post("/api/v1/public/auth/oauth/kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "c", "redirectUri": "http://localhost:3000/cb" }
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'login_failed'", Integer.class)).isEqualTo(1);
	}

	@Test
	void 리프레시로_액세스를_다시_받고_로그아웃하면_리프레시가_막힌다() throws Exception {
		when(socialOAuthAdapter.exchange(any(), any(), any(), any()))
				.thenReturn(new SocialProfile(AuthProvider.GOOGLE, "google-1", null));

		MockHttpServletResponse loginResponse = mockMvc.perform(post("/api/v1/public/auth/oauth/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "g-code", "redirectUri": "http://localhost:3000/cb" }
								"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse();

		Cookie refresh = loginResponse.getCookie("REFRESH_TOKEN");
		Cookie access = loginResponse.getCookie("ACCESS_TOKEN");
		assertThat(refresh).isNotNull();
		assertThat(access).isNotNull();

		MockHttpServletResponse refreshed = mockMvc.perform(post("/api/v1/public/auth/refresh").cookie(refresh))
				.andExpect(status().isNoContent())
				.andExpect(cookie().exists("ACCESS_TOKEN"))
				.andReturn()
				.getResponse();
		assertThat(refreshed.getCookie("ACCESS_TOKEN").getValue()).isNotBlank();

		Long userId = jdbcTemplate.queryForObject("SELECT id FROM users", Long.class);
		mockMvc.perform(post("/api/v1/user/auth/logout")
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId)
						.cookie(refresh))
				.andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM refresh_tokens WHERE revoked_at IS NOT NULL", Integer.class)).isEqualTo(1);

		mockMvc.perform(post("/api/v1/public/auth/refresh").cookie(refresh))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
	}

	@Test
	void 만료된_액세스_쿠키는_TOKEN_EXPIRED다() throws Exception {
		when(socialOAuthAdapter.exchange(any(), any(), any(), any()))
				.thenReturn(new SocialProfile(AuthProvider.KAKAO, "expire-1", null));

		MockHttpServletResponse loginResponse = mockMvc.perform(post("/api/v1/public/auth/oauth/kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "e-code", "redirectUri": "http://localhost:3000/cb" }
								"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse();

		Cookie access = loginResponse.getCookie("ACCESS_TOKEN");
		assertThat(access).isNotNull();

		clock.advance(Duration.ofMinutes(31));

		mockMvc.perform(get("/api/v1/user/ingredient-categories").cookie(access))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
	}

	@Test
	void DB에서_만료된_리프레시는_REFRESH_TOKEN_INVALID다() throws Exception {
		when(socialOAuthAdapter.exchange(any(), any(), any(), any()))
				.thenReturn(new SocialProfile(AuthProvider.GOOGLE, "expire-refresh", null));

		MockHttpServletResponse loginResponse = mockMvc.perform(post("/api/v1/public/auth/oauth/google")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "r-code", "redirectUri": "http://localhost:3000/cb" }
								"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse();

		Cookie refresh = loginResponse.getCookie("REFRESH_TOKEN");
		assertThat(refresh).isNotNull();

		jdbcTemplate.update("UPDATE refresh_tokens SET expires_at = ?", LocalDateTime.now(clock).minusDays(1));

		mockMvc.perform(post("/api/v1/public/auth/refresh").cookie(refresh))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"));
	}

	@Test
	void 정지_계정의_리프레시는_ACCOUNT_SUSPENDED다() throws Exception {
		when(socialOAuthAdapter.exchange(any(), any(), any(), any()))
				.thenReturn(new SocialProfile(AuthProvider.KAKAO, "suspend-refresh", null));

		MockHttpServletResponse loginResponse = mockMvc.perform(post("/api/v1/public/auth/oauth/kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "code": "s-code", "redirectUri": "http://localhost:3000/cb" }
								"""))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse();

		Cookie refresh = loginResponse.getCookie("REFRESH_TOKEN");
		assertThat(refresh).isNotNull();

		jdbcTemplate.update("UPDATE users SET status = 'SUSPENDED'");

		mockMvc.perform(post("/api/v1/public/auth/refresh").cookie(refresh))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));
	}

	@Test
	void 쿠키_없이_user_API는_401이다() throws Exception {
		mockMvc.perform(get("/api/v1/user/ingredient-categories"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
