package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.ingredient.application.port.in.ConsumeIngredientCommand;
import com.remake.ingredient.application.port.in.ConsumeIngredientUseCase;
import com.remake.ingredient.domain.ConsumeVia;
import com.remake.ingredient.domain.IngredientLogType;
import com.remake.ingredient.domain.IngredientStatus;
import com.remake.support.IntegrationTestSupport;

// 재료 사용·폐기(consume): API와 요리 후 차감이 쓰는 UseCase를 DB·이벤트까지 확인한다. 오늘은 시험용 시계의 2026-03-04이다.
class IngredientConsumeTest extends IntegrationTestSupport {

	private static final Long ME = 101L;
	private static final Long OTHER = 102L;
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Autowired
	private ConsumeIngredientUseCase consumeUseCase;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@BeforeEach
	void setUpUsers() {
		ensureUser(ME);
		ensureUser(OTHER);
	}

	/** 우유 4팩(4000원). 소비기한은 daysLeft일 뒤 */
	private Long milk(Long userId, int daysLeft, Long price) {
		Long id = createIngredient(userId, "DAIRY_EGG", "우유", TODAY.plusDays(daysLeft), IngredientStatus.ACTIVE, price);
		jdbcTemplate.update("UPDATE ingredients SET amount = 4, initial_amount = 4, unit = 'PACK' WHERE id = ?", id);
		return id;
	}

	private MockHttpServletRequestBuilder consume(Long userId, Long id, String body) {
		return post("/api/v1/user/ingredients/" + id + "/consume")
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId)
				.contentType(MediaType.APPLICATION_JSON).content(body);
	}

	private static String body(String type, String amount) {
		return "{ \"type\": \"" + type + "\", \"amount\": " + amount + " }";
	}

	private Map<String, Object> onlyLog() {
		return jdbcTemplate.queryForMap("SELECT * FROM ingredient_logs");
	}

	@Test
	void 일부를_사용하면_남은_양이_줄고_사용_기록이_남는다() throws Exception {
		Long id = milk(ME, 5, 4000L);

		mockMvc.perform(consume(ME, id, body("USED", "1.5")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.remainingAmount").value(2.5))
				.andExpect(jsonPath("$.deleted").value(false))
				.andExpect(jsonPath("$.costAmount").doesNotExist());

		Map<String, Object> log = onlyLog();
		assertThat(log.get("type")).isEqualTo("USED");
		assertThat((BigDecimal) log.get("amount")).isEqualByComparingTo("1.5");
		assertThat(log.get("unit")).isEqualTo("PACK");
		assertThat(log.get("name")).isEqualTo("우유");
		assertThat(log.get("user_id")).isEqualTo(ME);
		assertThat(log.get("cost_amount")).isNull();
		assertThat(log.get("ingredient_id")).isEqualTo(id);
		assertThat(jdbcTemplate.queryForObject("SELECT initial_amount FROM ingredients WHERE id = ?", BigDecimal.class,
				id)).isEqualByComparingTo("4");
	}

	@Test
	void 남은_양을_모두_사용하면_재료가_지워지고_기록은_이름과_함께_남는다() throws Exception {
		Long id = milk(ME, 5, null);

		mockMvc.perform(consume(ME, id, body("USED", "4")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.remainingAmount").value(0))
				.andExpect(jsonPath("$.deleted").value(true));

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients WHERE id = ?", Long.class, id)).isZero();
		// 기록은 재료 연결 없이(ingredient_id null) 이름·단위와 함께 남고, 같은 재료를 또 빼면 없는 재료다
		Map<String, Object> log = onlyLog();
		assertThat(log.get("ingredient_id")).isNull();
		assertThat(log.get("name")).isEqualTo("우유");
		assertThat(log.get("unit")).isEqualTo("PACK");
		assertThat((BigDecimal) log.get("amount")).isEqualByComparingTo("4");
		mockMvc.perform(consume(ME, id, body("USED", "1")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/user/ingredients").requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, ME))
				.andExpect(jsonPath("$.items.length()").value(0));
	}

	@Test
	void 버리면_가격에_비례한_금액이_기록에_남는다() throws Exception {
		Long id = milk(ME, 5, 4000L);

		mockMvc.perform(consume(ME, id, body("DISCARDED", "1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.remainingAmount").value(3))
				.andExpect(jsonPath("$.deleted").value(false))
				.andExpect(jsonPath("$.costAmount").value(1000));

		Map<String, Object> log = onlyLog();
		assertThat(log.get("type")).isEqualTo("DISCARDED");
		assertThat(log.get("cost_amount")).isEqualTo(1000L);
	}

	@Test
	void 가격이_없는_재료를_버리면_금액은_null로_남는다() throws Exception {
		Long id = milk(ME, 5, null);

		mockMvc.perform(consume(ME, id, body("DISCARDED", "1")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.costAmount").doesNotExist());

		assertThat(onlyLog().get("cost_amount")).isNull();
	}

	@Test
	void 여러_번_나눠_버려도_금액은_처음_양_기준이다() throws Exception {
		Long id = createIngredient(ME, "DAIRY_EGG", "우유", TODAY.plusDays(5), IngredientStatus.ACTIVE, 1000L);
		jdbcTemplate.update("UPDATE ingredients SET amount = 3, initial_amount = 3, unit = 'PACK' WHERE id = ?", id);

		mockMvc.perform(consume(ME, id, body("DISCARDED", "1"))).andExpect(status().isOk());
		mockMvc.perform(consume(ME, id, body("DISCARDED", "1"))).andExpect(status().isOk());
		mockMvc.perform(consume(ME, id, body("DISCARDED", "1"))).andExpect(status().isOk());

		assertThat(jdbcTemplate.queryForList("SELECT cost_amount FROM ingredient_logs ORDER BY id", Long.class))
				.containsExactly(333L, 333L, 333L);
	}

	@Test
	void 남은_양보다_많이_빼면_INSUFFICIENT_QUANTITY이고_아무것도_바뀌지_않는다() throws Exception {
		Long id = milk(ME, 5, 4000L);

		mockMvc.perform(consume(ME, id, body("USED", "4.001")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_QUANTITY"))
				.andExpect(jsonPath("$.details.remaining").value(4));

		assertThat(jdbcTemplate.queryForObject("SELECT amount FROM ingredients WHERE id = ?", BigDecimal.class, id))
				.isEqualByComparingTo("4");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredient_logs", Long.class)).isZero();
	}

	@Test
	void 수량_형식이_틀리면_INVALID_INPUT이고_필드_오류를_준다() throws Exception {
		Long id = milk(ME, 5, 4000L);

		mockMvc.perform(consume(ME, id, body("USED", "0")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		mockMvc.perform(consume(ME, id, body("USED", "-1")))
				.andExpect(status().isBadRequest());
		mockMvc.perform(consume(ME, id, body("USED", "0.0001")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
		mockMvc.perform(consume(ME, id, "{ \"type\": \"USED\" }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
		mockMvc.perform(consume(ME, id, "{ \"amount\": 1 }"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("type"));
		mockMvc.perform(consume(ME, id, body("EATEN", "1")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredient_logs", Long.class)).isZero();
	}

	@Test
	void 다른_사용자의_재료나_없는_재료는_404이다() throws Exception {
		Long id = milk(OTHER, 5, 4000L);

		mockMvc.perform(consume(ME, id, body("USED", "1")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
		mockMvc.perform(consume(ME, 999999L, body("USED", "1")))
				.andExpect(status().isNotFound());
	}

	@Test
	void 로그인_정보가_없으면_401이다() throws Exception {
		mockMvc.perform(post("/api/v1/user/ingredients/1/consume").contentType(MediaType.APPLICATION_JSON)
						.content(body("USED", "1")))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void 기록이_있는_재료도_삭제되고_기록은_이름과_함께_남는다() throws Exception {
		Long used = milk(ME, 5, 4000L);
		mockMvc.perform(consume(ME, used, body("DISCARDED", "1"))).andExpect(status().isOk());

		mockMvc.perform(delete("/api/v1/user/ingredients/" + used)
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, ME))
				.andExpect(status().isNoContent());

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients WHERE id = ?", Long.class, used)).isZero();
		Map<String, Object> log = onlyLog();
		assertThat(log.get("ingredient_id")).isNull();
		assertThat(log.get("name")).isEqualTo("우유");
		assertThat(log.get("cost_amount")).isEqualTo(1000L);
	}

	@Test
	void 다른_사용자의_재료는_기록이_있어도_삭제할_수_없다() throws Exception {
		Long id = milk(OTHER, 5, 4000L);
		mockMvc.perform(consume(OTHER, id, body("USED", "1"))).andExpect(status().isOk());

		mockMvc.perform(delete("/api/v1/user/ingredients/" + id)
						.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, ME))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INGREDIENT_NOT_FOUND"));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients WHERE id = ?", Long.class, id)).isEqualTo(1);
		assertThat(onlyLog().get("ingredient_id")).isEqualTo(id);
	}

	// ---- 이벤트 ----

	@Test
	void 사용하면_ingredient_used_이벤트를_코드_값으로만_보낸다() throws Exception {
		Long id = milk(ME, 2, 4000L);

		mockMvc.perform(consume(ME, id, body("USED", "1"))).andExpect(status().isOk());

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT event_name FROM events", String.class)).isEqualTo("ingredient_used");
		assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM events", Long.class)).isEqualTo(ME);
		String props = jdbcTemplate.queryForObject("SELECT properties::text FROM events", String.class);
		assertThat(props).contains("\"use_via\": \"manual\"", "\"category_code\": \"DAIRY_EGG\"",
				"\"expiry_status\": \"soon\"", "\"used_ratio\": 0.25").doesNotContain("우유");
	}

	@Test
	void 버리면_ingredient_discarded_이벤트에_금액과_가격_여부를_넣는다() throws Exception {
		Long id = milk(ME, -1, 4000L);

		mockMvc.perform(consume(ME, id, body("DISCARDED", "2"))).andExpect(status().isOk());

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT event_name FROM events", String.class))
				.isEqualTo("ingredient_discarded");
		String props = jdbcTemplate.queryForObject("SELECT properties::text FROM events", String.class);
		assertThat(props).contains("\"expiry_status\": \"danger\"", "\"used_ratio\": 0.5", "\"has_price\": true",
				"\"cost_amount\": 2000").doesNotContain("use_via");
	}

	@Test
	void 가격이_없는_재료를_버리면_이벤트에_cost_amount가_없다() throws Exception {
		Long id = milk(ME, 20, null);

		mockMvc.perform(consume(ME, id, body("DISCARDED", "1"))).andExpect(status().isOk());

		awaitEventWrites();
		String props = jdbcTemplate.queryForObject("SELECT properties::text FROM events", String.class);
		assertThat(props).contains("\"has_price\": false", "\"expiry_status\": \"ok\"").doesNotContain("cost_amount");
	}

	@Test
	void 거절된_요청은_이벤트를_보내지_않는다() throws Exception {
		Long id = milk(ME, 5, 4000L);

		mockMvc.perform(consume(ME, id, body("USED", "5"))).andExpect(status().isBadRequest());

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
	}

	// ---- 요리 후 차감이 쓰는 UseCase ----

	@Test
	void 요리_후_차감은_COOK으로_기록하고_이벤트의_use_via는_cook이다() {
		Long id = milk(ME, 5, 4000L);

		consumeUseCase.consumeIngredient(
				new ConsumeIngredientCommand(ME, id, IngredientLogType.USED, new BigDecimal("2"), ConsumeVia.COOK));

		assertThat(onlyLog().get("type")).isEqualTo("USED");
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT properties::text FROM events", String.class))
				.contains("\"use_via\": \"cook\"");
		assertThat(jdbcTemplate.queryForObject("SELECT screen FROM events", String.class)).isEqualTo("cook_deduct");
	}

	@Test
	void 요리_후_차감은_폐기_기록을_남길_수_없다() {
		Long id = milk(ME, 5, 4000L);

		assertThatThrownBy(() -> consumeUseCase.consumeIngredient(
				new ConsumeIngredientCommand(ME, id, IngredientLogType.DISCARDED, BigDecimal.ONE, ConsumeVia.COOK)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
	}

	@Test
	void 호출한_쪽이_롤백되면_차감과_기록과_이벤트가_모두_취소된다() {
		Long id = milk(ME, 5, 4000L);

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(txStatus -> {
			consumeUseCase.consumeIngredient(
					new ConsumeIngredientCommand(ME, id, IngredientLogType.USED, new BigDecimal("2"), ConsumeVia.COOK));
			throw new IllegalStateException("요리 기록 저장 실패");
		})).isInstanceOf(IllegalStateException.class);

		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT amount FROM ingredients WHERE id = ?", BigDecimal.class, id))
				.isEqualByComparingTo("4");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredient_logs", Long.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
	}
}
