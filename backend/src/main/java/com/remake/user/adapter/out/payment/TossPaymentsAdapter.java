package com.remake.user.adapter.out.payment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.remake.global.config.TossPaymentsProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.user.application.port.out.TossBillingPort;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;

/**
 * 토스 빌링키 발급·자동결제.
 * remake.toss.mock-enabled=true 이면 실제 HTTP 없이 가짜 키를 돌려준다(로컬).
 * 통합 테스트는 이 어댑터를 @MockitoBean으로 바꾼다.
 */
@Component
@RequiredArgsConstructor
public class TossPaymentsAdapter implements TossBillingPort {

	private final TossPaymentsProperties properties;
	private final RestClient.Builder restClientBuilder;

	@Override
	public IssuedBillingKey issueBillingKey(String authKey, String customerKey) {
		if (properties.mockEnabled()) {
			if (!StringUtils.hasText(authKey) || !StringUtils.hasText(customerKey)) {
				throw new BusinessException(ErrorCode.BILLING_AUTH_FAILED);
			}
			return new IssuedBillingKey("mock_billing_" + customerKey);
		}
		requireSecret();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("authKey", authKey);
		body.put("customerKey", customerKey);
		try {
			JsonNode response = restClientBuilder.build().post()
					.uri(baseUrl() + "/v1/billing/authorizations/issue")
					.header("Authorization", authorizationHeader())
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(JsonNode.class);
			String billingKey = text(response, "billingKey");
			if (!StringUtils.hasText(billingKey)) {
				throw new BusinessException(ErrorCode.BILLING_AUTH_FAILED);
			}
			return new IssuedBillingKey(billingKey);
		} catch (RestClientResponseException ex) {
			throw mapIssueError(ex);
		} catch (RestClientException ex) {
			throw new BusinessException(ErrorCode.PAYMENT_UPSTREAM_ERROR);
		}
	}

	@Override
	public ChargedPayment charge(String billingKey, String customerKey, String orderId, String orderName, long amount) {
		if (properties.mockEnabled()) {
			if (!StringUtils.hasText(billingKey) || !StringUtils.hasText(customerKey)) {
				throw new BusinessException(ErrorCode.PAYMENT_FAILED);
			}
			if (billingKey.startsWith("mock_fail_")) {
				throw new BusinessException(ErrorCode.PAYMENT_FAILED, ErrorCode.PAYMENT_FAILED.getMessage(),
						Map.of("reason", "카드 한도를 초과했어요"));
			}
			return new ChargedPayment("mock_pay_" + UUID.randomUUID(), amount);
		}
		requireSecret();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("customerKey", customerKey);
		body.put("amount", amount);
		body.put("orderId", orderId);
		body.put("orderName", orderName);
		try {
			JsonNode response = restClientBuilder.build().post()
					.uri(baseUrl() + "/v1/billing/{billingKey}", billingKey)
					.header("Authorization", authorizationHeader())
					.contentType(MediaType.APPLICATION_JSON)
					.body(body)
					.retrieve()
					.body(JsonNode.class);
			String paymentKey = text(response, "paymentKey");
			if (!StringUtils.hasText(paymentKey)) {
				throw new BusinessException(ErrorCode.PAYMENT_UPSTREAM_ERROR);
			}
			return new ChargedPayment(paymentKey, amount);
		} catch (RestClientResponseException ex) {
			throw mapChargeError(ex);
		} catch (RestClientException ex) {
			throw new BusinessException(ErrorCode.PAYMENT_UPSTREAM_ERROR);
		}
	}

	private BusinessException mapIssueError(RestClientResponseException ex) {
		HttpStatusCode status = ex.getStatusCode();
		if (status.is5xxServerError()) {
			return new BusinessException(ErrorCode.PAYMENT_UPSTREAM_ERROR);
		}
		return new BusinessException(ErrorCode.BILLING_AUTH_FAILED);
	}

	private BusinessException mapChargeError(RestClientResponseException ex) {
		HttpStatusCode status = ex.getStatusCode();
		if (status.is5xxServerError()) {
			return new BusinessException(ErrorCode.PAYMENT_UPSTREAM_ERROR);
		}
		String reason = safeReason(ex.getResponseBodyAsString());
		if (StringUtils.hasText(reason)) {
			return new BusinessException(ErrorCode.PAYMENT_FAILED, ErrorCode.PAYMENT_FAILED.getMessage(),
					Map.of("reason", reason));
		}
		return new BusinessException(ErrorCode.PAYMENT_FAILED);
	}

	private void requireSecret() {
		if (!StringUtils.hasText(properties.secretKey())) {
			throw new BusinessException(ErrorCode.PAYMENT_UPSTREAM_ERROR);
		}
	}

	private String baseUrl() {
		return StringUtils.hasText(properties.baseUrl())
				? properties.baseUrl().replaceAll("/$", "")
				: "https://api.tosspayments.com";
	}

	private String authorizationHeader() {
		String token = Base64.getEncoder()
				.encodeToString((properties.secretKey() + ":").getBytes(StandardCharsets.UTF_8));
		return "Basic " + token;
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

	/** 토스 메시지 중 사용자에게 안전한 짧은 문구만 남긴다. 키·내부 코드는 넣지 않는다. */
	private static String safeReason(String body) {
		if (!StringUtils.hasText(body) || body.length() > 200) {
			return null;
		}
		if (body.contains("billingKey") || body.contains("secret") || body.contains("authKey")) {
			return null;
		}
		return body.length() > 80 ? body.substring(0, 80) : body;
	}
}
