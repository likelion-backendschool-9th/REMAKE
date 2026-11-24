package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.remake.support.IntegrationTestSupport;

// V11(냉장고 스키마 정리)를 기존 데이터가 있는 DB에 적용해도 되는지 확인한다.
// 앱이 쓰는 public 스키마와 따로 만든 스키마에서 V9까지 적용하고 데이터를 넣은 뒤 최신 버전까지 올린다.
class IngredientSchemaMigrationTest extends IntegrationTestSupport {

	private static final String SCHEMA = "migration_v11_test";

	@Autowired
	private DataSource dataSource;

	private Flyway flyway(String target) {
		return Flyway.configure()
				.dataSource(dataSource)
				.schemas(SCHEMA)
				.locations("classpath:db/migration")
				.target(target)
				.load();
	}

	@AfterEach
	void dropSchema() {
		new JdbcTemplate(dataSource).execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
	}

	@Test
	void 기존_데이터가_있어도_컬럼을_지우고_다_쓴_재료는_정리한다() {
		flyway("9").migrate();
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.update("""
				INSERT INTO %1$s.users (id, provider, provider_user_id, email, role, status, plan, onboarding_completed,
				                        recipe_supplement_enabled, notification_enabled, created_at, updated_at)
				OVERRIDING SYSTEM VALUE
				VALUES (1, 'KAKAO', 'migration-1', NULL, 'USER', 'ACTIVE', 'BASIC', false, false, true, now(), now())
				""".formatted(SCHEMA));
		Long activeId = insertIngredient(jdbc, "우유", "ACTIVE", 1);
		Long finishedId = insertIngredient(jdbc, "다쓴우유", "FINISHED", 0);
		jdbc.update("""
				INSERT INTO %s.ingredient_logs (user_id, ingredient_id, type, name, amount, unit, created_at)
				VALUES (1, ?, 'USED', '다쓴우유', 1, 'EA', now())
				""".formatted(SCHEMA), finishedId);

		flyway("latest").migrate();

		// 다 쓴 재료만 지워지고, 기록은 이름·단위를 남긴 채 재료 연결만 비워진다
		assertThat(jdbc.queryForList("SELECT id FROM " + SCHEMA + ".ingredients", Long.class)).containsExactly(activeId);
		assertThat(jdbc.queryForObject("SELECT ingredient_id FROM " + SCHEMA + ".ingredient_logs", Long.class)).isNull();
		assertThat(jdbc.queryForObject("SELECT name FROM " + SCHEMA + ".ingredient_logs", String.class))
				.isEqualTo("다쓴우유");
		assertThat(columns(jdbc, "ingredients"))
				.doesNotContain("is_expiration_estimated", "status", "version")
				.contains("expiration_date", "amount", "initial_amount");
		assertThat(columns(jdbc, "ingredient_categories")).doesNotContain("default_unit").contains("sort_order");
	}

	@Test
	void 빈_DB에도_적용되고_카테고리_순서와_이름이_ERD_부록_A와_같다() {
		flyway("latest").migrate();
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);

		assertThat(jdbc.queryForList("SELECT code FROM " + SCHEMA + ".ingredient_categories ORDER BY sort_order",
				String.class)).containsExactly("VEGETABLE", "MEAT", "FRUIT", "SEAFOOD", "DAIRY_EGG", "GRAIN",
				"BEAN_TOFU", "PROCESSED", "SIDE_DISH", "SEASONING", "BEVERAGE", "ETC");
		assertThat(jdbc.queryForObject("SELECT name FROM " + SCHEMA + ".ingredient_categories WHERE code = 'SEASONING'",
				String.class)).isEqualTo("양념");
	}

	private Long insertIngredient(JdbcTemplate jdbc, String name, String status, int amount) {
		return jdbc.queryForObject("""
				INSERT INTO %s.ingredients (user_id, category_id, name, storage, expiration_date, amount, initial_amount,
				                            unit, status, created_at, updated_at)
				VALUES (1, 5, ?, 'FRIDGE', current_date, ?, 1, 'EA', ?, now(), now())
				RETURNING id
				""".formatted(SCHEMA), Long.class, name, amount, status);
	}

	private List<String> columns(JdbcTemplate jdbc, String table) {
		return jdbc.queryForList("""
				SELECT column_name FROM information_schema.columns WHERE table_schema = ? AND table_name = ?
				""", String.class, SCHEMA, table);
	}
}
