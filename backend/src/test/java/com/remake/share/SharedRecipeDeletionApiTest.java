package com.remake.share;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.global.config.UploadProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.global.web.LoginUserIdArgumentResolver;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.recipe.application.port.out.SaveRecipePort;
import com.remake.recipe.domain.Recipe;
import com.remake.recipe.domain.RecipeIngredient;
import com.remake.recipe.domain.RecipeSource;
import com.remake.recipe.domain.RecipeUnit;
import com.remake.share.application.port.in.DeleteSharedRecipeUseCase;
import com.remake.share.application.port.in.PublishSharedRecipeUseCase;
import com.remake.share.application.port.in.RetrySharedRecipePhotoDeletionUseCase;
import com.remake.share.application.port.in.SaveSharedRecipeUseCase;
import com.remake.share.application.port.in.SharedRecipePhotoUpload;
import com.remake.share.application.port.out.ManageSharedRecipePort;
import com.remake.support.IntegrationTestSupport;

class SharedRecipeDeletionApiTest extends IntegrationTestSupport {
	@Autowired private DeleteSharedRecipeUseCase deletion;
	@Autowired private PublishSharedRecipeUseCase publish;
	@Autowired private SaveRecipePort recipes;
	@Autowired private SaveSharedRecipeUseCase saves;
	@Autowired private RetrySharedRecipePhotoDeletionUseCase retry;
	@Autowired private ManageSharedRecipePort management;
	@Autowired private UploadProperties uploads;
	@Autowired private com.remake.user.application.port.in.DeleteAccountUseCase deleteAccount;
	private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 1};
	private static final String ROOT = "/api/v1/user/shared-recipes";

	private Long recipe(Long user) {
		return transactionTemplate.execute(tx -> recipes.save(Recipe.create(user, RecipeSource.QUICK,
				"원본", 1, 10, null, null,
				List.of(new RecipeIngredient(null, "양파", BigDecimal.ONE, RecipeUnit.EA, IngredientCategoryCode.VEGETABLE)),
				List.of("익힌다"))).id());
	}

	private Long post(Long user, Long recipe) {
		return publish.publish(user, recipe, "공유 제목", new SharedRecipePhotoUpload("image/jpeg", JPEG.length,
				() -> new ByteArrayInputStream(JPEG)));
	}

	private String key(Long id) {
		return jdbcTemplate.queryForObject("SELECT image_key FROM shared_recipes WHERE id = ?", String.class, id);
	}

	private Path path(String key) { return Path.of(uploads.rootDirectory()).resolve(key); }
	private long count(String table) { return jdbcTemplate.queryForObject("SELECT count(*) FROM " + table, Long.class); }
	private void visible(Long id) {
		assertThat(jdbcTemplate.queryForObject("SELECT status FROM shared_recipes WHERE id = ?", String.class, id)).isEqualTo("VISIBLE");
	}
	private void deleted(Long id) {
		var row = jdbcTemplate.queryForMap("SELECT status, title, image_key FROM shared_recipes WHERE id = ?", id);
		assertThat(row).containsEntry("status", "DELETED").containsEntry("title", null).containsEntry("image_key", null);
	}

	@Test
	void 본인목록은_공개숨김만_최신순으로_페이징하고_타인삭제글은_제외한다() throws Exception {
		Long user = createUser(); Long other = createUser();
		Long old = post(user, recipe(user)); clock.advance(Duration.ofMinutes(1));
		Long hidden = post(user, recipe(user));
		jdbcTemplate.update("UPDATE shared_recipes SET status = 'HIDDEN' WHERE id = ?", hidden);
		Long removed = post(user, recipe(user)); deletion.delete(user, removed);
		post(other, recipe(other));
		mockMvc.perform(get(ROOT + "/mine").param("size", "1").requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(hidden))
				.andExpect(jsonPath("$.content[0].status").value("HIDDEN"))
				.andExpect(jsonPath("$.content[0].imageUrl").isEmpty()).andExpect(jsonPath("$.content[0].canDelete").value(true))
				.andExpect(jsonPath("$.content[0].userId").doesNotExist()).andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.hasNext").value(true));
		mockMvc.perform(get(ROOT + "/mine").param("size", "1").param("page", "1")
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(old))
				.andExpect(jsonPath("$.content[0].imageUrl").value("/api/v1/user/shared-recipe-photos/" + old))
				.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	void 빈목록과_로그인_페이지입력_계약을_검증한다() throws Exception {
		Long user = createUser();
		mockMvc.perform(get(ROOT + "/mine")).andExpect(status().isUnauthorized());
		mockMvc.perform(get(ROOT + "/mine").requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").value(0));
		for (String query : List.of("?page=-1", "?size=0", "?size=101", "?page=x")) {
			mockMvc.perform(get(ROOT + "/mine" + query).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		}
	}

	@Test
	void 숨김글은_작성자만_상세삭제에_진입하고_사진은_노출하지않는다() throws Exception {
		Long user = createUser(); Long other = createUser(); Long id = post(user, recipe(user));
		mockMvc.perform(get(ROOT + "/" + id).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, other))
				.andExpect(status().isOk()).andExpect(jsonPath("$.canDelete").value(false));
		jdbcTemplate.update("UPDATE shared_recipes SET status = 'HIDDEN' WHERE id = ?", id);
		mockMvc.perform(get(ROOT + "/" + id).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isOk()).andExpect(jsonPath("$.canDelete").value(true))
				.andExpect(jsonPath("$.imageUrl").isEmpty()).andExpect(jsonPath("$.authorId").doesNotExist());
		mockMvc.perform(get(ROOT + "/" + id).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, other))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/user/shared-recipe-photos/" + id).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete(ROOT + "/" + id).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
				.andExpect(status().isNoContent());
		deleted(id);
	}

	@Test
	void 삭제는_204이며_제목사진을_제거하고_반복삭제도_204다() throws Exception {
		Long user = createUser(); Long original = recipe(user); Long id = post(user, original); String key = key(id);
		LocalDateTime sharedAt = jdbcTemplate.queryForObject("SELECT created_at FROM shared_recipes WHERE id = ?", LocalDateTime.class, id);
		clock.advance(Duration.ofMinutes(1));
		for (int i = 0; i < 2; i++) {
			mockMvc.perform(delete(ROOT + "/" + id).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
					.andExpect(status().isNoContent()).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(""));
		}
		deleted(id); assertThat(path(key)).doesNotExist(); assertThat(count("shared_recipe_photo_deletions")).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT recipe_id FROM shared_recipes WHERE id = ?", Long.class, id)).isEqualTo(original);
		assertThat(jdbcTemplate.queryForObject("SELECT created_at FROM shared_recipes WHERE id = ?", LocalDateTime.class, id)).isEqualTo(sharedAt);
		assertThat(jdbcTemplate.queryForObject("SELECT updated_at FROM shared_recipes WHERE id = ?", LocalDateTime.class, id)).isAfter(sharedAt);
	}

	@Test
	void 타인과_없는글은_같은404이고_비로그인과_잘못된ID도_차단한다() throws Exception {
		Long user = createUser(); Long other = createUser(); Long id = post(user, recipe(user)); String key = key(id);
		for (Long target : List.of(id, Long.MAX_VALUE)) {
			mockMvc.perform(delete(ROOT + "/" + target).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, other))
					.andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("SHARED_RECIPE_NOT_FOUND"));
		}
		mockMvc.perform(delete(ROOT + "/" + id)).andExpect(status().isUnauthorized());
		for (String target : List.of("0", "-1", "x")) {
			mockMvc.perform(delete(ROOT + "/" + target).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
					.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		}
		visible(id); assertThat(path(key)).exists(); assertThat(count("shared_recipe_photo_deletions")).isZero();
		deletion.delete(user, id);
		mockMvc.perform(delete(ROOT + "/" + id).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, other)).andExpect(status().isNotFound());
	}

	@Test
	void 원본저장즐겨찾기_수정장바구니_신고_확정순위보상을_보존한다() {
		Long user = createUser(); Long reader = createUser(); Long original = recipe(user); Long id = post(user, original);
		saves.save(reader, id);
		LocalDateTime now = LocalDateTime.now(clock); var week = now.toLocalDate().minusWeeks(1);
		jdbcTemplate.update("UPDATE saved_recipes SET is_favorite = true WHERE user_id = ?", reader);
		jdbcTemplate.update("UPDATE cart_items SET amount = 3.5, updated_at = ? WHERE user_id = ?", now.plusMinutes(1), reader);
		jdbcTemplate.update("INSERT INTO shared_recipe_reports(shared_recipe_id, reporter_id, reason, created_at) VALUES (?, ?, 'OTHER', ?)", id, reader, now);
		jdbcTemplate.update("INSERT INTO shared_recipe_weekly_ranks(week_start,shared_recipe_id,rank_no,save_count,created_at) VALUES (?, ?, 1, 1, ?)", week, id, now);
		jdbcTemplate.update("INSERT INTO shared_recipe_weekly_settlements VALUES (?, ?)", week, now);
		jdbcTemplate.update("INSERT INTO generation_quota_grants(user_id,shared_recipe_id,week_start,rank_no,amount,remaining,expires_at,created_at) VALUES (?, ?, ?, 1, 10, 7, ?, ?)", user, id, week, now.plusMonths(1), now);
		awaitEventWrites(); long events = count("events");
		deletion.delete(user, id);
		assertThat(count("recipes")).isEqualTo(1); assertThat(count("recipe_ingredients")).isEqualTo(1); assertThat(count("recipe_steps")).isEqualTo(1);
		assertThat(jdbcTemplate.queryForMap("SELECT shared_recipe_id, is_favorite FROM saved_recipes WHERE user_id = ?", reader))
				.containsEntry("shared_recipe_id", id).containsEntry("is_favorite", true);
		assertThat(jdbcTemplate.queryForObject("SELECT amount FROM cart_items WHERE user_id = ?", BigDecimal.class, reader)).isEqualByComparingTo("3.5");
		for (String table : List.of("shared_recipe_reports", "shared_recipe_weekly_ranks", "generation_quota_grants")) {
			assertThat(jdbcTemplate.queryForObject("SELECT shared_recipe_id FROM " + table, Long.class)).isEqualTo(id);
		}
		assertThat(jdbcTemplate.queryForObject("SELECT remaining FROM generation_quota_grants", Integer.class)).isEqualTo(7);
		assertThat(count("shared_recipe_weekly_settlements")).isEqualTo(1);
		awaitEventWrites(); assertThat(count("events")).isEqualTo(events);
	}

	@Test
	void 삭제글은_공개목록상세사진저장신고에서_제외되고_동일주재공유는_불가하다() throws Exception {
		Long user = createUser(); Long reader = createUser(); Long original = recipe(user); Long id = post(user, original);
		saves.save(reader, id); deletion.delete(user, id);
		for (String url : List.of(ROOT + "/" + id, "/api/v1/user/shared-recipe-photos/" + id)) {
			mockMvc.perform(get(url).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user)).andExpect(status().isNotFound());
		}
		mockMvc.perform(get(ROOT + "/weekly-top").requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
		for (String sort : List.of("popular", "latest")) {
			mockMvc.perform(get(ROOT + "/ranking").param("sort", sort).param("q", "공유")
					.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
					.andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty());
		}
		assertThatThrownBy(() -> saves.save(reader, id)).isInstanceOfSatisfying(BusinessException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.SHARED_RECIPE_NOT_FOUND));
		assertThatThrownBy(() -> post(user, original)).isInstanceOfSatisfying(BusinessException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALREADY_SHARED));
		clock.advance(Duration.ofDays(7)); assertThat(post(user, original)).isNotEqualTo(id);
	}

	@Test
	void 호출자가_롤백하면_상태와사진큐가_복구되고_파일은_남는다() {
		Long user = createUser(); Long id = post(user, recipe(user)); String key = key(id);
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(tx -> {
			deletion.delete(user, id); assertThat(path(key)).exists();
			throw new IllegalStateException("rollback");
		})).isInstanceOf(IllegalStateException.class);
		visible(id); assertThat(key(id)).isEqualTo(key); assertThat(path(key)).exists();
		assertThat(count("shared_recipe_photo_deletions")).isZero();
	}

	@Test
	void DB변경실패시_큐삽입도_롤백되고_사진은_남는다() {
		Long user = createUser(); Long id = post(user, recipe(user)); String key = key(id);
		jdbcTemplate.execute("ALTER TABLE shared_recipes ADD CONSTRAINT test_no_deleted CHECK (status <> 'DELETED')");
		try {
			assertThatThrownBy(() -> deletion.delete(user, id)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
			visible(id); assertThat(path(key)).exists(); assertThat(count("shared_recipe_photo_deletions")).isZero();
		} finally {
			jdbcTemplate.execute("ALTER TABLE shared_recipes DROP CONSTRAINT test_no_deleted");
		}
	}

	@Test
	void 실제파일삭제실패는_접근차단후_영속큐로_재시도한다() throws Exception {
		Long user = createUser(); Long id = post(user, recipe(user)); String key = key(id); Path file = path(key);
		Files.delete(file); Files.createDirectory(file); Path blocker = Files.write(file.resolve("blocker"), JPEG);
		try {
			deletion.delete(user, id); deleted(id);
			assertThat(count("shared_recipe_photo_deletions")).isEqualTo(1);
			assertThat(jdbcTemplate.queryForObject("SELECT image_key FROM shared_recipe_photo_deletions", String.class)).isEqualTo(key);
			mockMvc.perform(get("/api/v1/user/shared-recipe-photos/" + id).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user))
					.andExpect(status().isNotFound());
			retry.retryPending(); assertThat(count("shared_recipe_photo_deletions")).isEqualTo(1);
		} finally {
			Files.deleteIfExists(blocker); Files.deleteIfExists(file);
		}
		Files.write(file, JPEG); retry.retryPending();
		assertThat(file).doesNotExist(); assertThat(count("shared_recipe_photo_deletions")).isZero();
		retry.retryPending();
	}

	@Test
	void 이미없는사진도_삭제성공하고_동시삭제가_큐를_중복생성하지않는다() throws Exception {
		Long user = createUser(); Long id = post(user, recipe(user)); String key = key(id); Files.delete(path(key));
		CountDownLatch start = new CountDownLatch(1);
		try (var pool = Executors.newFixedThreadPool(2)) {
			Callable<Void> action = () -> { start.await(); deletion.delete(user, id); return null; };
			var first = pool.submit(action); var second = pool.submit(action); start.countDown();
			first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
		}
		deleted(id); assertThat(count("shared_recipe_photo_deletions")).isZero();
	}

	@Test
	void 삭제가_먼저잠그면_후행신규저장은_404다() throws Exception {
		Long user = createUser(); Long reader = createUser(); Long id = post(user, recipe(user));
		CountDownLatch locked = new CountDownLatch(1); CountDownLatch release = new CountDownLatch(1);
		try (var pool = Executors.newFixedThreadPool(2)) {
			var first = pool.submit(() -> transactionTemplate.executeWithoutResult(tx -> {
				deletion.delete(user, id); locked.countDown(); await(release);
			}));
			try {
				assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
				var second = pool.submit(() -> {
					try { saves.save(reader, id); return "saved"; }
					catch (BusinessException e) { return e.getErrorCode().name(); }
				});
				assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
				release.countDown(); first.get(10, TimeUnit.SECONDS);
				assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo("SHARED_RECIPE_NOT_FOUND");
			} finally { release.countDown(); }
		}
		assertThat(count("saved_recipes")).isZero(); assertThat(count("cart_items")).isZero();
	}

	@Test
	void 저장이_먼저잠그면_삭제가_기다렸다가_기존저장과_장바구니를_보존한다() throws Exception {
		Long user = createUser(); Long reader = createUser(); Long id = post(user, recipe(user));
		CountDownLatch locked = new CountDownLatch(1); CountDownLatch release = new CountDownLatch(1);
		try (var pool = Executors.newFixedThreadPool(2)) {
			var first = pool.submit(() -> transactionTemplate.executeWithoutResult(tx -> {
				saves.save(reader, id); locked.countDown(); await(release);
			}));
			try {
				assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
				var second = pool.submit(() -> deletion.delete(user, id));
				assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
				release.countDown(); first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
			} finally { release.countDown(); }
		}
		deleted(id); assertThat(count("saved_recipes")).isEqualTo(1); assertThat(count("cart_items")).isEqualTo(1);
	}

	@Test
	void 삭제는_확정조정잠금이_끝날때까지_기다린다() throws Exception {
		Long user = createUser(); Long id = post(user, recipe(user));
		CountDownLatch locked = new CountDownLatch(1); CountDownLatch release = new CountDownLatch(1);
		try (var pool = Executors.newFixedThreadPool(2)) {
			var first = pool.submit(() -> transactionTemplate.executeWithoutResult(tx -> {
				management.lockDeletionAndSettlement(); locked.countDown(); await(release);
			}));
			try {
				assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
				var second = pool.submit(() -> deletion.delete(user, id));
				assertThatThrownBy(() -> second.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);
				visible(id); release.countDown(); first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
			} finally { release.countDown(); }
		}
		deleted(id);
	}

	@Test
	void 실제탈퇴후에도_삭제글은_재노출하지않고_타인저장과_미삭제글은_유지한다() throws Exception {
		Long user = createUser(); Long reader = createUser();
		jdbcTemplate.update("UPDATE users SET email = 'deletion-test@example.test' WHERE id = ?", user);
		Long id = post(user, recipe(user)); Long retained = post(user, recipe(user));
		saves.save(reader, id); deletion.delete(user, id);
		var code = new java.util.concurrent.atomic.AtomicReference<String>();
		org.mockito.Mockito.doAnswer(invocation -> { code.set(invocation.getArgument(1)); return null; })
				.when(deletionCodeMailAdapter).send(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/user/account/deletion-code")
				.requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, user)).andExpect(status().isCreated());
		deleteAccount.delete(new com.remake.user.application.port.in.DeleteAccountUseCase.Command(user,
				com.remake.user.domain.DeletionMethod.EMAIL_CODE, code.get(), null, null, null));
		deleted(id);
		assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM users WHERE id = ?", LocalDateTime.class, user)).isNotNull();
		assertThat(count("saved_recipes")).isEqualTo(1); assertThat(count("cart_items")).isEqualTo(1);
		mockMvc.perform(get(ROOT + "/" + id).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader)).andExpect(status().isNotFound());
		mockMvc.perform(get(ROOT + "/" + retained).requestAttr(LoginUserIdArgumentResolver.ATTRIBUTE, reader))
				.andExpect(status().isOk()).andExpect(jsonPath("$.canDelete").value(false)).andExpect(jsonPath("$.authorId").doesNotExist());
	}

	@Test
	void 공유와삭제가_동시에실행돼도_같은주이력은_보존되고_재공유할수없다() throws Exception {
		Long user = createUser(); Long original = recipe(user); Long id = post(user, original);
		CountDownLatch start = new CountDownLatch(1);
		try (var pool = Executors.newFixedThreadPool(2)) {
			var remove = pool.submit(() -> { await(start); deletion.delete(user, id); });
			var republish = pool.submit(() -> {
				await(start);
				try { post(user, original); return "published"; }
				catch (BusinessException e) { return e.getErrorCode().name(); }
			});
			start.countDown(); remove.get(10, TimeUnit.SECONDS);
			assertThat(republish.get(10, TimeUnit.SECONDS)).isEqualTo("ALREADY_SHARED");
		}
		deleted(id); assertThat(count("shared_recipes")).isEqualTo(1);
		assertThat(count("shared_recipe_photo_deletions")).isZero();
	}

	private void await(CountDownLatch latch) {
		try { assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue(); }
		catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
	}
}
