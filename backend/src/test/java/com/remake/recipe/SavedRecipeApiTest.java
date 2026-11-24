package com.remake.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.recipe.application.port.in.GetRecipeListItemsUseCase;
import com.remake.recipe.application.port.in.RecipeListItem;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.support.IntegrationTestSupport;

// API 33~37 저장 목록·저장·저장 취소·즐겨찾기와 상세(30)의 saved·favorite·shareable. 기본 시각 2026-03-04 10:00(서울)
class SavedRecipeApiTest extends IntegrationTestSupport {

	private static final String SAVED_URL = "/api/v1/user/saved-recipes";
	private static final String RECIPE_URL = "/api/v1/user/recipes/";
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Autowired
	private SaveRecipePort saveRecipePort;
	@Autowired
	private GetRecipeListItemsUseCase getRecipeListItemsUseCase;

	private Long userId;

	@BeforeEach
	void setUp() {
		userId = createUser();
	}

	private MockHttpServletRequestBuilder as(Long id, MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, id);
	}

	/** 재료는 이름만 다르고 양은 1개. "물"은 ML, 나머지는 EA */
	private Long recipe(Long owner, String title, String... ingredientNames) {
		List<RecipeIngredient> ingredients = Arrays.stream(ingredientNames)
				.map(name -> new RecipeIngredient(null, name, BigDecimal.ONE, "물".equals(name) ? RecipeUnit.ML
						: RecipeUnit.EA))
				.toList();
		return transactionTemplate.execute(status -> saveRecipePort.save(Recipe.create(owner, RecipeSource.QUICK,
				title, 1, 10, null, null, ingredients, List.of("만든다.")))).id();
	}

	private ResultActions save(Long recipeId) throws Exception {
		return mockMvc.perform(as(userId, put(SAVED_URL + "/" + recipeId)));
	}

	private ResultActions list(String query) throws Exception {
		return mockMvc.perform(as(userId, get(SAVED_URL + query)));
	}

	/** 공유 영역의 공유 레시피 저장(API 53)처럼 남의 레시피를 내 저장 목록에 넣는다 */
	private void saveSharedDirectly(Long recipeId) {
		jdbcTemplate.update("INSERT INTO saved_recipes (user_id, recipe_id, is_favorite, saved_at) VALUES (?, ?, false, ?)",
				userId, recipeId, LocalDateTime.now(clock));
	}

	private int savedRows() {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM saved_recipes WHERE user_id = ?", Integer.class,
				userId);
	}

	@Test
	void 저장은_다시_보내도_같은_결과이고_처음_저장에만_이벤트를_남긴다() throws Exception {
		Long recipeId = recipe(userId, "계란말이", "계란");

		save(recipeId).andExpect(status().isOk())
				.andExpect(jsonPath("$.saved").value(true))
				.andExpect(jsonPath("$.cartAddedCount").value(0));
		save(recipeId).andExpect(status().isOk())
				.andExpect(jsonPath("$.saved").value(true));

		assertThat(savedRows()).isEqualTo(1);
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForList(
				"SELECT properties::text FROM events WHERE event_name = 'recipe_saved' AND user_id = ?", String.class,
				userId)).singleElement().asString()
				.contains("\"recipe_id\": " + recipeId, "\"save_source\": \"own\"", "\"cart_added_count\": 0");
	}

	@Test
	void 남의_레시피나_없는_레시피는_저장할_수_없다() throws Exception {
		Long others = recipe(createUser(), "남의 레시피", "계란");

		save(others).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
		save(999999L).andExpect(status().isNotFound());
		assertThat(savedRows()).isZero();
	}

	@Test
	void 즐겨찾기와_저장_취소는_저장한_레시피만_되고_상세에_반영된다() throws Exception {
		Long recipeId = recipe(userId, "계란말이", "계란");
		Long others = recipe(createUser(), "남의 레시피", "계란");

		mockMvc.perform(as(userId, put(SAVED_URL + "/" + recipeId + "/favorite")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SAVED_RECIPE_NOT_FOUND"));
		mockMvc.perform(as(userId, delete(SAVED_URL + "/" + recipeId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SAVED_RECIPE_NOT_FOUND"));
		mockMvc.perform(as(userId, put(SAVED_URL + "/" + others + "/favorite")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));

		save(recipeId);
		mockMvc.perform(as(userId, put(SAVED_URL + "/" + recipeId + "/favorite")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.favorite").value(true));
		mockMvc.perform(as(userId, get(RECIPE_URL + recipeId)))
				.andExpect(jsonPath("$.saved").value(true))
				.andExpect(jsonPath("$.favorite").value(true));

		mockMvc.perform(as(userId, delete(SAVED_URL + "/" + recipeId + "/favorite")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.favorite").value(false));
		mockMvc.perform(as(userId, get(RECIPE_URL + recipeId)))
				.andExpect(jsonPath("$.saved").value(true))
				.andExpect(jsonPath("$.favorite").value(false));

		mockMvc.perform(as(userId, delete(SAVED_URL + "/" + recipeId)))
				.andExpect(status().isNoContent());
		mockMvc.perform(as(userId, get(RECIPE_URL + recipeId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.saved").value(false));
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM recipes WHERE id = ?", Integer.class, recipeId))
				.as("저장 취소는 레시피 원본을 지우지 않는다").isEqualTo(1);
	}

	@Test
	void 저장_목록은_재료_요약과_정렬과_즐겨찾기_필터와_페이지를_준다() throws Exception {
		Long first = recipe(userId, "떡만두 미역국", "미역", "냉동 만두", "계란", "떡");
		Long second = recipe(userId, "계란말이", "계란");
		Long third = recipe(userId, "양파볶음", "양파", "간장");
		recipe(userId, "저장 안 한 레시피", "계란");
		save(first);
		clock.advance(Duration.ofMinutes(1));
		save(second);
		clock.advance(Duration.ofMinutes(1));
		save(third);
		mockMvc.perform(as(userId, put(SAVED_URL + "/" + first + "/favorite")));

		list("").andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").value(3))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.content.length()").value(3))
				.andExpect(jsonPath("$.content[0].recipeId").value(third))
				.andExpect(jsonPath("$.content[0].ingredientSummary").value("양파 · 간장"))
				.andExpect(jsonPath("$.content[0].extraIngredientCount").value(0))
				.andExpect(jsonPath("$.content[2].title").value("떡만두 미역국"))
				.andExpect(jsonPath("$.content[2].ingredientSummary").value("미역 · 냉동 만두 · 계란"))
				.andExpect(jsonPath("$.content[2].extraIngredientCount").value(1))
				.andExpect(jsonPath("$.content[2].favorite").value(true))
				.andExpect(jsonPath("$.hasNext").value(false));

		list("?sort=SAVED_ASC&size=2")
				.andExpect(jsonPath("$.content[0].recipeId").value(first))
				.andExpect(jsonPath("$.content[1].recipeId").value(second))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.hasNext").value(true));

		list("?filter=FAVORITE")
				.andExpect(jsonPath("$.totalCount").value(3))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].recipeId").value(first));
	}

	@Test
	void 바로_가능은_물을_뺀_모든_재료가_소비기한_안의_내_냉장고에_있는_레시피만이다() throws Exception {
		createIngredient(userId, "DAIRY_EGG", "계란", TODAY.plusDays(3));
		createIngredient(userId, "VEGETABLE", "양파", TODAY.minusDays(1));
		createIngredient(createUser(), "SEASONING", "소금", TODAY.plusDays(30));
		Long eggSoup = recipe(userId, "계란국", "계란", "물");
		Long saltyEgg = recipe(userId, "소금 계란", "계란", "소금");
		Long onion = recipe(userId, "양파볶음", "양파");
		for (Long id : List.of(eggSoup, saltyEgg, onion)) {
			save(id);
		}

		list("?filter=AVAILABLE").andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCount").value(3))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].recipeId").value(eggSoup));
	}

	@Test
	void 저장한_공유_레시피는_상세를_보고_저장을_취소할_수_있다() throws Exception {
		Long shared = recipe(createUser(), "남의 계란말이", "계란");
		mockMvc.perform(as(userId, get(RECIPE_URL + shared))).andExpect(status().isNotFound());

		saveSharedDirectly(shared);
		mockMvc.perform(as(userId, get(RECIPE_URL + shared)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("남의 계란말이"))
				.andExpect(jsonPath("$.saved").value(true))
				// 남에게서 저장한 레시피는 공유할 수 없다
				.andExpect(jsonPath("$.shareable").value(false));
		list("").andExpect(jsonPath("$.content[0].recipeId").value(shared));

		mockMvc.perform(as(userId, delete(SAVED_URL + "/" + shared))).andExpect(status().isNoContent());
		mockMvc.perform(as(userId, get(RECIPE_URL + shared))).andExpect(status().isNotFound());
	}

	@Test
	void 상세의_공유_가능은_이번_주에_공유했으면_false이고_다음_주_월요일에_다시_true다() throws Exception {
		Long recipeId = recipe(userId, "계란말이", "계란");
		mockMvc.perform(as(userId, get(RECIPE_URL + recipeId))).andExpect(jsonPath("$.shareable").value(true));

		// 수요일(3/4)에 공유
		LocalDateTime now = LocalDateTime.now(clock);
		jdbcTemplate.update("INSERT INTO shared_recipes (recipe_id, title, image_key, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
				recipeId, "공유 제목", "shared-recipes/test.jpg", now, now);
		mockMvc.perform(as(userId, get(RECIPE_URL + recipeId))).andExpect(jsonPath("$.shareable").value(false));

		// 일요일(3/8) 23:59까지는 같은 주
		clock.advance(Duration.ofDays(4).plusHours(13).plusMinutes(59));
		mockMvc.perform(as(userId, get(RECIPE_URL + recipeId))).andExpect(jsonPath("$.shareable").value(false));

		// 월요일(3/9) 0시부터 새 주
		clock.advance(Duration.ofMinutes(1));
		mockMvc.perform(as(userId, get(RECIPE_URL + recipeId))).andExpect(jsonPath("$.shareable").value(true));
	}

	@Test
	void 잘못된_필터나_페이지_크기는_INVALID_INPUT이다() throws Exception {
		list("?filter=NOPE").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		list("?size=0").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		list("?size=101").andExpect(status().isBadRequest());
		list("?page=-1").andExpect(status().isBadRequest());
	}

	@Test
	void 목록_한_줄_읽기는_내가_만들었거나_저장한_레시피만_돌려준다() throws Exception {
		Long mine = recipe(userId, "내 레시피", "계란", "양파", "대파", "마늘", "물");
		Long savedShared = recipe(createUser(), "저장한 남의 레시피", "두부");
		Long notVisible = recipe(createUser(), "남의 레시피", "두부");
		saveSharedDirectly(savedShared);
		jdbcTemplate.update("UPDATE saved_recipes SET is_favorite = true WHERE recipe_id = ?", savedShared);

		Map<Long, RecipeListItem> items = getRecipeListItemsUseCase.getListItems(userId,
				List.of(mine, savedShared, notVisible, 999999L));

		assertThat(items).containsOnlyKeys(mine, savedShared);
		assertThat(items.get(mine)).isEqualTo(new RecipeListItem(mine, "내 레시피", "계란 · 양파 · 대파", 2, false));
		assertThat(items.get(savedShared).favorite()).isTrue();
	}
}
