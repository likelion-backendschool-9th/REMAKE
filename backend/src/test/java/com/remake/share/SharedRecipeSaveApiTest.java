package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.share.application.port.in.PublishSharedRecipeUseCase;
import com.remake.share.application.port.in.SaveSharedRecipeUseCase;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;
import com.remake.support.IntegrationTestSupport;

class SharedRecipeSaveApiTest extends IntegrationTestSupport {
	@Autowired private SaveRecipePort recipes;
	@Autowired private PublishSharedRecipeUseCase publish;
	@Autowired private SaveSharedRecipeUseCase saves;
	private Long reader;
	private Long author;
	private Long recipeId;
	private Long postId;

	@BeforeEach
	void setUp() {
		author = createUser();
		reader = createUser();
		Long onion = createIngredient(author, "VEGETABLE", "양파", LocalDate.now(clock).plusDays(2));
		recipeId = transactionTemplate.execute(tx -> recipes.save(Recipe.create(author, RecipeSource.QUICK,
				"원본", 2, 10, null, null, List.of(
						new RecipeIngredient(onion, "양파", new BigDecimal("2"), RecipeUnit.EA),
						new RecipeIngredient(null, "당근", BigDecimal.ONE, RecipeUnit.EA),
						new RecipeIngredient(null, "소금", null, RecipeUnit.TO_TASTE),
						new RecipeIngredient(null, "후추", BigDecimal.ONE, RecipeUnit.PINCH),
						new RecipeIngredient(null, "물", new BigDecimal("200"), RecipeUnit.ML)),
				List.of("재료를 익힌다")))).id();
		postId = post();
		awaitEventWrites();
	}

	private Long post() {
		byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1};
		return publish.publish(author, recipeId, "공유 제목", new SharedRecipePhotoUpload(
				"image/jpeg", jpeg.length, () -> new ByteArrayInputStream(jpeg)));
	}

	private String url(Long id) { return "/api/v1/user/shared-recipes/" + id + "/save"; }
	private ResultActions save() throws Exception {
		return mockMvc.perform(put(url(postId)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader));
	}
	private ResultActions unsave() throws Exception {
		return mockMvc.perform(delete(url(postId)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader));
	}
	private long count(String table) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class);
	}
	private long events(String name) {
		awaitEventWrites();
		return jdbcTemplate.queryForObject("SELECT count(*) FROM events WHERE user_id=? AND event_name=?",
				Long.class, reader, name);
	}

	@Test
	void 원본참조로_저장하고_반복PUT은_저장수와_장바구니와_이벤트를_늘리지않는다() throws Exception {
		save().andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true))
				.andExpect(jsonPath("$.saveCount").value(1)).andExpect(jsonPath("$.cartAddedCount").value(2));
		assertThat(jdbcTemplate.queryForObject("SELECT recipe_id FROM saved_recipes WHERE user_id=?", Long.class, reader)).isEqualTo(recipeId);
		assertThat(jdbcTemplate.queryForObject("SELECT shared_recipe_id FROM saved_recipes WHERE user_id=?", Long.class, reader)).isEqualTo(postId);
		assertThat(count("recipes")).isEqualTo(1);
		assertThat(count("recipe_steps")).isEqualTo(1);
		assertThat(count("recipe_ingredients")).isEqualTo(5);
		assertThat(jdbcTemplate.queryForList("SELECT name FROM cart_items", String.class)).containsExactlyInAnyOrder("양파", "당근");
		// 이미 저장한 PUT은 사용자가 장바구니에서 지운 재료도 다시 담지 않는다.
		jdbcTemplate.update("DELETE FROM cart_items WHERE user_id=?", reader);
		save().andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(0))
				.andExpect(jsonPath("$.saveCount").value(1));
		assertThat(count("cart_items")).isZero();
		assertThat(events("recipe_saved")).isEqualTo(1);
		assertThat(events("cart_item_added")).isEqualTo(1);
		assertThat(events("shared_recipe_saved")).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT properties->>'save_source' FROM events WHERE user_id=? AND event_name='recipe_saved'", String.class, reader)).isEqualTo("shared");
		assertThat(jdbcTemplate.queryForObject("SELECT properties->>'cart_source' FROM events WHERE user_id=? AND event_name='cart_item_added'", String.class, reader)).isEqualTo("shared_recipe");
		assertThat(jdbcTemplate.queryForObject("SELECT properties->>'cart_added_count' FROM events WHERE user_id=? AND event_name='recipe_saved'", String.class, reader)).isEqualTo("2");
		mockMvc.perform(get("/api/v1/user/saved-recipes").requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(1));
	}

	@Test
	void 취소와_재저장은_기존수정량을_유지하고_현재없는재료만_추가한다() throws Exception {
		createIngredient(reader, "VEGETABLE", "당근", LocalDate.now(clock));
		save().andExpect(jsonPath("$.cartAddedCount").value(1));
		Long cartId = jdbcTemplate.queryForObject("SELECT id FROM cart_items WHERE user_id=?", Long.class, reader);
		jdbcTemplate.update("UPDATE cart_items SET amount=7 WHERE id=?", cartId);
		unsave().andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false)).andExpect(jsonPath("$.saveCount").value(0));
		unsave().andExpect(status().isOk()).andExpect(jsonPath("$.saveCount").value(0));
		assertThat(count("cart_items")).isEqualTo(1);
		clock.advance(Duration.ofDays(1));
		save().andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(1)).andExpect(jsonPath("$.saveCount").value(1));
		assertThat(jdbcTemplate.queryForObject("SELECT amount FROM cart_items WHERE id=?", BigDecimal.class, cartId)).isEqualByComparingTo("7");
		assertThat(count("cart_items")).isEqualTo(2);
		assertThat(events("recipe_saved")).isEqualTo(2);
		assertThat(events("cart_item_added")).isEqualTo(2);
	}

	@Test
	void 이름이정확히같고_유효한재료만_보유로보고_모두있으면_담지않는다() throws Exception {
		createIngredient(reader, "VEGETABLE", "양파", LocalDate.now(clock));
		createIngredient(reader, "VEGETABLE", "당근", LocalDate.now(clock).plusDays(1));
		save().andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(0));
		assertThat(count("cart_items")).isZero();
		assertThat(events("cart_item_added")).isZero();
		assertThat(events("recipe_saved")).isEqualTo(1);
	}

	@Test
	void 만료재료와_부분일치이름은_부족재료를_가리지않는다() throws Exception {
		createIngredient(reader, "VEGETABLE", "양파", LocalDate.now(clock).minusDays(1));
		createIngredient(reader, "VEGETABLE", "다진 당근", LocalDate.now(clock).plusDays(1));
		save().andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(2));
	}

	@Test
	void 다른경로에서_이미저장한원본은_출처와_즐겨찾기와_시각을_바꾸지않는다() throws Exception {
		var before = LocalDateTime.now(clock);
		jdbcTemplate.update("INSERT INTO saved_recipes(user_id, recipe_id, is_favorite, saved_at) VALUES (?, ?, true, ?)", reader, recipeId, before);
		clock.advance(Duration.ofHours(1));
		save().andExpect(status().isOk()).andExpect(jsonPath("$.saveCount").value(0)).andExpect(jsonPath("$.saved").value(true)).andExpect(jsonPath("$.cartAddedCount").value(0));
		assertThat(jdbcTemplate.queryForObject("SELECT shared_recipe_id FROM saved_recipes WHERE user_id=?", Long.class, reader)).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT is_favorite FROM saved_recipes WHERE user_id=?", Boolean.class, reader)).isTrue();
		assertThat(jdbcTemplate.queryForObject("SELECT saved_at FROM saved_recipes WHERE user_id=?", LocalDateTime.class, reader)).isEqualTo(before);
		assertThat(events("recipe_saved")).isZero();
	}

	@Test
	void 같은원본의_다른주게시글은_저장수를_중복으로얻지않고_본인저장만_취소한다() throws Exception {
		save().andExpect(status().isOk());
		clock.advance(Duration.ofDays(7));
		Long second = post();
		mockMvc.perform(put(url(second)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true))
				.andExpect(jsonPath("$.saveCount").value(0)).andExpect(jsonPath("$.cartAddedCount").value(0));
		Long other = createUser();
		saves.save(other, postId);
		mockMvc.perform(delete(url(second)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM saved_recipes WHERE user_id=?", Long.class, other)).isEqualTo(1);
	}

	@Test
	void 숨김과_없는글은404_비로그인은401이다() throws Exception {
		mockMvc.perform(put(url(postId))).andExpect(status().isUnauthorized());
		mockMvc.perform(delete(url(postId))).andExpect(status().isUnauthorized());
		jdbcTemplate.update("UPDATE shared_recipes SET status='HIDDEN' WHERE id=?", postId);
		for (Long id : new Long[]{postId, Long.MAX_VALUE}) {
			mockMvc.perform(put(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
					.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHARED_RECIPE_NOT_FOUND"));
			mockMvc.perform(delete(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
					.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHARED_RECIPE_NOT_FOUND"));
		}
		assertThat(count("saved_recipes")).isZero();
		assertThat(count("cart_items")).isZero();
	}

	@Test
	void 장바구니_DB실패면_저장과_담기와_이벤트가_모두롤백된다() {
		// 카테고리 fallback 재료를 먼저 INSERT한 뒤 다음 재료에서 실패해도 모두 롤백한다.
		jdbcTemplate.update("UPDATE recipe_ingredients SET name='가나다채소', category_code='VEGETABLE' WHERE recipe_id=? AND name='당근'", recipeId);
		jdbcTemplate.execute("ALTER TABLE cart_items ADD CONSTRAINT test_shared_save_failure CHECK (name <> '양파')");
		try {
			assertThatThrownBy(() -> saves.save(reader, postId)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
			assertThat(count("saved_recipes")).isZero();
			assertThat(count("cart_items")).isZero();
			assertThat(events("recipe_saved")).isZero();
			assertThat(events("cart_item_added")).isZero();
		} finally {
			jdbcTemplate.execute("ALTER TABLE cart_items DROP CONSTRAINT test_shared_save_failure");
		}
	}

	@Test
	void 사전미등록이고_확정카테고리도없는_부족재료는400이고_저장도롤백한다() throws Exception {
		jdbcTemplate.update("UPDATE recipe_ingredients SET name='사전에없는재료' WHERE recipe_id=? AND name='양파'", recipeId);
		save().andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		assertThat(count("saved_recipes")).isZero();
		assertThat(count("cart_items")).isZero();
		assertThat(events("recipe_saved")).isZero();
		assertThat(events("cart_item_added")).isZero();
	}

	@Test
	void 사전미등록도_확정카테고리로담고_반복과재저장은_수정량과이벤트를보존한다() throws Exception {
		jdbcTemplate.update("UPDATE recipe_ingredients SET name='사전에없는채소', category_code='VEGETABLE' WHERE recipe_id=? AND name='양파'", recipeId);
		save().andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true))
				.andExpect(jsonPath("$.saveCount").value(1)).andExpect(jsonPath("$.cartAddedCount").value(2));
		Long cartId = jdbcTemplate.queryForObject("SELECT id FROM cart_items WHERE user_id=? AND name='사전에없는채소'", Long.class, reader);
		assertThat(jdbcTemplate.queryForObject("SELECT c.code FROM cart_items i JOIN ingredient_categories c ON c.id=i.category_id WHERE i.id=?", String.class, cartId)).isEqualTo("VEGETABLE");
		assertThat(jdbcTemplate.queryForObject("SELECT recipe_id FROM saved_recipes WHERE user_id=?", Long.class, reader)).isEqualTo(recipeId);
		assertThat(count("recipes")).isEqualTo(1);
		jdbcTemplate.update("UPDATE cart_items SET amount=7 WHERE id=?", cartId);
		save().andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(0));
		assertThat(events("recipe_saved")).isEqualTo(1);
		assertThat(events("cart_item_added")).isEqualTo(1);
		unsave().andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false));
		assertThat(count("cart_items")).isEqualTo(2);
		jdbcTemplate.update("DELETE FROM cart_items WHERE user_id=? AND name='당근'", reader);
		save().andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(1));
		assertThat(jdbcTemplate.queryForObject("SELECT amount FROM cart_items WHERE id=?", BigDecimal.class, cartId)).isEqualByComparingTo("7");
		assertThat(count("cart_items")).isEqualTo(2);
		assertThat(events("recipe_saved")).isEqualTo(2);
		assertThat(events("cart_item_added")).isEqualTo(2);
		assertThat(jdbcTemplate.queryForList("SELECT properties->>'cart_added_count' FROM events WHERE user_id=? AND event_name='recipe_saved'", String.class, reader)).containsExactlyInAnyOrder("2", "1");
		assertThat(jdbcTemplate.queryForList("SELECT properties->>'item_count' FROM events WHERE user_id=? AND event_name='cart_item_added'", String.class, reader)).containsExactlyInAnyOrder("2", "1");
	}

	@Test
	void 사전카테고리는_레시피카테고리보다_우선한다() throws Exception {
		jdbcTemplate.update("UPDATE recipe_ingredients SET category_code='MEAT' WHERE recipe_id=? AND name='양파'", recipeId);
		save().andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(2));
		assertThat(jdbcTemplate.queryForObject("SELECT c.code FROM cart_items i JOIN ingredient_categories c ON c.id=i.category_id WHERE i.user_id=? AND i.name='양파'", String.class, reader)).isEqualTo("VEGETABLE");
	}

	@Test
	void 수량없는재료와_PINCH와_TO_TASTE는_사전과카테고리가없어도_제외한다() throws Exception {
		jdbcTemplate.update("UPDATE recipe_ingredients SET name='사전미등록수량없음' WHERE recipe_id=? AND name='소금'", recipeId);
		jdbcTemplate.update("UPDATE recipe_ingredients SET name='사전미등록꼬집' WHERE recipe_id=? AND name='후추'", recipeId);
		jdbcTemplate.update("INSERT INTO recipe_ingredients(recipe_id, name, amount, unit, sort_order, created_at) VALUES (?, '사전미등록적당량', 1, 'TO_TASTE', 5, ?)", recipeId, LocalDateTime.now(clock));
		save().andExpect(status().isOk()).andExpect(jsonPath("$.cartAddedCount").value(2));
		assertThat(jdbcTemplate.queryForList("SELECT name FROM cart_items", String.class)).containsExactlyInAnyOrder("양파", "당근");
	}

	@Test
	void 동시저장도_저장과_담기와_이벤트가_한번만반영된다() throws Exception {
		jdbcTemplate.update("UPDATE recipe_ingredients SET name='사전에없는채소', category_code='VEGETABLE' WHERE recipe_id=? AND name='양파'", recipeId);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			var first = executor.submit(() -> { start.await(); return saves.save(reader, postId); });
			var second = executor.submit(() -> { start.await(); return saves.save(reader, postId); });
			start.countDown();
			var a = first.get(15, TimeUnit.SECONDS);
			var b = second.get(15, TimeUnit.SECONDS);
			assertThat(List.of(a.cartAddedCount(), b.cartAddedCount())).containsExactlyInAnyOrder(2, 0);
			assertThat(a.saveCount()).isEqualTo(1);
			assertThat(b.saveCount()).isEqualTo(1);
		}
		assertThat(count("saved_recipes")).isEqualTo(1);
		assertThat(count("cart_items")).isEqualTo(2);
		assertThat(events("recipe_saved")).isEqualTo(1);
		assertThat(events("cart_item_added")).isEqualTo(1);
	}
}
