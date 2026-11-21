package com.remake.auth.adapter.out.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.remake.auth.domain.SocialProfile;
import com.remake.global.config.OAuthProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.user.domain.AuthProvider;

/**
 * 제공자별 토큰·프로필 JSON 매핑. Spring 없이 RestClient + MockRestServiceServer만 쓴다.
 */
class SocialOAuthAdapterTest {

	private static final String TOKEN_URI = "http://localhost:1/oauth/token";
	private static final String USER_URI = "http://localhost:1/user";

	private MockRestServiceServer server;
	private SocialOAuthAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		OAuthProperties.Provider provider = new OAuthProperties.Provider(
				"client-id", "client-secret", TOKEN_URI, USER_URI);
		OAuthProperties properties = new OAuthProperties(false, provider, provider, provider);
		adapter = new SocialOAuthAdapter(properties, builder);
	}

	@Test
	void 카카오_프로필은_id와_kakao_account_email을_읽는다() {
		expectTokenThenUser("""
				{ "id": 12345, "kakao_account": { "email": "k@example.com" } }
				""");

		SocialProfile profile = adapter.exchange(AuthProvider.KAKAO, "code", "http://localhost/cb", null);

		assertThat(profile.provider()).isEqualTo(AuthProvider.KAKAO);
		assertThat(profile.providerUserId()).isEqualTo("12345");
		assertThat(profile.email()).isEqualTo("k@example.com");
		server.verify();
	}

	@Test
	void 네이버는_state가_없으면_OAUTH_FAILED다() {
		assertThatThrownBy(() -> adapter.exchange(AuthProvider.NAVER, "code", "http://localhost/cb", null))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException) ex).getErrorCode())
				.isEqualTo(ErrorCode.OAUTH_FAILED);
	}

	@Test
	void 네이버_프로필은_response_id와_email을_읽고_state를_토큰_요청에_넣는다() {
		server.expect(requestTo(TOKEN_URI))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess("""
						{ "access_token": "naver-token" }
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo(USER_URI))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer naver-token"))
				.andRespond(withSuccess("""
						{ "response": { "id": "n-99", "email": "n@example.com" } }
						""", MediaType.APPLICATION_JSON));

		SocialProfile profile = adapter.exchange(AuthProvider.NAVER, "code", "http://localhost/cb", "state-1");

		assertThat(profile.providerUserId()).isEqualTo("n-99");
		assertThat(profile.email()).isEqualTo("n@example.com");
		server.verify();
	}

	@Test
	void 구글_프로필은_sub를_우선한다() {
		expectTokenThenUser("""
				{ "sub": "g-sub", "id": "g-id", "email": "g@example.com" }
				""");

		SocialProfile profile = adapter.exchange(AuthProvider.GOOGLE, "code", "http://localhost/cb", null);

		assertThat(profile.providerUserId()).isEqualTo("g-sub");
		assertThat(profile.email()).isEqualTo("g@example.com");
		server.verify();
	}

	private void expectTokenThenUser(String userJson) {
		server.expect(requestTo(TOKEN_URI))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess("""
						{ "access_token": "access" }
						""", MediaType.APPLICATION_JSON));
		server.expect(requestTo(USER_URI))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Bearer access"))
				.andRespond(withSuccess(userJson, MediaType.APPLICATION_JSON));
	}
}
