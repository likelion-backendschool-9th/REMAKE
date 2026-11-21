package com.remake.auth.adapter.out.oauth;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.remake.auth.application.port.out.OAuthClientPort;
import com.remake.auth.domain.SocialProfile;
import com.remake.global.config.OAuthProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.user.domain.AuthProvider;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/**
 * 소셜 인가 코드 → 액세스 토큰 → 프로필.
 * remake.oauth.mock-enabled=true 이면 실제 HTTP 없이 code를 provider_user_id로 쓴다(로컬·키 없이 개발).
 * 통합 테스트는 이 어댑터 클래스를 @MockitoBean으로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class SocialOAuthAdapter implements OAuthClientPort {

	private final OAuthProperties oAuthProperties;
	private final RestClient.Builder restClientBuilder;

	@Override
	public SocialProfile exchange(AuthProvider provider, String code, String redirectUri, String state) {
		if (oAuthProperties.mockEnabled()) {
			if (!StringUtils.hasText(code)) {
				throw new BusinessException(ErrorCode.OAUTH_FAILED);
			}
			return new SocialProfile(provider, code, null);
		}
		return switch (provider) {
			case KAKAO -> exchangeKakao(code, redirectUri);
			case NAVER -> exchangeNaver(code, redirectUri, state);
			case GOOGLE -> exchangeGoogle(code, redirectUri);
		};
	}

	private SocialProfile exchangeKakao(String code, String redirectUri) {
		OAuthProperties.Provider cfg = requireConfigured(oAuthProperties.kakao(), "kakao");
		String accessToken = fetchToken(cfg, code, redirectUri, Map.of());
		JsonNode body = getJson(cfg.userInfoUri(), accessToken);
		String id = text(body, "id");
		if (!StringUtils.hasText(id)) {
			throw new BusinessException(ErrorCode.OAUTH_FAILED);
		}
		String email = text(body != null ? body.path("kakao_account") : null, "email");
		return new SocialProfile(AuthProvider.KAKAO, id, blankToNull(email));
	}

	private SocialProfile exchangeNaver(String code, String redirectUri, String state) {
		OAuthProperties.Provider cfg = requireConfigured(oAuthProperties.naver(), "naver");
		if (!StringUtils.hasText(state)) {
			throw new BusinessException(ErrorCode.OAUTH_FAILED);
		}
		Map<String, String> extra = new LinkedHashMap<>();
		extra.put("state", state);
		String accessToken = fetchToken(cfg, code, redirectUri, extra);
		JsonNode body = getJson(cfg.userInfoUri(), accessToken);
		JsonNode response = body != null ? body.path("response") : null;
		String id = text(response, "id");
		if (!StringUtils.hasText(id)) {
			throw new BusinessException(ErrorCode.OAUTH_FAILED);
		}
		return new SocialProfile(AuthProvider.NAVER, id, blankToNull(text(response, "email")));
	}

	private SocialProfile exchangeGoogle(String code, String redirectUri) {
		OAuthProperties.Provider cfg = requireConfigured(oAuthProperties.google(), "google");
		String accessToken = fetchToken(cfg, code, redirectUri, Map.of());
		JsonNode body = getJson(cfg.userInfoUri(), accessToken);
		String id = text(body, "sub");
		if (!StringUtils.hasText(id)) {
			id = text(body, "id");
		}
		if (!StringUtils.hasText(id)) {
			throw new BusinessException(ErrorCode.OAUTH_FAILED);
		}
		return new SocialProfile(AuthProvider.GOOGLE, id, blankToNull(text(body, "email")));
	}

	private JsonNode getJson(String uri, String accessToken) {
		try {
			return restClientBuilder.build().get()
					.uri(uri)
					.header("Authorization", "Bearer " + accessToken)
					.retrieve()
					.body(JsonNode.class);
		} catch (RestClientException ex) {
			throw new BusinessException(ErrorCode.OAUTH_FAILED);
		}
	}

	private String fetchToken(OAuthProperties.Provider cfg, String code, String redirectUri,
			Map<String, String> extra) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "authorization_code");
		form.add("client_id", cfg.clientId());
		form.add("client_secret", cfg.clientSecret() != null ? cfg.clientSecret() : "");
		form.add("code", code);
		form.add("redirect_uri", redirectUri);
		extra.forEach(form::add);
		try {
			JsonNode body = restClientBuilder.build().post()
					.uri(cfg.tokenUri())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(JsonNode.class);
			String accessToken = text(body, "access_token");
			if (!StringUtils.hasText(accessToken)) {
				throw new BusinessException(ErrorCode.OAUTH_FAILED);
			}
			return accessToken;
		} catch (RestClientException ex) {
			throw new BusinessException(ErrorCode.OAUTH_FAILED);
		}
	}

	private OAuthProperties.Provider requireConfigured(OAuthProperties.Provider cfg, String name) {
		if (cfg == null || !StringUtils.hasText(cfg.clientId()) || !StringUtils.hasText(cfg.tokenUri())
				|| !StringUtils.hasText(cfg.userInfoUri())) {
			throw new BusinessException(ErrorCode.OAUTH_FAILED);
		}
		return cfg;
	}

	private static String text(JsonNode node, String field) {
		if (node == null || node.isMissingNode() || node.isNull()) {
			return null;
		}
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asString();
	}

	private static String blankToNull(String value) {
		return StringUtils.hasText(value) ? value : null;
	}
}
