package com.remake.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.cart.application.port.in.CartDeductionCommand;
import com.remake.cart.application.port.in.DeductCartItemsUseCase;
import com.remake.cart.application.port.in.MoveCartItemsToFridgeUseCase;
import com.remake.cart.application.port.in.UpdateCartItemUseCase;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.support.IntegrationTestSupport;

class CartApiTest extends IntegrationTestSupport {
	@Autowired private MoveCartItemsToFridgeUseCase move;
	@Autowired private DeductCartItemsUseCase deduct;
	@Autowired private UpdateCartItemUseCase update;

	private Long item(Long user, Long recipe, String name, String amount, String unit, String category) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO cart_items(user_id, recipe_id, category_id, name, amount, unit, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
				""", Long.class, user, recipe, categoryId(category), name, new BigDecimal(amount), unit, now, now);
	}

	private Long item(Long user, Long recipe) {
		return item(user, recipe, "양파", "2", "EA", "VEGETABLE");
	}

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder request, Long user) {
		return request.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user);
	}

	private MockHttpServletRequestBuilder select(String action, String ids, Long user) {
		return asUser(post("/api/v1/user/cart-items/" + action).contentType(MediaType.APPLICATION_JSON)
				.content("{\"ids\":" + ids + "}"), user);
	}

	private BigDecimal amount(Long id) {
		return jdbcTemplate.queryForObject("SELECT amount FROM cart_items WHERE id = ?", BigDecimal.class, id);
	}

	@Test
	void 빈목록은_items_배열이고_로그인하지_않으면_401이다() throws Exception {
		Long user = createUser();
		mockMvc.perform(asUser(get("/api/v1/user/cart-items"), user)).andExpect(status().isOk())
				.andExpect(jsonPath("$.items").isEmpty());
		mockMvc.perform(get("/api/v1/user/cart-items")).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/user/cart-items/delete").contentType(MediaType.APPLICATION_JSON)
				.content("{\"ids\":[1]}" )).andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/v1/user/cart-items/move-to-fridge").contentType(MediaType.APPLICATION_JSON)
				.content("{\"ids\":[1]}" )).andExpect(status().isUnauthorized());
		mockMvc.perform(patch("/api/v1/user/cart-items/1").contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":1}" )).andExpect(status().isUnauthorized());
	}

	@Test
	void 목록은_본인행만_반환하고_다른레시피의_같은재료는_합치지않는다() throws Exception {
		Long user = createUser(); Long other = createUser();
		Long first = item(user, createRecipe(user)); Long second = item(user, createRecipe(user));
		item(other, createRecipe(other));
		mockMvc.perform(asUser(get("/api/v1/user/cart-items"), user)).andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(2))
				.andExpect(jsonPath("$.items[0].id").value(first))
				.andExpect(jsonPath("$.items[1].id").value(second))
				.andExpect(jsonPath("$.items[0].recipeTitle").value("테스트 레시피"))
				.andExpect(jsonPath("$.items[0].categoryCode").value("VEGETABLE"))
				.andExpect(jsonPath("$.items[0].imageUrl").isString())
				.andExpect(jsonPath("$.items[0].amount").value(2))
				.andExpect(jsonPath("$.items[0].unit").value("EA"));
	}

	@Test
	void 저장하지않은_타인원본도_본인장바구니의_출처제목은_표시한다() throws Exception {
		Long user = createUser(); Long author = createUser(); Long recipe = createRecipe(author);
		item(user, recipe);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM saved_recipes", Long.class)).isZero();
		mockMvc.perform(asUser(get("/api/v1/user/cart-items"), user)).andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].recipeId").value(recipe))
				.andExpect(jsonPath("$.items[0].recipeTitle").value("테스트 레시피"));
	}

	@Test
	void 출처레시피가_없는_과거행도_null제목으로_조회한다() throws Exception {
		Long user = createUser(); item(user, null);
		mockMvc.perform(asUser(get("/api/v1/user/cart-items"), user)).andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].recipeId").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$.items[0].recipeTitle").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void 수량변경은_단위와출처를_유지하고_소수셋째자리까지_허용한다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user); Long id = item(user, recipe);
		clock.advance(Duration.ofMinutes(1));
		mockMvc.perform(asUser(patch("/api/v1/user/cart-items/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"amount\":1.234}"), user)).andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.amount").value(1.234))
				.andExpect(jsonPath("$.unit").value("EA"));
		assertThat(amount(id)).isEqualByComparingTo("1.234");
		var row = jdbcTemplate.queryForMap("SELECT recipe_id, name, updated_at FROM cart_items WHERE id = ?", id);
		assertThat(row.get("recipe_id")).isEqualTo(recipe);
		assertThat(row.get("name")).isEqualTo("양파");
		assertThat(((java.sql.Timestamp) row.get("updated_at")).toLocalDateTime()).isEqualTo(LocalDateTime.now(clock));
	}

	@Test
	void 잘못된수량과_알수없는필드는_거절한다() throws Exception {
		Long user = createUser(); Long id = item(user, createRecipe(user));
		for (String body : List.of("{}", "{\"amount\":null}", "{\"amount\":0}", "{\"amount\":-1}",
				"{\"amount\":0.0001}", "{\"amount\":10000000}", "{\"amount\":1,\"unit\":\"KG\"}")) {
			mockMvc.perform(asUser(patch("/api/v1/user/cart-items/" + id).contentType(MediaType.APPLICATION_JSON)
					.content(body), user)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		}
		assertThat(amount(id)).isEqualByComparingTo("2");
	}

	@Test
	void 타인항목과_없는항목은_수량변경_404다() throws Exception {
		Long user = createUser(); Long other = createUser(); Long foreign = item(other, createRecipe(other));
		for (Long id : List.of(foreign, Long.MAX_VALUE)) {
			mockMvc.perform(asUser(patch("/api/v1/user/cart-items/" + id).contentType(MediaType.APPLICATION_JSON)
					.content("{\"amount\":1}"), user)).andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
		}
		assertThat(amount(foreign)).isEqualByComparingTo("2");
	}

	@Test
	void 선택삭제는_선택한행만_지우고_저장목록과_원본을_유지한다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user); Long id = item(user, recipe);
		Long untouched = item(user, createRecipe(user));
		jdbcTemplate.update("INSERT INTO saved_recipes(user_id, recipe_id, saved_at) VALUES (?, ?, ?)", user, recipe, LocalDateTime.now(clock));
		mockMvc.perform(select("delete", "[" + id + "]", user)).andExpect(status().isNoContent());
		assertThat(amount(untouched)).isEqualByComparingTo("2");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM recipes WHERE id = ?", Long.class, recipe)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM saved_recipes WHERE recipe_id = ?", Long.class, recipe)).isEqualTo(1);
	}

	@Test
	void 삭제와이동에_타인이나없는항목이_섞이면_전체를_유지한다() throws Exception {
		Long user = createUser(); Long other = createUser();
		Long id = item(user, createRecipe(user)); Long foreign = item(other, createRecipe(other));
		for (String action : List.of("delete", "move-to-fridge")) {
			for (Long target : List.of(foreign, Long.MAX_VALUE)) {
				mockMvc.perform(select(action, "[" + id + "," + target + "]", user))
						.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("CART_ITEM_NOT_FOUND"));
			}
		}
		assertThat(amount(id)).isEqualByComparingTo("2");
		assertThat(amount(foreign)).isEqualByComparingTo("2");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isZero();
	}

	@Test
	void 빈목록_중복_잘못된선택은_400이다() throws Exception {
		Long user = createUser(); Long id = item(user, createRecipe(user));
		for (String action : List.of("delete", "move-to-fridge")) {
			for (String ids : List.of("[]", "null", "[null]", "[0]", "[-1]", "[" + id + "," + id + "]")) {
				mockMvc.perform(select(action, ids, user)).andExpect(status().isBadRequest())
						.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
			}
		}
		assertThat(amount(id)).isEqualByComparingTo("2");
	}

	@Test
	void 이동은_수량과단위를_보존하고_카테고리기본값과_이벤트를_반영한다() throws Exception {
		Long user = createUser();
		Long first = item(user, createRecipe(user), "양파", "200", "G", "VEGETABLE");
		Long second = item(user, createRecipe(user), "소스", "2", "TBSP", "SEASONING");
		mockMvc.perform(select("move-to-fridge", "[" + first + "," + second + "]", user))
				.andExpect(status().isOk()).andExpect(jsonPath("$.addedCount").value(2));
		var onion = jdbcTemplate.queryForMap("SELECT * FROM ingredients WHERE name = '양파'");
		assertThat(onion.get("amount")).isEqualTo(new BigDecimal("200.000"));
		assertThat(onion.get("unit")).isEqualTo("G");
		assertThat(onion.get("initial_amount")).isEqualTo(onion.get("amount"));
		assertThat(onion.get("storage")).isEqualTo("FRIDGE");
		assertThat(onion.get("expiration_date")).isEqualTo(java.sql.Date.valueOf(LocalDate.now(clock).plusDays(7)));
		assertThat(onion.get("price")).isNull();
		var sauce = jdbcTemplate.queryForMap("SELECT amount, unit, storage, expiration_date, price FROM ingredients WHERE name = '소스'");
		assertThat(sauce.get("amount")).isEqualTo(new BigDecimal("30.000"));
		assertThat(sauce.get("unit")).isEqualTo("ML");
		assertThat(sauce.get("storage")).isEqualTo("PANTRY");
		assertThat(sauce.get("expiration_date")).isEqualTo(java.sql.Date.valueOf(LocalDate.now(clock).plusDays(180)));
		assertThat(sauce.get("price")).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM cart_items", Long.class)).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events WHERE event_name='ingredient_added' AND properties->>'add_source'='cart' AND screen='cart_list'", Long.class)).isEqualTo(2);
		var event = jdbcTemplate.queryForMap("SELECT properties->>'moved_count' AS moved, properties->>'was_select_all' AS all_selected FROM events WHERE event_name='cart_moved_to_fridge'");
		assertThat(event.get("moved")).isEqualTo("2");
		assertThat(event.get("all_selected")).isEqualTo("true");
	}

	@Test
	void 일부행만_이동하면_다른레시피재료는_남고_전체선택은_false다() {
		Long user = createUser(); Long first = item(user, createRecipe(user)); Long second = item(user, createRecipe(user));
		assertThat(move.move(user, List.of(first))).isEqualTo(1);
		assertThat(amount(second)).isEqualByComparingTo("2");
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT properties->>'was_select_all' FROM events WHERE event_name='cart_moved_to_fridge'", String.class)).isEqualTo("false");
	}

	@Test
	void 환산오버플로면_다른항목도_등록하거나_지우지않는다() throws Exception {
		Long user = createUser(); Long first = item(user, createRecipe(user));
		Long second = item(user, createRecipe(user), "소스", "9999999", "CUP", "SEASONING");
		mockMvc.perform(select("move-to-fridge", "[" + first + "," + second + "]", user))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		assertThat(amount(first)).isEqualByComparingTo("2");
		assertThat(amount(second)).isEqualByComparingTo("9999999");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isZero();
	}

	@Test
	void 이동중_DB실패면_냉장고등록_장바구니삭제_이벤트가_모두_롤백된다() throws Exception {
		Long user = createUser(); Long first = item(user, createRecipe(user));
		Long second = item(user, createRecipe(user), "소스", "2", "ML", "SEASONING");
		jdbcTemplate.execute("ALTER TABLE ingredients ADD CONSTRAINT test_reject_cart_move CHECK (name <> '소스')");
		try {
			mockMvc.perform(select("move-to-fridge", "[" + first + "," + second + "]", user))
					.andExpect(status().isInternalServerError());
			assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isZero();
			assertThat(amount(first)).isEqualByComparingTo("2");
			assertThat(amount(second)).isEqualByComparingTo("2");
			awaitEventWrites();
			assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
		} finally { jdbcTemplate.execute("ALTER TABLE ingredients DROP CONSTRAINT test_reject_cart_move"); }
	}

	@Test
	void 호출자가_이동뒤_실패해도_전체가_롤백된다() {
		Long user = createUser(); Long id = item(user, createRecipe(user));
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(tx -> {
			move.move(user, List.of(id));
			throw new IllegalStateException("호출자 실패");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(amount(id)).isEqualByComparingTo("2");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM events", Long.class)).isZero();
	}

	@Test
	void 동시에_같은행을_이동해도_냉장고에는_한번만_등록된다() throws Exception {
		Long user = createUser(); Long id = item(user, createRecipe(user));
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Callable<String> action = () -> {
				start.await();
				try { return "added=" + move.move(user, List.of(id)); }
				catch (BusinessException e) { return e.getErrorCode().name(); }
			};
			var first = executor.submit(action); var second = executor.submit(action); start.countDown();
			assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder("added=1", "CART_ITEM_NOT_FOUND");
		}
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isEqualTo(1);
	}

	@Test
	void 반대순서로_보낸_여러행의_동시이동도_잠금순서가_같다() throws Exception {
		Long user = createUser(); Long a = item(user, createRecipe(user)); Long b = item(user, createRecipe(user));
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Callable<String> firstAction = () -> { start.await(); try { move.move(user, List.of(a, b)); return "OK"; }
				catch (BusinessException e) { return e.getErrorCode().name(); } };
			Callable<String> secondAction = () -> { start.await(); try { move.move(user, List.of(b, a)); return "OK"; }
				catch (BusinessException e) { return e.getErrorCode().name(); } };
			var first = executor.submit(firstAction); var second = executor.submit(secondAction); start.countDown();
			assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
					.containsExactlyInAnyOrder("OK", "CART_ITEM_NOT_FOUND");
		}
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients", Long.class)).isEqualTo(2);
	}

	@Test
	void 수량변경과_영수증차감은_같은행잠금으로_직렬화된다() throws Exception {
		Long user = createUser(); Long id = item(user, createRecipe(user), "양파", "5", "EA", "VEGETABLE");
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> { start.await(); update.updateAmount(user, id, new BigDecimal("10")); return 1; });
			var second = executor.submit(() -> { start.await(); return deduct.deduct(user,
					List.of(new CartDeductionCommand(id, "양파", new BigDecimal("2"), IngredientUnit.EA))); });
			start.countDown(); first.get(15, TimeUnit.SECONDS); assertThat(second.get(15, TimeUnit.SECONDS)).isZero();
		}
		assertThat(amount(id)).isIn(new BigDecimal("8.000"), new BigDecimal("10.000"));
	}

	@Test
	void 수동담기_API는_없다() throws Exception {
		Long user = createUser();
		mockMvc.perform(asUser(post("/api/v1/user/cart-items").contentType(MediaType.APPLICATION_JSON)
				.content("{}"), user)).andExpect(status().isMethodNotAllowed());
	}
}
