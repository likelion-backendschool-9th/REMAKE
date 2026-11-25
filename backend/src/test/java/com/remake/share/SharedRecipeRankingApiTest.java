package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.share.domain.SharedRecipe;
import com.remake.support.IntegrationTestSupport;
import com.remake.support.MutableClock;

class SharedRecipeRankingApiTest extends IntegrationTestSupport {
	private static final String ROOT = "/api/v1/user/shared-recipes";
	private Long author;
	private Long reader;
	private LocalDateTime now;
	private LocalDateTime monday;

	@BeforeEach
	void setUp() {
		author = createUser();
		reader = createUser();
		now = LocalDateTime.now(clock);
		monday = SharedRecipe.weekStart(clock);
	}

	@Test
	void 월요일부터_현재까지_유지중인_저장만_세고_누적수와_본인저장은_제외한다() throws Exception {
		Long old = post("지난주 인기", monday.minusWeeks(2));
		Long current = post("이번주 인기", monday.minusWeeks(1));
		for (int i = 0; i < 4; i++) {
			save(old, createUser(), monday.minusSeconds(1));
		}
		save(old, author, now);
		save(old, createUser(), now.plusSeconds(1));
		save(old, createUser(), monday.plusWeeks(1));
		save(current, reader, monday);
		request(get(ROOT + "/ranking"))
				.andExpect(jsonPath("$.content[0].id").value(current))
				.andExpect(jsonPath("$.content[1].id").value(old));
		request(get(ROOT + "/weekly-top"))
				.andExpect(jsonPath("$.items[0].id").value(current));
	}

	@Test
	void 실제_저장취소와_재저장이_순위에_반영된다() throws Exception {
		Long first = post("먼저 공유", monday);
		Long second = post("나중 공유", monday.plusHours(1));
		save(first, reader, now);
		save(second, reader, now);
		request(get(ROOT + "/ranking")).andExpect(jsonPath("$.content[0].id").value(first));
		request(delete(ROOT + "/" + first + "/save")).andExpect(jsonPath("$.saved").value(false));
		request(get(ROOT + "/ranking")).andExpect(jsonPath("$.content[0].id").value(second));
		request(put(ROOT + "/" + first + "/save")).andExpect(jsonPath("$.saved").value(true));
		request(get(ROOT + "/ranking")).andExpect(jsonPath("$.content[0].id").value(first));
	}

	@Test
	void 지난주_대비_상승과_하락을_반환하고_동일순위와_기록없음은_null이다() throws Exception {
		Long up = post("상승", monday);
		Long down = post("하락", monday.plusHours(1));
		Long same = post("동일", monday.plusHours(2));
		Long fresh = post("신규", monday.plusHours(3));
		save(up, reader, now);
		save(up, createUser(), now);
		save(down, reader, now);
		previous(up, 3, monday.minusWeeks(1));
		previous(down, 1, monday.minusWeeks(1));
		previous(same, 3, monday.minusWeeks(1));
		previous(fresh, 1, monday.minusWeeks(2));
		request(get(ROOT + "/ranking"))
				.andExpect(jsonPath("$.content[0].rank").value(1))
				.andExpect(jsonPath("$.content[0].rankChange").value(2))
				.andExpect(jsonPath("$.content[1].rankChange").value(-1))
				.andExpect(jsonPath("$.content[2].rankChange").value(nullValue()))
				.andExpect(jsonPath("$.content[3].rankChange").value(nullValue()));
		request(get(ROOT + "/weekly-top"))
				.andExpect(jsonPath("$.items.length()").value(3))
				.andExpect(jsonPath("$.items[0].rankChange").doesNotExist())
				.andExpect(jsonPath("$.items[0].sharedAt").doesNotExist())
				.andExpect(jsonPath("$.items[0].saveCount").doesNotExist())
				.andExpect(jsonPath("$.items[0].userId").doesNotExist());
	}

	@Test
	void 최신순은_저장수와_무관한_공유시각순이고_순위와변동이_null이다() throws Exception {
		Long old = post("오래된 글", monday.minusWeeks(1));
		Long newest = post("최신 글", now);
		Long sameTime = post("같은 시각", now);
		save(old, reader, now);
		previous(newest, 1, monday.minusWeeks(1));
		request(get(ROOT + "/ranking").param("sort", "latest"))
				.andExpect(jsonPath("$.content[0].id").value(sameTime))
				.andExpect(jsonPath("$.content[1].id").value(newest))
				.andExpect(jsonPath("$.content[2].id").value(old))
				.andExpect(jsonPath("$.content[0].rank").value(nullValue()))
				.andExpect(jsonPath("$.content[1].rankChange").value(nullValue()))
				.andExpect(jsonPath("$.content[0].sharedAt").value("2026-03-04T10:00:00+09:00"));
	}

	@Test
	void 검색과_페이지를_적용해도_전체순위를_유지하고_제목만_검색한다() throws Exception {
		post("다른 제목", monday);
		Long first = post("Chicken 하나", monday.plusHours(1));
		Long second = post("CHICKEN 둘", monday.plusHours(2));
		request(get(ROOT + "/ranking").param("q", " chicken ").param("size", "1"))
				.andExpect(jsonPath("$.content[0].id").value(first))
				.andExpect(jsonPath("$.content[0].rank").value(2))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.hasNext").value(true));
		request(get(ROOT + "/ranking").param("q", "chicken").param("size", "1").param("page", "1"))
				.andExpect(jsonPath("$.content[0].id").value(second))
				.andExpect(jsonPath("$.content[0].rank").value(3))
				.andExpect(jsonPath("$.hasNext").value(false));
		request(get(ROOT + "/ranking").param("q", "chicken").param("page", "2"))
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.totalElements").value(2));
		request(get(ROOT + "/ranking").param("sort", "latest").param("q", "chicken"))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].id").value(second));
	}

	@Test
	void 인기순은_TOP100을_먼저_정하고_최신순은_100위밖의_글도_검색한다() throws Exception {
		for (int i = 0; i < 100; i++) {
			post("공유 " + i, monday.plusSeconds(i));
		}
		Long last = post("100위 밖", monday.plusHours(1));
		request(get(ROOT + "/ranking").param("size", "100"))
				.andExpect(jsonPath("$.totalElements").value(100))
				.andExpect(jsonPath("$.content.length()").value(100))
				.andExpect(jsonPath("$.content[99].rank").value(100))
				.andExpect(jsonPath("$.hasNext").value(false));
		request(get(ROOT + "/ranking").param("q", "100위 밖"))
				.andExpect(jsonPath("$.totalElements").value(0));
		request(get(ROOT + "/ranking").param("sort", "latest").param("q", "100위 밖"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(last));
	}

	@Test
	void 검색어의_와일드카드와_SQL문자는_문자그대로_검색하고_이벤트를_발행하지않는다() throws Exception {
		Long special = post("100%_양파's", monday);
		post("다른 글", now);
		request(get(ROOT + "/ranking").param("q", "%_"))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(special));
		request(get(ROOT + "/ranking").param("q", "' OR 1=1 --"))
				.andExpect(jsonPath("$.content").isEmpty());
		request(get(ROOT + "/ranking").param("q", " "))
				.andExpect(jsonPath("$.totalElements").value(2));
		request(get(ROOT + "/ranking").param("q", "가".repeat(10000)))
				.andExpect(jsonPath("$.content").isEmpty());
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events", Long.class)).isZero();
	}

	@Test
	void 같은원본의_다른주_공유글과_내저장은_해당게시글의_저장수에_합산하지않는다() throws Exception {
		Long first = post("지난주 공유글", monday.minusWeeks(1));
		Long recipeId = jdbcTemplate.queryForObject("SELECT recipe_id FROM shared_recipes WHERE id=?", Long.class, first);
		Long second = jdbcTemplate.queryForObject("""
				INSERT INTO shared_recipes (recipe_id, title, image_key, status, created_at, updated_at)
				VALUES (?, '이번주 공유글', 'ranking-test.jpg', 'VISIBLE', ?, ?) RETURNING id
				""", Long.class, recipeId, monday, monday);
		save(second, reader, now);
		jdbcTemplate.update("INSERT INTO saved_recipes (user_id, recipe_id, saved_at) VALUES (?, ?, ?)",
				createUser(), recipeId, now);
		request(get(ROOT + "/ranking"))
				.andExpect(jsonPath("$.content[0].id").value(second))
				.andExpect(jsonPath("$.content[1].id").value(first));
	}

	@Test
	void 숨김글은_어떤_목록에도_노출하지않고_검색_0건과_빈목록은_정상응답이다() throws Exception {
		Long hidden = post("숨김", monday);
		save(hidden, reader, now);
		jdbcTemplate.update("UPDATE shared_recipes SET status='HIDDEN' WHERE id=?", hidden);
		request(get(ROOT + "/weekly-top")).andExpect(jsonPath("$.items").isEmpty());
		for (String sort : List.of("popular", "latest")) {
			request(get(ROOT + "/ranking").param("sort", sort))
					.andExpect(jsonPath("$.content").isEmpty())
					.andExpect(jsonPath("$.totalElements").value(0))
					.andExpect(jsonPath("$.hasNext").value(false));
		}
		post("공개", monday);
		request(get(ROOT + "/ranking").param("q", "없는 제목"))
				.andExpect(jsonPath("$.content").isEmpty());
	}

	@Test
	void TOP3가_3개미만이어도_있는만큼_반환하며_물과_꼬집과_약간도_요약에_포함한다() throws Exception {
		Long post = post("간단 요리", monday, "물", "후추", "소금");
		request(get(ROOT + "/weekly-top"))
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.items[0].id").value(post))
				.andExpect(jsonPath("$.items[0].ingredientSummary").value("물 · 후추 · 소금"))
				.andExpect(jsonPath("$.items[0].extraIngredientCount").value(0))
				.andExpect(jsonPath("$.items[0].imageUrl").value("/api/v1/user/shared-recipe-photos/" + post));
	}

	@Test
	void 모든목록이_원본순서의_앞3개와_나머지개수를_같이반환하며_재료0개도_허용한다() throws Exception {
		post("다섯 재료", monday, "닭가슴살", "청상추", "계란", "물", "소금");
		post("재료 없음", monday.plusHours(1));
		request(get(ROOT + "/weekly-top"))
				.andExpect(jsonPath("$.items[0].ingredientSummary").value("닭가슴살 · 청상추 · 계란"))
				.andExpect(jsonPath("$.items[0].extraIngredientCount").value(2))
				.andExpect(jsonPath("$.items[1].ingredientSummary").value(""))
				.andExpect(jsonPath("$.items[1].extraIngredientCount").value(0));
		for (String sort : List.of("popular", "latest")) {
			request(get(ROOT + "/ranking").param("sort", sort).param("q", "다섯 재료"))
					.andExpect(jsonPath("$.content[0].ingredientSummary").value("닭가슴살 · 청상추 · 계란"))
					.andExpect(jsonPath("$.content[0].extraIngredientCount").value(2));
		}
	}

	@Test
	void KST_월요일0시에_집계주가_바뀌고_지난주확정순위와_비교한다() throws Exception {
		clock.setNow(ZonedDateTime.of(2026, 3, 1, 23, 59, 59, 0, MutableClock.ZONE));
		Long old = post("이전 주", LocalDateTime.now(clock).minusDays(1));
		Long fresh = post("새 주", LocalDateTime.now(clock));
		save(old, reader, LocalDateTime.now(clock));
		previous(fresh, 2, LocalDateTime.of(2026, 2, 23, 0, 0));
		request(get(ROOT + "/ranking")).andExpect(jsonPath("$.content[0].id").value(old))
				.andExpect(jsonPath("$.weekLabel").value("2월 넷째 주"));
		clock.setNow(ZonedDateTime.of(2026, 3, 2, 0, 0, 0, 0, MutableClock.ZONE));
		save(fresh, reader, LocalDateTime.now(clock));
		request(get(ROOT + "/ranking"))
				.andExpect(jsonPath("$.weekLabel").value("3월 첫째 주"))
				.andExpect(jsonPath("$.content[0].id").value(fresh))
				.andExpect(jsonPath("$.content[0].rankChange").value(1));
	}

	@Test
	void 잘못된_정렬과_페이지는_INVALID_INPUT이고_로그인이_필요하다() throws Exception {
		for (String[] invalid : List.of(new String[]{"sort", "POPULAR"}, new String[]{"sort", "other"},
				new String[]{"page", "-1"}, new String[]{"page", "abc"}, new String[]{"size", "0"},
				new String[]{"size", "101"}, new String[]{"size", "2147483648"})) {
			mockMvc.perform(get(ROOT + "/ranking").param(invalid[0], invalid[1])
					.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		}
		request(get(ROOT + "/ranking").param("page", "2147483647").param("size", "100"))
				.andExpect(jsonPath("$.content").isEmpty());
		mockMvc.perform(get(ROOT + "/ranking")).andExpect(status().isUnauthorized());
		mockMvc.perform(get(ROOT + "/weekly-top")).andExpect(status().isUnauthorized());
	}

	private ResultActions request(MockHttpServletRequestBuilder request) throws Exception {
		return mockMvc.perform(request.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk());
	}

	private Long post(String title, LocalDateTime sharedAt, String... names) {
		Long recipeId = createRecipe(author);
		for (int i = 0; i < names.length; i++) {
			// INSERT 순서도 뒤집어 sort_order가 원본 순서를 결정하는지 검증한다.
			int order = names.length - 1 - i;
			jdbcTemplate.update("""
					INSERT INTO recipe_ingredients (recipe_id, name, amount, unit, sort_order, created_at)
					VALUES (?, ?, NULL, ?, ?, ?)
					""", recipeId, names[order], switch (names[order]) {
					case "물" -> "ML";
					case "후추" -> "PINCH";
					case "소금" -> "TO_TASTE";
					default -> "EA";
				}, order, now);
		}
		return jdbcTemplate.queryForObject("""
				INSERT INTO shared_recipes (recipe_id, title, image_key, status, created_at, updated_at)
				VALUES (?, ?, 'ranking-test.jpg', 'VISIBLE', ?, ?) RETURNING id
				""", Long.class, recipeId, title, sharedAt, sharedAt);
	}

	private void save(Long postId, Long userId, LocalDateTime savedAt) {
		jdbcTemplate.update("""
				INSERT INTO saved_recipes (user_id, recipe_id, shared_recipe_id, saved_at)
				SELECT ?, recipe_id, id, ? FROM shared_recipes WHERE id=?
				""", userId, savedAt, postId);
	}

	private void previous(Long postId, int rank, LocalDateTime weekStart) {
		jdbcTemplate.update("""
				INSERT INTO shared_recipe_weekly_ranks (week_start, shared_recipe_id, rank_no, save_count, created_at)
				VALUES (?, ?, ?, 1, ?)
				""", weekStart.toLocalDate(), postId, rank, now);
	}
}
