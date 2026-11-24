package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import com.remake.global.config.UploadProperties;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.support.IntegrationTestSupport;

class SharedRecipePublishingApiTest extends IntegrationTestSupport {
	private static final String URL = "/api/v1/user/shared-recipes";
	private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1};
	@Autowired
	private UploadProperties uploads;

	private MockMultipartHttpServletRequestBuilder request(Long recipeId, String title, byte[] photo) {
		var builder = multipart(URL).file(new MockMultipartFile("payload", "", "application/json",
				("{\"recipeId\":" + recipeId + ",\"title\":\"" + title + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8)));
		if (photo != null) {
			builder.file(new MockMultipartFile("image", "original.jpg", "image/jpeg", photo));
		}
		return builder;
	}
	private long fileCount() throws Exception {
		try (var files = Files.walk(Path.of(uploads.rootDirectory()))) {
			return files.filter(Files::isRegularFile).count();
		}
	}
	private int publish(Long userId, Long recipeId) throws Exception {
		return mockMvc.perform(request(recipeId, "공유 제목", JPEG)
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, userId)).andReturn().getResponse().getStatus();
	}

	@Test
	void 게시하면_원본과_사진키를_저장하고_개인정보없는_이벤트를_발행한다() throws Exception {
		Long user = createUser();
		Long recipe = createRecipe(user);
		mockMvc.perform(request(recipe, "  공유 제목  ", JPEG).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.id").isNumber());
		var row = jdbcTemplate.queryForMap("SELECT * FROM shared_recipes");
		assertThat(row.get("recipe_id")).isEqualTo(recipe);
		assertThat(row.get("title")).isEqualTo("공유 제목");
		assertThat(row.get("status")).isEqualTo("VISIBLE");
		assertThat(row.get("image_key").toString()).startsWith("shared-recipes/").endsWith(".jpg");
		assertThat(fileCount()).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recipes", Long.class)).isEqualTo(1);
		awaitEventWrites();
		String props = jdbcTemplate.queryForObject("SELECT properties::text FROM events WHERE event_name = 'shared_recipe_published'", String.class);
		assertThat(props).contains("shared_recipe_id").doesNotContain("공유 제목", "original.jpg", "photo_count");
	}

	@Test
	void 사진_누락과_빈사진은_PHOTO_REQUIRED다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user);
		for (byte[] photo : new byte[][] {null, new byte[0]}) {
			mockMvc.perform(request(recipe, "제목", photo).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PHOTO_REQUIRED"))
					.andExpect(jsonPath("$.message").value("실제로 요리한 사진을 올려 주세요"));
		}
		assertThat(fileCount()).isZero();
	}

	@Test
	void 제목_누락과_빈제목은_입력오류다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user);
		mockMvc.perform(request(recipe, "  ", JPEG).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		mockMvc.perform(multipart(URL).file(new MockMultipartFile("payload", "", "application/json",
				("{\"recipeId\":" + recipe + "}").getBytes())).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isBadRequest());
		assertThat(fileCount()).isZero();
	}

	@Test
	void 공유제목은_100자까지_저장한다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user);
		String title = "가".repeat(100);
		mockMvc.perform(request(recipe, title, JPEG).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isCreated());
		assertThat(jdbcTemplate.queryForObject("SELECT title FROM shared_recipes", String.class)).isEqualTo(title);
		assertThat(fileCount()).isEqualTo(1);
	}

	@Test
	void 공유제목이_101자면_필드오류이며_파일과_게시글과_이벤트가_생기지않는다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user);
		mockMvc.perform(request(recipe, "가".repeat(101), JPEG)
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
		assertThat(fileCount()).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shared_recipes", Long.class)).isZero();
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM events WHERE event_name = 'shared_recipe_published'", Long.class)).isZero();
	}

	@Test
	void 타인원본과_없는원본을_구분하고_비로그인은_거절한다() throws Exception {
		Long user = createUser(); Long otherRecipe = createRecipe(createUser());
		mockMvc.perform(request(otherRecipe, "제목", JPEG).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("RECIPE_NOT_SHAREABLE"));
		mockMvc.perform(request(999999L, "제목", JPEG).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RECIPE_NOT_FOUND"));
		mockMvc.perform(request(otherRecipe, "제목", JPEG)).andExpect(status().isUnauthorized());
		assertThat(fileCount()).isZero();
	}

	@Test
	void 같은주에는_숨겨진_게시글도_중복이고_다음주에는_다시_공유한다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user);
		clock.setNow(ZonedDateTime.parse("2026-03-08T23:59:59+09:00[Asia/Seoul]"));
		assertThat(publish(user, recipe)).isEqualTo(201);
		jdbcTemplate.update("UPDATE shared_recipes SET status = 'HIDDEN'");
		mockMvc.perform(request(recipe, "제목", JPEG).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_SHARED"));
		clock.setNow(ZonedDateTime.parse("2026-03-09T00:00:00+09:00[Asia/Seoul]"));
		assertThat(publish(user, recipe)).isEqualTo(201);
		assertThat(fileCount()).isEqualTo(2);
	}

	@Test
	void 동시게시도_하나만_성공하고_사진은_한장만_남는다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user);
		CountDownLatch start = new CountDownLatch(1);
		try (var executor = Executors.newFixedThreadPool(2)) {
			Callable<Integer> action = () -> { start.await(); return publish(user, recipe); };
			var first = executor.submit(action); var second = executor.submit(action);
			start.countDown();
			assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
		}
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shared_recipes", Long.class)).isEqualTo(1);
		assertThat(fileCount()).isEqualTo(1);
		awaitEventWrites();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events WHERE event_name = 'shared_recipe_published'", Long.class)).isEqualTo(1);
	}

	@Test
	void 사진저장후_DB실패시_파일과_게시글과_이벤트가_남지않는다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user);
		jdbcTemplate.execute("ALTER TABLE shared_recipes ADD CONSTRAINT test_reject_title CHECK (title <> '실패')");
		try {
			mockMvc.perform(request(recipe, "실패", JPEG).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
					.andExpect(status().isInternalServerError());
			assertThat(fileCount()).isZero();
			assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shared_recipes", Long.class)).isZero();
			awaitEventWrites();
			assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM events WHERE event_name = 'shared_recipe_published'", Long.class)).isZero();
		} finally {
			jdbcTemplate.execute("ALTER TABLE shared_recipes DROP CONSTRAINT test_reject_title");
		}
	}

	@Test
	void 사진을_여러장_보내면_거절한다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user);
		mockMvc.perform(request(recipe, "제목", JPEG)
				.file(new MockMultipartFile("image", "second.jpg", "image/jpeg", JPEG))
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		assertThat(fileCount()).isZero();
	}

	@Test
	void 사진용량과_위조헤더를_API에서도_검증한다() throws Exception {
		Long user = createUser(); Long recipe = createRecipe(user);
		mockMvc.perform(request(recipe, "제목", new byte[5 * 1024 * 1024 + 1]).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isContentTooLarge()).andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"))
				.andExpect(jsonPath("$.message").value("파일이 너무 커요 (최대 5MiB)"));
		mockMvc.perform(request(recipe, "제목", new byte[]{1, 2, 3}).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"))
				.andExpect(jsonPath("$.message").value("지원하지 않는 파일 형식이에요"));
		assertThat(fileCount()).isZero();
	}
}
