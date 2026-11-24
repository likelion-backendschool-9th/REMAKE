package com.remake.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import com.remake.auth.application.port.out.JwtTokenPort;
import com.remake.auth.domain.SocialProfile;
import com.remake.global.config.UploadProperties;
import com.remake.global.error.BusinessException;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.application.port.in.RegisterReceiptUseCase;
import com.remake.support.IntegrationTestSupport;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.UserRole;

import jakarta.servlet.http.Cookie;

class ReceiptRegistrationApiTest extends IntegrationTestSupport {

	@Autowired
	private UploadProperties uploadProperties;

	private Path imageDirectory() {
		return Path.of(uploadProperties.rootDirectory(), "receipts");
	}

	@Autowired
	RegisterReceiptUseCase registerReceiptUseCase;

	@Autowired
	private JwtTokenPort jwtTokens;

	private Long userId;

	@BeforeEach
	void createReceiptOwner() throws Exception {
		Files.createDirectories(imageDirectory());
		userId = createUser();
	}

	@Test
	void 소셜_로그인에서_발급한_쿠키로_본인_영수증을_저장한다() throws Exception {
		String redirectUri = "http://localhost:3000/callback";
		when(socialOAuthAdapter.exchange(AuthProvider.KAKAO, "receipt-code", redirectUri, null))
				.thenReturn(new SocialProfile(AuthProvider.KAKAO, "receipt-login", null));
		Cookie accessToken = mockMvc.perform(post("/api/v1/public/auth/oauth/kakao")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"code":"receipt-code","redirectUri":"http://localhost:3000/callback"}
						""")).andExpect(status().isOk()).andReturn().getResponse().getCookie("ACCESS_TOKEN");
		assertThat(accessToken).isNotNull();
		Long loginUserId = jdbcTemplate.queryForObject(
				"SELECT id FROM users WHERE provider_user_id = 'receipt-login'", Long.class);

		mockMvc.perform(receiptRequest("양파", "우유").cookie(accessToken)).andExpect(status().isCreated());
		assertThat(jdbcTemplate.queryForList("SELECT user_id FROM receipts", Long.class))
				.containsExactly(loginUserId);
	}

	@Test
	void 영수증_품목을_모두_저장하고_냉장고에_등록한다() throws Exception {
		mockMvc.perform(request("양파", "우유")).andExpect(status().isCreated())
				.andExpect(jsonPath("$.receiptId").isNumber()).andExpect(jsonPath("$.ingredientCount").value(2));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipt_items", Long.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT store_code FROM receipts", String.class)).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT receipt_number FROM receipts", String.class))
				.isEqualTo("000123");
		assertThat(jdbcTemplate.queryForObject("SELECT purchased_time::text FROM receipts", String.class))
				.isEqualTo("14:25:00");
		assertThat(jdbcTemplate.queryForList("""
				SELECT ri.name FROM ingredients i JOIN receipt_items ri ON ri.id = i.receipt_item_id
				""", String.class)).containsExactlyInAnyOrder("양파", "우유");
		String imageKey = jdbcTemplate.queryForObject("SELECT image_key FROM receipts", String.class);
		assertThat(Files.readAllBytes(imageDirectory().resolve(imageKey))).containsExactly(1, 2, 3);

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForList("SELECT event_name FROM events", String.class))
				.containsExactlyInAnyOrder("receipt_saved", "ingredient_added", "ingredient_added");
		assertThat(jdbcTemplate.queryForObject("""
				SELECT (properties->>'food_total_amount')::bigint FROM events WHERE event_name = 'receipt_saved'
				""", Long.class)).isEqualTo(4500L);
		assertThat(jdbcTemplate.queryForObject("""
				SELECT (properties->>'registered_count')::int FROM events WHERE event_name = 'receipt_saved'
				""", Integer.class)).isEqualTo(2);
	}

	@Test
	void 팩당_중량을_환산하고_중량이_없으면_팩_수량으로_저장한다() throws Exception {
		mockMvc.perform(receiptWithItems("""
				{"name":"한돈 돼지고기","categoryCode":"MEAT","purchaseQuantity":2,"storage":"FRIDGE",
				 "volume":{"amount":0.5,"unit":"KG","scope":"PER_PACK"}},
				{"name":"삼겹살","categoryCode":"MEAT","purchaseQuantity":2,"storage":"FRIDGE",
				 "cartDeductions":[]}
				""")).andExpect(status().isCreated()).andExpect(jsonPath("$.cartRemovedCount").value(0));
		for (String table : List.of("receipt_items", "ingredients")) {
			assertThat(jdbcTemplate.queryForList(
					"SELECT name || ':' || trim_scale(amount) || ':' || unit FROM " + table, String.class))
					.containsExactlyInAnyOrder("한돈 돼지고기:1000:G", "삼겹살:2:PACK");
		}
	}

	@Test
	void 수량을_확정할_수_없거나_총차감량이_과하면_저장하지_않는다() throws Exception {
		mockMvc.perform(receiptWithItems("""
				{"name":"돼지고기","categoryCode":"MEAT","storage":"FRIDGE",
				 "volume":{"amount":500,"unit":"G","scope":"PER_PACK"}}
				""")).andExpect(status().isBadRequest());
		mockMvc.perform(receiptWithItems("""
				{"name":"양파","categoryCode":"VEGETABLE","purchaseQuantity":2,"storage":"PANTRY",
				 "cartDeductions":[{"cartItemId":1,"amount":3}]}
				""")).andExpect(status().isBadRequest());
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isZero();
	}

	@Test
	void 합계가_등록_수량_이내여도_품목에_차감_대상을_두_개_고르면_저장하지_않는다() throws Exception {
		mockMvc.perform(receiptWithItems("""
				{"name":"양파","categoryCode":"VEGETABLE","purchaseQuantity":2,"storage":"PANTRY",
				 "cartDeductions":[{"cartItemId":1,"amount":1},{"cartItemId":2,"amount":1}]}
				""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isZero();
	}

	@Test
	void 장바구니를_부분_차감하고_공백을_정리한_최종_재료명을_사용한다() throws Exception {
		Long target = cartItem(userId, "5");
		mockMvc.perform(receiptWithItems(onionDeduction(target, 2)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.cartRemovedCount").value(0));
		assertThat(cartAmount(target)).isEqualByComparingTo("3");
		assertThat(jdbcTemplate.queryForObject("SELECT name FROM ingredients", String.class)).isEqualTo("양파");
	}

	@Test
	void 여러_품목에서_선택한_같은_행은_합산해_삭제하고_이벤트에도_한_건을_기록한다() throws Exception {
		Long target = cartItem(userId, "3");
		mockMvc.perform(receiptWithItems(onionDeduction(target, 1) + "," + onionDeduction(target, 2)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.ingredientCount").value(2))
				.andExpect(jsonPath("$.cartRemovedCount").value(1));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("""
				SELECT (properties->>'cart_removed_count')::int FROM events WHERE event_name = 'receipt_saved'
				""", Integer.class)).isEqualTo(1);
	}

	@Test
	void 장바구니_잔량이_부족하면_영수증과_품목과_냉장고와_사진을_모두_롤백한다() throws Exception {
		Long target = cartItem(userId, "1");
		mockMvc.perform(receiptWithItems(onionDeduction(target, 2))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_QUANTITY"))
				.andExpect(jsonPath("$.details.remaining").value(1));
		assertThat(cartAmount(target)).isEqualByComparingTo("1");
		for (String table : List.of("receipts", "receipt_items", "ingredients", "events")) {
			assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class)).isZero();
		}
		try (var files = Files.list(imageDirectory())) {
			assertThat(files).isEmpty();
		}
	}

	@Test
	void 타인의_장바구니를_선택하면_404를_반환하고_영수증을_저장하지_않는다() throws Exception {
		Long target = cartItem(createUser(), "2");
		mockMvc.perform(receiptWithItems(onionDeduction(target, 1))).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
		assertThat(cartAmount(target)).isEqualByComparingTo("2");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isZero();
	}

	private Long cartItem(Long owner, String amount) {
		var now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO cart_items (user_id, category_id, name, amount, unit, created_at, updated_at)
				VALUES (?, ?, '양파', ?, 'EA', ?, ?) RETURNING id
				""", Long.class, owner, categoryId("VEGETABLE"), new BigDecimal(amount), now, now);
	}

	private BigDecimal cartAmount(Long id) {
		return jdbcTemplate.queryForObject("SELECT amount FROM cart_items WHERE id = ?", BigDecimal.class, id);
	}

	private String onionDeduction(Long id, int amount) {
		return """
				{"name":" 양파 ","categoryCode":"VEGETABLE","purchaseQuantity":2,"storage":"PANTRY",
				 "cartDeductions":[{"cartItemId":%d,"amount":%d}]}
				""".formatted(id, amount);
	}

	private MockMultipartHttpServletRequestBuilder receiptWithItems(String items) {
		String payload = """
				{"purchasedOn":"2026-03-04","storeName":"동네마트","items":[%s]}
				""".formatted(items);
		var request = multipart("/api/v1/user/receipts")
				.file(new MockMultipartFile("image", "receipt.png", "image/png", new byte[] {1, 2, 3}))
				.file(new MockMultipartFile("payload", "", "application/json", payload.getBytes(StandardCharsets.UTF_8)));
		return request.cookie(accessToken(userId));
	}

	@Test
	void 잘못된_요청은_저장_전에_거절한다() throws Exception {
		mockMvc.perform(request("양파", "가".repeat(51))).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("items[1].name"));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isZero();
		mockMvc.perform(request("양파", "우유")).andExpect(status().isCreated());
	}

	@Test
	void 구매일이_정확히_6개월_전인_영수증은_등록한다() throws Exception {
		clock.advance(Duration.ofDays(184)); // 3월 4일 → 9월 4일
		mockMvc.perform(request("양파", "우유")).andExpect(status().isCreated());
	}

	@Test
	void 구매일이_6개월보다_하루_더_지난_영수증은_저장_전에_거절한다() throws Exception {
		clock.advance(Duration.ofDays(185)); // 3월 4일 → 9월 5일
		mockMvc.perform(request("양파", "우유")).andExpect(status().is(422))
				.andExpect(jsonPath("$.code").value("RECEIPT_TOO_OLD"));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isZero();
		try (var files = Files.list(imageDirectory())) {
			assertThat(files).isEmpty();
		}
	}

	@Test
	void 같은_영수증을_동시_등록해도_한_번만_저장한다() throws Exception {
		var start = new CyclicBarrier(2);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var requests = executor.<Integer>invokeAll(List.of(
					() -> registerAfter(start), () -> registerAfter(start)), 15, TimeUnit.SECONDS);
			assertThat(List.of(requests.get(0).get(), requests.get(1).get()))
					.containsExactlyInAnyOrder(201, 409);
		}
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isEqualTo(1);

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForList("SELECT event_name FROM events", String.class))
				.containsExactlyInAnyOrder("receipt_saved", "ingredient_added", "ingredient_added");
		try (var files = Files.list(imageDirectory())) {
			assertThat(files.count()).isEqualTo(1);
		}
	}

	@Test
	void 영수증_중복은_사용자별로_검사한다() throws Exception {
		mockMvc.perform(request("양파", "우유")).andExpect(status().isCreated());
		mockMvc.perform(receiptRequest("양파", "우유").cookie(accessToken(createUser())))
				.andExpect(status().isCreated());
		mockMvc.perform(request("양파", "우유")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECEIPT_DUPLICATE"));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isEqualTo(2);
	}

	@Test
	void 영수증_번호가_없으면_같은_날의_품목명을_순서와_관계없이_비교한다() throws Exception {
		mockMvc.perform(request("양파", "우유")).andExpect(status().isCreated());
		mockMvc.perform(receiptRequest("우유", "양파", "").cookie(accessToken(userId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECEIPT_DUPLICATE"));
		mockMvc.perform(receiptRequest("당근", "우유", "").cookie(accessToken(userId)))
				.andExpect(status().isCreated());
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isEqualTo(2);
	}

	@Test
	void 인증_또는_필수_요청이_없으면_등록하지_않는다() throws Exception {
		mockMvc.perform(multipart("/api/v1/user/receipts"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(multipart("/api/v1/user/receipts").cookie(accessToken(userId)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isZero();
	}

	private int registerAfter(CyclicBarrier start) throws Exception {
		start.await(5, TimeUnit.SECONDS);
		return mockMvc.perform(request("양파", "우유").header("Origin", "http://localhost:3000"))
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
				.andReturn().getResponse().getStatus();
	}

	@Test
	void 재료_등록에_실패하면_영수증과_사진을_정리한다() throws Exception {
		// HTTP 검증을 거치지 않고 긴 이름을 넘겨, 저장 도중 재료 쪽에서 실패하게 한다.
		var item = new RegisterReceiptUseCase.ItemRequest("가".repeat(51), IngredientCategoryCode.VEGETABLE,
				BigDecimal.ONE, null, StorageLocation.PANTRY, null, null, null, List.of());
		var command = new RegisterReceiptUseCase.Command(LocalDate.of(2026, 3, 4), "동네마트", null, null, null,
				List.of(item));
		assertThatThrownBy(() -> registerReceiptUseCase.register(userId, new ByteArrayInputStream(new byte[] {1}), command))
				.isInstanceOf(BusinessException.class);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipts", Long.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM receipt_items", Long.class)).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
		try (var files = Files.list(imageDirectory())) {
			assertThat(files).isEmpty();
		}
	}

	private MockMultipartHttpServletRequestBuilder request(String selectedName, String otherName) {
		return receiptRequest(selectedName, otherName).cookie(accessToken(userId));
	}

	private Cookie accessToken(Long ownerId) {
		return new Cookie("ACCESS_TOKEN", jwtTokens.issueAccessToken(ownerId, UserRole.USER));
	}

	private MockMultipartHttpServletRequestBuilder receiptRequest(String selectedName, String otherName) {
		return receiptRequest(selectedName, otherName, "000123");
	}

	private MockMultipartHttpServletRequestBuilder receiptRequest(String selectedName, String otherName,
			String receiptNumber) {
		String payload = """
				{
				  "purchasedOn": "2026-03-04",
				  "purchasedTime": "14:25:00",
				  "receiptNumber": "%s",
				  "storeName": "동네마트",
				  "items": [
				    {"name":"%s","categoryCode":"VEGETABLE","amount":2,"price":2000,"storage":"PANTRY"},
				    {"name":"%s","categoryCode":"DAIRY_EGG","amount":1,"price":2500,"storage":"FRIDGE"}
				  ]
				}
				""".formatted(receiptNumber, selectedName, otherName);
		var request = multipart("/api/v1/user/receipts")
				.file(new MockMultipartFile("image", "receipt.png", "image/png", new byte[] {1, 2, 3}))
				.file(new MockMultipartFile("payload", "", "application/json", payload.getBytes(StandardCharsets.UTF_8)));
		return request;
	}
}
