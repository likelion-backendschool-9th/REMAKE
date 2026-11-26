package com.remake.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.remake.auth.application.port.out.JwtTokenPort;
import com.remake.ingredient.application.port.in.RegisterCartIngredientsUseCase;
import com.remake.ingredient.application.port.in.RegisterIngredientCommand;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;
import com.remake.receipt.application.port.in.DeleteReceiptsForUserUseCase;
import com.remake.receipt.application.port.in.UpdateManualIngredientExpensePriceUseCase;
import com.remake.support.IntegrationTestSupport;
import com.remake.user.domain.UserRole;

import tools.jackson.databind.json.JsonMapper;

class ExpenseHistoryApiTest extends IntegrationTestSupport {

	private static final String EXPENSES = "/api/v1/user/expenses";
	private static final String INGREDIENTS = "/api/v1/user/ingredients";
	private static final String RECEIPTS = "/api/v1/user/receipts";

	@Autowired
	private JwtTokenPort tokens;

	@Autowired
	private JsonMapper json;

	@Autowired
	private DeleteReceiptsForUserUseCase deleteReceiptsForUser;

	@Autowired
	private RegisterCartIngredientsUseCase registerCartIngredients;

	@Autowired
	private UpdateManualIngredientExpensePriceUseCase updateManualExpensePrice;

	private Long owner;

	@BeforeEach
	void createOwner() {
		owner = createUser();
	}

	@Test
	void 월별_카테고리와_일별_합계가_날짜별_품목_합계와_같다() throws Exception {
		registerReceipt(owner, "2026-03-03", 3000);
		registerIngredient(owner, "1000");
		registerIngredient(owner, "null");
		registerIngredient(createUser(), "7000");
		registerReceipt(createUser(), "2026-03-03", 9000);

		mockMvc.perform(get(EXPENSES + "/patterns").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.month").value("2026-03"))
				.andExpect(jsonPath("$.totalAmount").value(4000))
				.andExpect(jsonPath("$.averageDiffAmount").doesNotExist())
				.andExpect(jsonPath("$.categories.length()").value(2))
				.andExpect(jsonPath("$.categories[?(@.categoryCode == 'VEGETABLE')].ratio").value(contains(0.25)))
				.andExpect(jsonPath("$.categories[?(@.categoryCode == 'MEAT')].ratio").value(contains(0.75)))
				.andExpect(jsonPath("$.dailyTotals[0].date").value("2026-03-03"))
				.andExpect(jsonPath("$.dailyTotals[0].amount").value(3000))
				.andExpect(jsonPath("$.dailyTotals[1].amount").value(1000));

		mockMvc.perform(get(EXPENSES + "/daily-items").param("date", "2026-03-03").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.date").value("2026-03-03"))
				.andExpect(jsonPath("$.totalAmount").value(3000))
				.andExpect(jsonPath("$.items[0].name").value("돼지고기"))
				.andExpect(jsonPath("$.items[0].categoryCode").value("MEAT"))
				.andExpect(jsonPath("$.items[0].storage").value("FRIDGE"))
				.andExpect(jsonPath("$.items[0].amount").value(500))
				.andExpect(jsonPath("$.items[0].unit").value("G"))
				.andExpect(jsonPath("$.items[0].imageUrl").isString())
				.andExpect(jsonPath("$.items[0].price").value(3000));
	}

	@Test
	void 재료가_삭제되거나_소진되어도_직접_등록_지출만_남는다() throws Exception {
		long receiptId = registerReceipt(owner, "2026-03-03", 3000);
		long deleted = registerIngredient(owner, "1000");
		long discarded = registerIngredient(owner, "1500");

		mockMvc.perform(delete(INGREDIENTS + "/" + deleted).cookie(cookie(owner)))
				.andExpect(status().isNoContent());
		mockMvc.perform(post(INGREDIENTS + "/" + discarded + "/consume").cookie(cookie(owner))
						.contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"DISCARDED\",\"amount\":1}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.deleted").value(true));
		mockMvc.perform(delete(RECEIPTS + "/" + receiptId).cookie(cookie(owner)))
				.andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM ingredients WHERE user_id = ? AND receipt_item_id IS NULL AND price = 3000",
				Long.class, owner)).isEqualTo(1);
		mockMvc.perform(get(EXPENSES + "/patterns").cookie(cookie(owner)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalAmount").value(2500));
		mockMvc.perform(get(EXPENSES + "/daily-items").param("date", "2026-03-04").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(2500))
				.andExpect(jsonPath("$.items.length()").value(2));
	}

	@Test
	void 직접_등록_지출_가격을_바꾸면_등록일은_그대로이고_합계에_반영된다() throws Exception {
		long noPrice = registerIngredient(owner, "null");
		long priced = registerIngredient(owner, "3000");
		long receiptOnly = registerReceipt(owner, "2026-03-03", 5000);
		long receiptIngredientId = jdbcTemplate.queryForObject(
				"SELECT id FROM ingredients WHERE user_id = ? AND receipt_item_id IS NOT NULL", Long.class, owner);

		transactionTemplate.executeWithoutResult(status -> updateManualExpensePrice.updatePrice(noPrice, 3000L));
		transactionTemplate.executeWithoutResult(status -> updateManualExpensePrice.updatePrice(priced, 3500L));
		transactionTemplate.executeWithoutResult(status -> updateManualExpensePrice.updatePrice(receiptIngredientId, 999L));

		assertThat(jdbcTemplate.queryForObject(
				"SELECT price FROM manual_ingredient_expenses WHERE ingredient_id = ?", Long.class, noPrice))
				.isEqualTo(3000L);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT price FROM manual_ingredient_expenses WHERE ingredient_id = ?", Long.class, priced))
				.isEqualTo(3500L);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT CAST(created_at AS date) FROM manual_ingredient_expenses WHERE ingredient_id = ?",
				LocalDate.class, noPrice)).isEqualTo(LocalDate.of(2026, 3, 4));
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM manual_ingredient_expenses WHERE ingredient_id = ?", Long.class,
				receiptIngredientId)).isZero();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT price FROM receipt_items WHERE receipt_id = ?", Long.class, receiptOnly)).isEqualTo(5000L);

		mockMvc.perform(get(EXPENSES + "/patterns").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(11500));
	}

	@Test
	void 직접_등록_지출_가격을_지우면_합계에서_빠지고_행_없는_id는_무시한다() throws Exception {
		long priced = registerIngredient(owner, "3000");
		long missingId = priced + 9_999;

		transactionTemplate.executeWithoutResult(status -> updateManualExpensePrice.updatePrice(priced, null));
		transactionTemplate.executeWithoutResult(status -> updateManualExpensePrice.updatePrice(missingId, 1000L));

		assertThat(jdbcTemplate.queryForObject(
				"SELECT price FROM manual_ingredient_expenses WHERE ingredient_id = ?", Long.class, priced)).isNull();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM manual_ingredient_expenses WHERE ingredient_id = ?", Long.class, missingId))
				.isZero();
		mockMvc.perform(get(EXPENSES + "/patterns").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(0))
				.andExpect(jsonPath("$.categories").isEmpty())
				.andExpect(jsonPath("$.dailyTotals").isEmpty());
	}

	@Test
	void 가격_없는_직접_등록과_장바구니_이동은_출처만_남기고_식비에서_제외한다() throws Exception {
		long directId = registerIngredient(owner, "null");
		long cartId = registerCartIngredients.registerCartIngredients(List.of(new RegisterIngredientCommand(
				owner, "우유", IngredientCategoryCode.DAIRY_EGG, StorageLocation.FRIDGE,
				null, null, BigDecimal.ONE, IngredientUnit.EA, null, null))).getFirst();
		long receiptId = registerReceipt(owner, "2026-03-03", 3000);
		mockMvc.perform(delete(RECEIPTS + "/" + receiptId).cookie(cookie(owner)))
				.andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForList("""
				SELECT ingredient_id FROM manual_ingredient_expenses WHERE user_id = ? AND price IS NULL
				""", Long.class, owner)).containsExactlyInAnyOrder(directId, cartId);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT count(*) FROM manual_ingredient_expenses WHERE user_id = ?", Long.class, owner)).isEqualTo(2);
		mockMvc.perform(get(EXPENSES + "/patterns").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(0))
				.andExpect(jsonPath("$.categories").isEmpty())
				.andExpect(jsonPath("$.dailyTotals").isEmpty());
	}

	@Test
	void 회원_탈퇴_정리에서는_이미_삭제한_재료의_지출도_제거한다() throws Exception {
		long deleted = registerIngredient(owner, "1000");
		Long other = createUser();
		registerIngredient(other, "2000");
		mockMvc.perform(delete(INGREDIENTS + "/" + deleted).cookie(cookie(owner)))
				.andExpect(status().isNoContent());

		deleteReceiptsForUser.deleteReceiptsForUser(owner);

		assertThat(jdbcTemplate.queryForList("SELECT user_id FROM manual_ingredient_expenses", Long.class))
				.containsExactly(other);
	}

	@Test
	void 등록일은_KST_월_경계를_따르고_평균은_기록이_있는_지난달만_사용한다() throws Exception {
		clock.setNow(ZonedDateTime.parse("2026-02-28T23:59:59+09:00[Asia/Seoul]"));
		registerIngredient(owner, "1000");
		clock.setNow(ZonedDateTime.parse("2026-03-01T00:00:00+09:00[Asia/Seoul]"));
		registerIngredient(owner, "3000");
		registerReceipt(owner, "2026-01-31", 1501);

		mockMvc.perform(get(EXPENSES + "/patterns").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(3000))
				.andExpect(jsonPath("$.averageDiffAmount").value(1749))
				.andExpect(jsonPath("$.dailyTotals[0].date").value("2026-03-01"));
		mockMvc.perform(get(EXPENSES + "/patterns").param("month", "2026-02").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(1000))
				.andExpect(jsonPath("$.dailyTotals[0].date").value("2026-02-28"));
	}

	@Test
	void 빈_월과_날짜는_빈_목록을_주고_로그인이_필요하다() throws Exception {
		mockMvc.perform(get(EXPENSES + "/patterns")).andExpect(status().isUnauthorized());
		mockMvc.perform(get(EXPENSES + "/patterns").param("month", "2025-10").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(0))
				.andExpect(jsonPath("$.categories").isEmpty())
				.andExpect(jsonPath("$.dailyTotals").isEmpty());
		mockMvc.perform(get(EXPENSES + "/daily-items").param("date", "2025-10-01").cookie(cookie(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalAmount").value(0))
				.andExpect(jsonPath("$.items").isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = {"2025-09", "2026-04", "2026-13", "invalid"})
	void 잘못된_월은_400을_준다(String month) throws Exception {
		mockMvc.perform(get(EXPENSES + "/patterns").param("month", month).cookie(cookie(owner)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"2025-09-30", "2026-04-01", "2026-02-31", " "})
	void 잘못된_날짜는_400을_준다(String date) throws Exception {
		mockMvc.perform(get(EXPENSES + "/daily-items").param("date", date).cookie(cookie(owner)))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	private long registerIngredient(Long userId, String price) throws Exception {
		var response = mockMvc.perform(post(INGREDIENTS).cookie(cookie(userId)).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"양파","categoryCode":"VEGETABLE","storage":"PANTRY","expirationDate":"2026-03-31","amount":1,"price":%s}
								""".formatted(price)))
				.andExpect(status().isCreated()).andReturn().getResponse();
		return json.readTree(response.getContentAsByteArray()).get("id").asLong();
	}

	private long registerReceipt(Long userId, String date, long price) throws Exception {
		var image = new MockMultipartFile("image", "receipt.jpg", "image/jpeg", new byte[]{1, 2, 3});
		var payload = new MockMultipartFile("payload", "", "application/json", """
				{"storeName":"동네마트","purchasedOn":"%s","items":[
				  {"name":"돼지고기","categoryCode":"MEAT","storage":"FRIDGE","amount":500,"price":%d}
				]}
				""".formatted(date, price).getBytes(StandardCharsets.UTF_8));
		var response = mockMvc.perform(multipart(RECEIPTS).file(image).file(payload).cookie(cookie(userId)))
				.andExpect(status().isCreated()).andReturn().getResponse();
		return json.readTree(response.getContentAsByteArray()).get("receiptId").asLong();
	}

	private Cookie cookie(Long userId) {
		return new Cookie("ACCESS_TOKEN", tokens.issueAccessToken(userId, UserRole.USER));
	}
}
