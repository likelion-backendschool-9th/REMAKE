package com.remake.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.auth.domain.SocialProfile;
import com.remake.global.config.UploadProperties;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.user.application.port.in.DeleteAccountUseCase;
import com.remake.user.application.port.in.PurgeExpiredDeletionCodesUseCase;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.DeletionMethod;

class AccountDeletionApiTest extends IntegrationTestSupport {

	@Autowired
	private PurgeExpiredDeletionCodesUseCase purgeExpiredDeletionCodesUseCase;

	@Autowired
	private DeleteAccountUseCase deleteAccountUseCase;

	@Autowired
	private UploadProperties uploadProperties;

	private MockHttpServletRequestBuilder asUser(Long userId, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId);
	}

	private Path receiptImagePath(String imageKey) {
		return Path.of(uploadProperties.rootDirectory(), "receipts", imageKey);
	}

	private Long createUserWithEmail(AuthProvider provider, String providerUserId, String email) {
		Long userId = createUser(provider, providerUserId);
		jdbcTemplate.update("UPDATE users SET email = ? WHERE id = ?", email, userId);
		return userId;
	}

	private String captureDeletionCode(Long userId) throws Exception {
		AtomicReference<String> code = new AtomicReference<>();
		doAnswer(invocation -> {
			code.set(invocation.getArgument(1));
			return null;
		}).when(deletionCodeMailAdapter).send(anyString(), anyString());

		mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion-code")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.expiresAt").exists());
		assertThat(code.get()).isNotBlank().hasSize(6);
		return code.get();
	}

	@Test
	void 카카오_이메일이_없으면_ACCOUNT_EMAIL_MISSING이다() throws Exception {
		Long userId = createUser(AuthProvider.KAKAO, "no-email");

		mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion-code")))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.code").value("ACCOUNT_EMAIL_MISSING"));
	}

	@Test
	void 이메일_인증번호로_탈퇴하면_개인데이터를_지우고_레시피는_남긴다() throws Exception {
		Long userId = createUserWithEmail(AuthProvider.KAKAO, "kakao-del", "del@example.com");
		createIngredient(userId, "VEGETABLE", "양파", java.time.LocalDate.of(2026, 3, 10));
		Long recipeId = createRecipe(userId);
		jdbcTemplate.update("""
				INSERT INTO refresh_tokens (user_id, token_hash, expires_at, created_at)
				VALUES (?, 'hash-del-1', ?, ?)
				""", userId, LocalDateTime.now(clock).plusDays(1), LocalDateTime.now(clock));

		String imageKey = UUID.randomUUID().toString();
		Path imageFile = receiptImagePath(imageKey);
		Files.createDirectories(imageFile.getParent());
		Files.writeString(imageFile, "receipt-bytes");
		LocalDateTime now = LocalDateTime.now(clock);
		jdbcTemplate.update("""
				INSERT INTO receipts (user_id, purchased_on, store_name, image_key, created_at)
				VALUES (?, ?, '동네마트', ?, ?)
				""", userId, now.toLocalDate(), imageKey, now);
		jdbcTemplate.update("""
				INSERT INTO receipt_first_applications
				(user_id, store_name, purchased_on, receipt_number, created_at)
				VALUES (?, '동네마트', ?, 'DEL-1', ?)
				""", userId, now.toLocalDate(), now);

		String code = captureDeletionCode(userId);

		mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "method": "EMAIL_CODE", "code": "%s" }
								""".formatted(code)))
				.andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForObject("SELECT provider_user_id FROM users WHERE id = ?", String.class, userId))
				.isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = ?", String.class, userId)).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM users WHERE id = ?", LocalDateTime.class, userId))
				.isNotNull();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ingredients WHERE user_id = ?", Integer.class,
				userId)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM receipts WHERE user_id = ?", Integer.class, userId))
				.isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM receipt_first_applications WHERE user_id = ?", Integer.class, userId)).isZero();
		Awaitility.await().atMost(Duration.ofSeconds(3)).until(() -> !Files.exists(imageFile));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM refresh_tokens WHERE user_id = ?", Integer.class,
				userId)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_deletion_requests WHERE user_id = ?",
				Integer.class, userId)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipes WHERE id = ?", Integer.class, recipeId))
				.isEqualTo(1);

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'account_deletion_requested' AND user_id = ?",
				Integer.class, userId)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT properties->>'deletion_method' FROM events WHERE event_name = 'account_deleted' AND user_id = ?",
				String.class, userId)).isEqualTo("email_code");
	}

	@Test
	void 틀린_인증번호는_남은_횟수를_알려주고_다섯_번이면_잠긴다() throws Exception {
		Long userId = createUserWithEmail(AuthProvider.GOOGLE, "google-lock", "lock@example.com");
		captureDeletionCode(userId);

		for (int i = 1; i <= 4; i++) {
			mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion"))
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{ "method": "EMAIL_CODE", "code": "000000" }
									"""))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("DELETION_CODE_INVALID"))
					.andExpect(jsonPath("$.details.remainingAttempts").value(5 - i));
		}

		mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "method": "EMAIL_CODE", "code": "000000" }
								"""))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("DELETION_CODE_LOCKED"));

		assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM users WHERE id = ?", Object.class, userId))
				.isNull();
	}

	@Test
	void 틀린_인증번호를_동시에_보내도_다섯_번에_잠긴다() throws Exception {
		Long userId = createUserWithEmail(AuthProvider.KAKAO, "kakao-race", "race@example.com");
		captureDeletionCode(userId);

		// REQUIRES_NEW가 바깥 TX와 커넥션을 동시에 쓰므로, 테스트 풀(기본 10)을 넘기지 않게 3개만 보낸다.
		int threads = 3;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch ready = new CountDownLatch(threads);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger invalid = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();
		var command = new DeleteAccountUseCase.Command(userId, DeletionMethod.EMAIL_CODE, "000000", null, null, null);
		for (int i = 0; i < threads; i++) {
			futures.add(pool.submit(() -> {
				try {
					ready.countDown();
					start.await();
					deleteAccountUseCase.delete(command);
				} catch (BusinessException e) {
					if (e.getErrorCode() != ErrorCode.DELETION_CODE_INVALID) {
						throw e;
					}
					invalid.incrementAndGet();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}));
		}
		ready.await();
		start.countDown();
		for (Future<?> future : futures) {
			future.get();
		}
		pool.shutdownNow();

		// 덮어쓰기면 1에 머물고, 원자적 +1이면 동시 3번이 모두 반영돼 3이 된다.
		assertThat(jdbcTemplate.queryForObject(
				"SELECT attempts FROM account_deletion_requests WHERE user_id = ? ORDER BY id DESC LIMIT 1",
				Integer.class, userId)).isEqualTo(threads);
		assertThat(invalid.get()).isEqualTo(threads);
		assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM users WHERE id = ?", Object.class, userId))
				.isNull();
	}

	@Test
	void 만료된_인증번호는_DELETION_CODE_EXPIRED이다() throws Exception {
		Long userId = createUserWithEmail(AuthProvider.KAKAO, "kakao-exp", "exp@example.com");
		captureDeletionCode(userId);
		clock.advance(Duration.ofMinutes(6));

		mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "method": "EMAIL_CODE", "code": "123456" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("DELETION_CODE_EXPIRED"));
	}

	@Test
	void 네이버_다른_계정이면_REAUTH_ACCOUNT_MISMATCH이고_삭제하지_않는다() throws Exception {
		Long userId = createUser(AuthProvider.NAVER, "naver-real");
		when(socialOAuthAdapter.exchange(eq(AuthProvider.NAVER), anyString(), anyString(), any()))
				.thenReturn(new SocialProfile(AuthProvider.NAVER, "other-naver", null));

		mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "method": "REAUTH",
								  "oauthCode": "code",
								  "redirectUri": "https://app.example/callback",
								  "state": "state-1"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("REAUTH_ACCOUNT_MISMATCH"));

		assertThat(jdbcTemplate.queryForObject("SELECT provider_user_id FROM users WHERE id = ?", String.class, userId))
				.isEqualTo("naver-real");
		assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM users WHERE id = ?", Object.class, userId))
				.isNull();
	}

	@Test
	void 네이버_같은_계정이면_탈퇴한다() throws Exception {
		Long userId = createUser(AuthProvider.NAVER, "naver-ok");
		when(socialOAuthAdapter.exchange(eq(AuthProvider.NAVER), anyString(), anyString(), any()))
				.thenReturn(new SocialProfile(AuthProvider.NAVER, "naver-ok", null));

		mockMvc.perform(asUser(userId, post("/api/v1/user/account/deletion"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "method": "REAUTH",
								  "oauthCode": "code",
								  "redirectUri": "https://app.example/callback",
								  "state": "state-1"
								}
								"""))
				.andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM users WHERE id = ?", LocalDateTime.class, userId))
				.isNotNull();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT properties->>'deletion_method' FROM events WHERE event_name = 'account_deleted' AND user_id = ?",
				String.class, userId)).isEqualTo("reauth");
	}

	@Test
	void 하루_지난_인증번호_요청을_정리한다() throws Exception {
		Long userId = createUserWithEmail(AuthProvider.KAKAO, "kakao-purge", "purge@example.com");
		captureDeletionCode(userId);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_deletion_requests", Integer.class))
				.isEqualTo(1);

		clock.advance(Duration.ofDays(1).plusMinutes(1));
		assertThat(purgeExpiredDeletionCodesUseCase.purgeExpired()).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM account_deletion_requests", Integer.class))
				.isZero();
	}
}
