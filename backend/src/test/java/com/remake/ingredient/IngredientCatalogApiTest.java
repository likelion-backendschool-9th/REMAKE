package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.ingredient.application.port.in.GetIngredientCatalogUseCase;
import com.remake.ingredient.domain.IngredientCatalogItem;
import com.remake.support.IntegrationTestSupport;

// 재료 사전: Flyway 기준 데이터(V5)와 검색 API, 다른 기능이 쓰는 UseCase를 확인한다.
class IngredientCatalogApiTest extends IntegrationTestSupport {

	private static final String URL = "/api/v1/user/ingredient-catalog";

	@Autowired
	private GetIngredientCatalogUseCase getIngredientCatalogUseCase;

	private MockHttpServletRequestBuilder asUser(MockHttpServletRequestBuilder builder) {
		return builder.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, 1L);
	}

	@Test
	void 로그인_정보가_없으면_401이다() throws Exception {
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
	}

	@Test
	void 기준_데이터는_카테고리_12종_모두에_항목이_있고_이름이_겹치지_않는다() {
		Integer categories = jdbcTemplate.queryForObject("SELECT count(DISTINCT category_id) FROM ingredient_catalog",
				Integer.class);
		Integer total = jdbcTemplate.queryForObject("SELECT count(*) FROM ingredient_catalog", Integer.class);
		Integer distinct = jdbcTemplate.queryForObject("SELECT count(DISTINCT name) FROM ingredient_catalog",
				Integer.class);

		assertThat(categories).isEqualTo(12);
		assertThat(total).isEqualTo(distinct).isGreaterThan(100);
	}

	@Test
	void 대표_알레르기_재료가_들어_있다() {
		List<String> common = jdbcTemplate.queryForList(
				"SELECT name FROM ingredient_catalog WHERE is_common_exclusion ORDER BY name", String.class);

		assertThat(common).contains("달걀", "우유", "땅콩", "대두", "밀", "메밀", "갑각류", "새우", "게", "고등어", "돼지고기",
				"복숭아", "토마토", "호두");
	}

	@Test
	void 같은_재료의_다른_표기는_대표_알레르기를_하나에만_표시한다() throws Exception {
		List<String> common = jdbcTemplate.queryForList(
				"SELECT name FROM ingredient_catalog WHERE is_common_exclusion", String.class);

		assertThat(common).contains("달걀", "쇠고기").doesNotContain("계란", "소고기");
		// 다른 표기로 검색해도 항목은 나온다
		mockMvc.perform(asUser(get(URL).param("q", "소고기")))
				.andExpect(jsonPath("$[0].name").value("소고기"))
				.andExpect(jsonPath("$[0].isCommonExclusion").value(false));
		mockMvc.perform(asUser(get(URL).param("q", "계란")))
				.andExpect(jsonPath("$[0].name").value("계란"));
	}

	@Test
	void 이름에_검색어가_들어간_항목을_카테고리와_보관_일수와_함께_돌려준다() throws Exception {
		mockMvc.perform(asUser(get(URL).param("q", "우유")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.name == '우유')].categoryCode", contains("DAIRY_EGG")))
				.andExpect(jsonPath("$[?(@.name == '우유')].isCommonExclusion", contains(true)))
				// 항목 값이 있으면 그 값(우유 7일)
				.andExpect(jsonPath("$[?(@.name == '우유')].shelfLifeDays", contains(7)))
				.andExpect(jsonPath("$[*].name", hasItem("저지방우유")));
	}

	@Test
	void 항목에_보관_일수가_없으면_카테고리_기본값을_채운다() throws Exception {
		Integer nullCount = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM ingredient_catalog WHERE shelf_life_days IS NULL", Integer.class);
		assertThat(nullCount).isPositive();

		mockMvc.perform(asUser(get(URL).param("q", "오이")))
				.andExpect(jsonPath("$[?(@.name == '오이')].shelfLifeDays", contains(7)));
	}

	@Test
	void 검색어는_대소문자를_가리지_않고_앞부분이_맞는_이름이_먼저다() throws Exception {
		mockMvc.perform(asUser(get(URL).param("q", "버섯")))
				.andExpect(jsonPath("$[0].name").value("버섯"));
		mockMvc.perform(asUser(get(URL).param("q", "  양파 ")))
				.andExpect(jsonPath("$[0].name").value("양파"));
	}

	@Test
	void 검색어가_없으면_최대_50개를_돌려준다() throws Exception {
		mockMvc.perform(asUser(get(URL)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(50)));
	}

	@Test
	void commonExclusion이면_대표_알레르기_재료가_맨_위에_온다() throws Exception {
		int commonCount = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM ingredient_catalog WHERE is_common_exclusion", Integer.class);

		mockMvc.perform(asUser(get(URL).param("commonExclusion", "true")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0:" + commonCount + "][*].isCommonExclusion", everyItem(org.hamcrest.Matchers.is(true))))
				.andExpect(jsonPath("$[" + commonCount + "].isCommonExclusion").value(false));
	}

	@Test
	void 일치하는_항목이_없으면_빈_목록이다() throws Exception {
		mockMvc.perform(asUser(get(URL).param("q", "없는재료이름")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void 퍼센트와_밑줄은_글자_그대로_찾는다() throws Exception {
		mockMvc.perform(asUser(get(URL).param("q", "%")))
				.andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(asUser(get(URL).param("q", "_")))
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void 검색어가_51자_이상이면_INVALID_INPUT이다() throws Exception {
		mockMvc.perform(asUser(get(URL).param("q", "가".repeat(51))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	@Test
	void 다른_기능이_쓰는_UseCase는_id와_이름으로_읽는다() {
		IngredientCatalogItem egg = getIngredientCatalogUseCase.findCatalogItemByName(" 달걀 ").orElseThrow();

		assertThat(egg.commonExclusion()).isTrue();
		assertThat(getIngredientCatalogUseCase.getCatalogItemsByIds(List.of(egg.id(), -1L)))
				.extracting(IngredientCatalogItem::name).containsExactly("달걀");
		assertThat(getIngredientCatalogUseCase.getCatalogItemsByIds(List.of())).isEmpty();
		assertThat(getIngredientCatalogUseCase.findCatalogItemByName("없는재료이름")).isEmpty();
		assertThat(getIngredientCatalogUseCase.findCatalogItemByName("  ")).isEmpty();
	}
}
