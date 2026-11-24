package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.jayway.jsonpath.JsonPath;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.share.application.port.in.PublishSharedRecipeUseCase;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;
import com.remake.support.IntegrationTestSupport;

class SharedRecipeDetailApiTest extends IntegrationTestSupport {
	@Autowired private PublishSharedRecipeUseCase publish;
	@Autowired private SaveRecipePort recipes;
	private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1};

	private Recipe original(Long author) {
		Long onion = createIngredient(author, "VEGETABLE", "양파", LocalDate.of(2026, 3, 10));
		return transactionTemplate.execute(tx -> recipes.save(Recipe.create(author, RecipeSource.QUICK,
				"원본 제목", 2, 15, "조리 팁", null,
				List.of(new RecipeIngredient(onion, "양파", new BigDecimal("2"), RecipeUnit.EA),
						new RecipeIngredient(null, "소금", null, RecipeUnit.TO_TASTE)),
				List.of("양파를 썬다", "익힌다"))));
	}
	private Long post(Long author, Long recipe) {
		return publish.publish(author, recipe, "공유 제목", new SharedRecipePhotoUpload(
				"image/jpeg", JPEG.length, () -> new ByteArrayInputStream(JPEG)));
	}
	private String url(Long id) { return "/api/v1/user/shared-recipes/" + id; }
	private void save(Long user, Long recipe, Long shared) {
		jdbcTemplate.update("INSERT INTO saved_recipes(user_id, recipe_id, shared_recipe_id, saved_at) VALUES (?, ?, ?, ?)",
				user, recipe, shared, LocalDateTime.now(clock));
	}

	@Test
	void 상세는_원본내용과_요청자기준_보유여부만_반환하고_작성자정보를_노출하지않는다() throws Exception {
		Long author = createUser(); Recipe recipe = original(author); Long id = post(author, recipe.id());
		Long reader = createUser();
		createIngredient(reader, "VEGETABLE", "양파", LocalDate.of(2026, 3, 10));
		String body = mockMvc.perform(get(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.title").value("공유 제목"))
				.andExpect(jsonPath("$.imageUrl").value("/api/v1/user/shared-recipe-photos/" + id))
				.andExpect(jsonPath("$.servings").value(2)).andExpect(jsonPath("$.cookMinutes").value(15))
				.andExpect(jsonPath("$.tips").value("조리 팁"))
				.andExpect(jsonPath("$.ingredients[0].name").value("양파"))
				.andExpect(jsonPath("$.ingredients[0].amount").value(2))
				.andExpect(jsonPath("$.ingredients[0].unit").value("EA"))
				.andExpect(jsonPath("$.ingredients[0].have").value(true))
				.andExpect(jsonPath("$.ingredients[1].have").value(false))
				.andExpect(jsonPath("$.steps[0].stepNo").value(1))
				.andExpect(jsonPath("$.steps[1].content").value("익힌다"))
				.andExpect(jsonPath("$.saved").value(false)).andExpect(jsonPath("$.saveCount").value(0))
				.andReturn().getResponse().getContentAsString();
		Map<?, ?> fields = JsonPath.parse(body).read("$", Map.class);
		assertThat(fields.keySet().stream().map(Object::toString).toList()).containsExactlyInAnyOrder("id", "title", "imageUrl", "servings", "cookMinutes",
				"tips", "ingredients", "steps", "saveCount", "saved");
		assertThat(body).doesNotContain("userId", "recipeId", "ingredientId", "imageKey", "author", "email", "customOptions");
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipes", Long.class)).isEqualTo(1);
	}

	@Test
	void 작성자의_재료나_부분일치_이름은_다른회원의_보유로_세지않는다() throws Exception {
		Long author = createUser(); Recipe recipe = original(author); Long id = post(author, recipe.id());
		Long reader = createUser();
		createIngredient(reader, "VEGETABLE", "다진 양파", LocalDate.of(2026, 3, 10));
		mockMvc.perform(get(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk()).andExpect(jsonPath("$.ingredients[0].have").value(false))
				.andExpect(jsonPath("$.ingredients[1].have").value(false));
	}

	@Test
	void 저장여부는_원본기준이고_저장수는_이_공유글에서_저장한_현재행수다() throws Exception {
		Long author = createUser(); Recipe recipe = original(author); Long id = post(author, recipe.id());
		Long reader = createUser(); Long other = createUser();
		save(reader, recipe.id(), id); save(other, recipe.id(), id);
		mockMvc.perform(get(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true))
				.andExpect(jsonPath("$.saveCount").value(2));
		jdbcTemplate.update("DELETE FROM saved_recipes WHERE user_id = ?", other);
		mockMvc.perform(get(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, other))
				.andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(false))
				.andExpect(jsonPath("$.saveCount").value(1));
		jdbcTemplate.update("UPDATE saved_recipes SET shared_recipe_id = NULL WHERE user_id = ?", reader);
		mockMvc.perform(get(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true))
				.andExpect(jsonPath("$.saveCount").value(0));
	}

	@Test
	void 숨긴글과_없는글은_404이고_비로그인은_401이다() throws Exception {
		Long author = createUser(); Long id = post(author, original(author).id());
		mockMvc.perform(get(url(id))).andExpect(status().isUnauthorized());
		jdbcTemplate.update("UPDATE shared_recipes SET status = 'HIDDEN' WHERE id = ?", id);
		for (Long target : new Long[]{id, 999999L}) {
			mockMvc.perform(get(url(target)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, author))
					.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHARED_RECIPE_NOT_FOUND"));
		}
	}

	@Test
	void 작성자_탈퇴후에도_익명조리내용은_유지한다() throws Exception {
		Long author = createUser(); Recipe recipe = original(author); Long id = post(author, recipe.id());
		jdbcTemplate.update("UPDATE users SET email = NULL, deleted_at = ? WHERE id = ?", LocalDateTime.now(clock), author);
		mockMvc.perform(get(url(id)).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, createUser()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.steps[0].content").value("양파를 썬다"));
	}
}
