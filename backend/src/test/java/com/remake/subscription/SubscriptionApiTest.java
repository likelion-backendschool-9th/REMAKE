package com.remake.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.quota.application.port.in.ConsumeMealPlanQuotaUseCase;
import com.remake.quota.application.port.in.ConsumeRecipeQuotaUseCase;
import com.remake.support.IntegrationTestSupport;
import com.remake.support.MutableClock;
import com.remake.subscription.application.port.in.EndSubscriptionForWithdrawalUseCase;
import com.remake.subscription.application.port.out.TossBillingPort;
import com.remake.subscription.domain.Subscription;
import com.remake.user.domain.AuthProvider;

import tools.jackson.databind.json.JsonMapper;

class SubscriptionApiTest extends IntegrationTestSupport {

	@Autowired
	private EndSubscriptionForWithdrawalUseCase endSubscriptionForWithdrawalUseCase;

	@Autowired
	private ConsumeRecipeQuotaUseCase consumeRecipeQuotaUseCase;

	@Autowired
	private ConsumeMealPlanQuotaUseCase consumeMealPlanQuotaUseCase;

	private MockHttpServletRequestBuilder asUser(Long userId, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId);
	}

	@Test
	void 탈퇴_유스케이스만_호출해도_구독이_종료된다() throws Exception {
		Long userId = createUser();
		startSubscription(userId, "auth-end-only", "cust-end-only");

		endSubscriptionForWithdrawalUseCase.endForWithdrawal(userId);

		assertThat(jdbcTemplate.queryForObject("SELECT status FROM subscriptions WHERE user_id = ?", String.class,
				userId)).isEqualTo("ENDED");
		assertThat(jdbcTemplate.queryForObject("SELECT plan FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("BASIC");
	}

	@Test
	void 구독이_없으면_NONE과_다음_결제일을_돌려준다() throws Exception {
		Long userId = createUser();

		mockMvc.perform(asUser(userId, get("/api/v1/user/subscription")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("NONE"))
				.andExpect(jsonPath("$.price").value(9900))
				.andExpect(jsonPath("$.nextBillingDate").value("2026-04-04"))
				.andExpect(jsonPath("$.currentPeriodEnd").doesNotExist());
	}

	@Test
	void 카드_등록_준비는_customerKey와_금액을_준다() throws Exception {
		Long userId = createUser();

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/billing-auth")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.customerKey").isNotEmpty())
				.andExpect(jsonPath("$.orderName").value(Subscription.ORDER_NAME))
				.andExpect(jsonPath("$.amount").value(9900));
	}

	@Test
	void 첫_결제가_성공하면_ACTIVE와_PREMIUM이_된다() throws Exception {
		Long userId = createUser();
		when(tossPaymentsAdapter.issueBillingKey(eq("auth-ok"), anyString()))
				.thenReturn(new TossBillingPort.IssuedBillingKey("billing-plain"));
		when(tossPaymentsAdapter.charge(eq("billing-plain"), anyString(), anyString(), anyString(), eq(9900L)))
				.thenReturn(new TossBillingPort.ChargedPayment("pay-1", 9900L));

		String billingAuthBody = mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/billing-auth")))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String key = JsonMapper.builder().build().readTree(billingAuthBody).get("customerKey").asString();

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "authKey": "auth-ok", "customerKey": "%s" }
								""".formatted(key)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.price").value(9900))
				.andExpect(jsonPath("$.nextBillingDate").value("2026-04-04"));

		assertThat(jdbcTemplate.queryForObject("SELECT plan FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("PREMIUM");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM subscriptions WHERE user_id = ? AND status = 'ACTIVE'", Integer.class, userId))
						.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM subscription_payments WHERE user_id = ? AND status = 'DONE'", Integer.class,
				userId)).isEqualTo(1);
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'subscription_started' AND user_id = ?", Integer.class,
				userId)).isEqualTo(1);
	}

	@Test
	void 이미_구독_중이면_ALREADY_SUBSCRIBED다() throws Exception {
		Long userId = createUser();
		startSubscription(userId, "auth-dup", "cust-dup");

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/billing-auth")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_SUBSCRIBED"));
	}

	@Test
	void 결제가_거절되면_구독이_생기지_않는다() throws Exception {
		Long userId = createUser();
		when(tossPaymentsAdapter.issueBillingKey(anyString(), anyString()))
				.thenReturn(new TossBillingPort.IssuedBillingKey("billing-plain"));
		when(tossPaymentsAdapter.charge(anyString(), anyString(), anyString(), anyString(), anyLong()))
				.thenThrow(new BusinessException(ErrorCode.PAYMENT_FAILED, ErrorCode.PAYMENT_FAILED.getMessage(),
						Map.of("reason", "한도 초과")));

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "authKey": "auth-fail", "customerKey": "cust-fail" }
								"""))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value("PAYMENT_FAILED"));

		assertThat(jdbcTemplate.queryForObject("SELECT plan FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("BASIC");
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscriptions WHERE user_id = ?", Integer.class,
				userId)).isEqualTo(0);
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'subscription_payment_failed' AND user_id = ?",
				Integer.class, userId)).isEqualTo(1);
	}

	@Test
	void 해지_예약은_idempotent하고_혜택을_유지한다() throws Exception {
		Long userId = createUser();
		startSubscription(userId, "auth-cancel", "cust-cancel");

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/cancel")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELED"))
				.andExpect(jsonPath("$.currentPeriodEnd").value("2026-04-03"));

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/cancel")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELED"));

		assertThat(jdbcTemplate.queryForObject("SELECT plan FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("PREMIUM");
	}

	@Test
	void 탈퇴하면_구독이_ENDED가_되고_결제_기록은_파기된다() throws Exception {
		// AUTH-56 / #350: 탈퇴 시 구독 ENDED + subscription_payments 파기(#339 문서).
		Long userId = createUser(AuthProvider.KAKAO, "kakao-sub-del");
		jdbcTemplate.update("UPDATE users SET email = ? WHERE id = ?", "sub-del@example.com", userId);
		startSubscription(userId, "auth-del", "cust-del");
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM subscriptions WHERE user_id = ?", String.class, userId))
				.isEqualTo("ACTIVE");
		assertThat(jdbcTemplate.queryForObject("SELECT plan FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("PREMIUM");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM subscription_payments WHERE user_id = ?", Integer.class, userId)).isEqualTo(1);

		AtomicReference<String> code = new AtomicReference<>();
		doAnswer(invocation -> {
			code.set(invocation.getArgument(1));
			return null;
		}).when(deletionCodeMailAdapter).send(anyString(), anyString());

		mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion-code")))
				.andExpect(status().isCreated());

		mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "method": "EMAIL_CODE", "code": "%s" }
								""".formatted(code.get())))
				.andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForObject("SELECT status FROM subscriptions WHERE user_id = ?", String.class,
				userId)).isEqualTo("ENDED");
		assertThat(jdbcTemplate.queryForObject("SELECT billing_key FROM subscriptions WHERE user_id = ?", String.class,
				userId)).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT plan FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("BASIC");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM subscription_payments WHERE user_id = ?", Integer.class, userId)).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'subscription_ended' AND user_id = ?", Integer.class,
				userId)).isEqualTo(1);
	}

	@Test
	void 혜택_미사용_7일_안이면_전액_환불된다() throws Exception {
		Long userId = createUser();
		startSubscription(userId, "auth-refund-ok", "cust-refund-ok");
		// BASIC 일일 한도(2)까지는 써도 환불 가능
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/refund")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("NONE"));

		verify(tossPaymentsAdapter).cancelPayment(eq("pay-cust-refund-ok"), anyString(), eq(9900L));
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM subscriptions WHERE user_id = ?", String.class,
				userId)).isEqualTo("ENDED");
		assertThat(jdbcTemplate.queryForObject("SELECT billing_key FROM subscriptions WHERE user_id = ?", String.class,
				userId)).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT plan FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("BASIC");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM subscription_payments WHERE user_id = ?", Integer.class, userId)).isEqualTo(1);
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'subscription_refunded' AND user_id = ?", Integer.class,
				userId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'subscription_ended' AND user_id = ?", Integer.class,
				userId)).isZero();
		String refundProps = jdbcTemplate.queryForObject(
				"SELECT properties::text FROM events WHERE event_name = 'subscription_refunded' AND user_id = ?",
				String.class, userId);
		assertThat(refundProps).contains("\"payment_amount\": 9900").contains("\"refund_amount\": 9900");
	}

	@Test
	void 결제_7일이_지나면_환불할_수_없다() throws Exception {
		Long userId = createUser();
		startSubscription(userId, "auth-refund-late", "cust-refund-late");
		clock.setNow(ZonedDateTime.of(2026, 3, 11, 10, 0, 0, 0, MutableClock.ZONE));

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/refund")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("REFUND_NOT_ELIGIBLE"))
				.andExpect(jsonPath("$.details.reason").value("WINDOW_EXPIRED"));

		verify(tossPaymentsAdapter, never()).cancelPayment(anyString(), anyString(), anyLong());
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM subscriptions WHERE user_id = ?", String.class,
				userId)).isEqualTo("ACTIVE");
		assertThat(jdbcTemplate.queryForObject("SELECT plan FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("PREMIUM");
	}

	@Test
	void 식단표를_만들었으면_환불할_수_없다() throws Exception {
		Long userId = createUser();
		startSubscription(userId, "auth-refund-meal", "cust-refund-meal");
		consumeMealPlanQuotaUseCase.consumeMealPlanQuota(userId);

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/refund")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("REFUND_NOT_ELIGIBLE"))
				.andExpect(jsonPath("$.details.reason").value("BENEFIT_USED"));

		verify(tossPaymentsAdapter, never()).cancelPayment(anyString(), anyString(), anyLong());
	}

	@Test
	void BASIC_일일_한도를_넘는_레시피를_만들면_환불할_수_없다() throws Exception {
		Long userId = createUser();
		startSubscription(userId, "auth-refund-recipe", "cust-refund-recipe");
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);
		consumeRecipeQuotaUseCase.consumeRecipeQuota(userId);

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/refund")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("REFUND_NOT_ELIGIBLE"))
				.andExpect(jsonPath("$.details.reason").value("BENEFIT_USED"));

		verify(tossPaymentsAdapter, never()).cancelPayment(anyString(), anyString(), anyLong());
	}

	@Test
	void 토스_취소가_실패하면_구독은_유지된다() throws Exception {
		Long userId = createUser();
		startSubscription(userId, "auth-refund-toss", "cust-refund-toss");
		doThrow(new BusinessException(ErrorCode.PAYMENT_UPSTREAM_ERROR)).when(tossPaymentsAdapter)
				.cancelPayment(anyString(), anyString(), anyLong());

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/refund")))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("PAYMENT_UPSTREAM_ERROR"));

		assertThat(jdbcTemplate.queryForObject("SELECT status FROM subscriptions WHERE user_id = ?", String.class,
				userId)).isEqualTo("ACTIVE");
		assertThat(jdbcTemplate.queryForObject("SELECT plan FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("PREMIUM");
	}

	@Test
	void 구독이_없으면_환불도_SUBSCRIPTION_NOT_FOUND다() throws Exception {
		Long userId = createUser();

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription/refund")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));
	}

	private void startSubscription(Long userId, String authKey, String customerKey) throws Exception {
		when(tossPaymentsAdapter.issueBillingKey(eq(authKey), eq(customerKey)))
				.thenReturn(new TossBillingPort.IssuedBillingKey("billing-" + customerKey));
		when(tossPaymentsAdapter.charge(eq("billing-" + customerKey), eq(customerKey), anyString(), anyString(),
				eq(9900L))).thenReturn(new TossBillingPort.ChargedPayment("pay-" + customerKey, 9900L));

		mockMvc.perform(asUser(userId, post("/api/v1/user/subscription"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "authKey": "%s", "customerKey": "%s" }
								""".formatted(authKey, customerKey)))
				.andExpect(status().isCreated());
	}
}
