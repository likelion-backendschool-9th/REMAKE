package com.remake.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.remake.ingredient.domain.IngredientStatus;

/**
 * 모든 통합 테스트의 부모 클래스. 테스트 클래스는 이 클래스를 상속만 하고 설정 어노테이션을 붙이지 않는다.
 * 설정이 모두 같아야 Spring 앱을 한 번만 띄우고 재사용한다. 규칙: docs/agent/backend-testing.md (한국어판 docs/developer/backend-testing.md)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTestSupport.TestConfig.class)
public abstract class IntegrationTestSupport {

	/**
	 * 마이그레이션이 넣는 기준 데이터 테이블. 테스트가 비우지 않는다.
	 * 기준 데이터를 넣는 마이그레이션을 만들면 같은 PR에서 여기에 추가한다. 빠뜨리면 앱이 뜰 때 TestDatabase가 알려 준다.
	 */
	static final Set<String> REFERENCE_TABLES = Set.of("ingredient_categories", "ingredient_catalog");

	@TestConfiguration(proxyBeanMethods = false)
	static class TestConfig {

		// 앱 하나에 PostgreSQL 컨테이너 하나. 개발 DB에는 연결하지 않는다. 운영(compose.prod.yml)의 PostgreSQL 버전을 바꾸면 여기도 바꾼다.
		@Bean
		@ServiceConnection
		PostgreSQLContainer postgres() {
			return new PostgreSQLContainer("postgres:17");
		}

		// 운영 Clock 대신 쓰는 시험용 시계. 테스트마다 기본 시각으로 돌아간다.
		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock();
		}

		// 앱이 뜬 직후 테이블을 검사하고, 테스트마다 기준 데이터 밖의 테이블을 비운다.
		@Bean
		TestDatabase testDatabase(JdbcTemplate jdbcTemplate) {
			return new TestDatabase(jdbcTemplate, REFERENCE_TABLES);
		}
	}

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	// 저장 어댑터는 운영처럼 트랜잭션 안에서 부른다.
	@Autowired
	protected TransactionTemplate transactionTemplate;

	@Autowired
	protected MutableClock clock;

	@Autowired
	private TestDatabase testDatabase;

	// 외부 서비스 어댑터는 여기서 어댑터 클래스 단위로 가짜로 바꾼다.
	// @MockitoBean protected RecipeAiAdapter recipeAiAdapter;

	// 도우미는 필요한 테이블이 생기는 PR에서 여기에 추가한다.
	// protected Long createUser() { ... }            users 테이블을 만드는 PR

	/** 재료 카테고리 id. 기준 데이터 id는 하드코딩하지 않고 code로 찾는다(예: categoryId("MEAT")). */
	protected Long categoryId(String code) {
		return jdbcTemplate.queryForObject("SELECT id FROM ingredient_categories WHERE code = ?", Long.class, code);
	}

	/** 냉장고 재료 한 건을 넣고 id를 돌려준다. 단위·양은 기본값(EA, 1)이고 생성·수정 시각은 시험용 시계를 따른다. */
	protected Long createIngredient(Long userId, String categoryCode, String name, LocalDate expirationDate,
			IngredientStatus status) {
		return createIngredient(userId, categoryCode, name, expirationDate, status, null);
	}

	protected Long createIngredient(Long userId, String categoryCode, String name, LocalDate expirationDate,
			IngredientStatus status, Long price) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO ingredients (user_id, category_id, name, storage, expiration_date, amount, initial_amount,
				                         unit, price, status, created_at, updated_at)
				VALUES (?, ?, ?, 'FRIDGE', ?, 1, 1, 'EA', ?, ?, ?, ?)
				RETURNING id
				""", Long.class, userId, categoryId(categoryCode), name, expirationDate, price, status.name(), now, now);
	}

	@AfterEach
	void cleanUp() {
		clock.reset();
		testDatabase.cleanUp();
	}
}
