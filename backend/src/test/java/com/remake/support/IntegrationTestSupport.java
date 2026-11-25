package com.remake.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.remake.auth.adapter.out.oauth.SocialOAuthAdapter;
import com.remake.global.config.AsyncConfig;
import com.remake.global.config.UploadProperties;
import com.remake.mealplan.adapter.out.ai.GeminiMealPlanAdapter;
import com.remake.receipt.application.port.out.ReceiptImageStoragePort;
import com.remake.recipe.adapter.out.ai.GeminiRecipeAdapter;
import com.remake.user.adapter.out.mail.LoggingDeletionCodeMailAdapter;
import com.remake.user.domain.AuthProvider;

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

	@Autowired
	private UploadProperties uploadProperties;

	// 이벤트 비동기 적재가 truncate 전에 끝나게 기다린다. SyncTaskExecutor로 바꾸지 않는다.
	@Autowired
	@Qualifier(AsyncConfig.EVENT_TASK_EXECUTOR)
	private ThreadPoolTaskExecutor eventTaskExecutor;

	// 식단표 생성 서버 작업. 작업이 끝난 뒤에 비우도록 기다린다(작업이 이벤트도 남기므로 이벤트보다 먼저 기다린다).
	@Autowired
	@Qualifier(AsyncConfig.MEAL_PLAN_TASK_EXECUTOR)
	private ThreadPoolTaskExecutor mealPlanTaskExecutor;

	// 외부 서비스 어댑터는 여기서 어댑터 클래스 단위로 가짜로 바꾼다.
	@MockitoBean
	protected SocialOAuthAdapter socialOAuthAdapter;

	// AI 레시피 생성. 실제 Gemini를 부르지 않는다. 테스트마다 필요한 응답·예외를 stub한다.
	@MockitoBean
	protected GeminiRecipeAdapter geminiRecipeAdapter;

	// 식단표 AI. 실제 Gemini를 부르지 않는다. 테스트마다 만들 칸·예외를 stub한다. stub하지 않으면 빈 목록(칸 빠짐 = AI_INVALID_RESPONSE)이다.
	@MockitoBean
	protected GeminiMealPlanAdapter mealPlanAiAdapter;

	// 계정 삭제 인증번호 메일. 실제 SMTP를 쓰지 않고 테스트가 평문 번호를 가로챈다.
	// (MockedAdapterWiringTest가 구체 어댑터 생성자를 검사하므로 포트가 아니라 어댑터를 바꾼다.)
	@MockitoBean
	protected LoggingDeletionCodeMailAdapter deletionCodeMailAdapter;

	// 영수증 사진 저장소. 진짜 구현을 그대로 쓰되 호출을 지켜보고 일부 호출만 실패시킬 수 있다.
	// 테스트 클래스마다 따로 선언하면 Spring 컨텍스트가 하나 더 떠서(앱·DB 기동 2회) 여기 한 곳에 둔다.
	@MockitoSpyBean
	protected ReceiptImageStoragePort images;

	/** 테스트용 회원 한 명을 넣고 id를 돌려준다. provider_user_id는 호출마다 다르게 만든다. */
	protected Long createUser() {
		return createUser(AuthProvider.KAKAO, "test-" + System.nanoTime());
	}

	protected Long createUser(AuthProvider provider, String providerUserId) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO users (provider, provider_user_id, email, role, status, plan, onboarding_completed,
				                   recipe_supplement_enabled, notification_enabled, created_at, updated_at)
				VALUES (?, ?, NULL, 'USER', 'ACTIVE', 'BASIC', false, false, true, ?, ?)
				RETURNING id
				""", Long.class, provider.name(), providerUserId, now, now);
	}

	/** 서버 EventTracker(@Async) 적재가 끝날 때까지 기다린다. */
	protected void awaitEventWrites() {
		Awaitility.await().atMost(Duration.ofSeconds(3)).until(() -> idle(eventTaskExecutor));
	}

	/** 식단표 생성 서버 작업이 모두 끝날 때까지 기다린다. */
	protected void awaitMealPlanJobs() {
		Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> idle(mealPlanTaskExecutor));
	}

	private static boolean idle(ThreadPoolTaskExecutor executor) {
		ThreadPoolExecutor pool = executor.getThreadPoolExecutor();
		return executor.getActiveCount() == 0 && pool.getQueue().isEmpty();
	}

	/** 재료 카테고리 id. 기준 데이터 id는 하드코딩하지 않고 code로 찾는다(예: categoryId("MEAT")). */
	protected Long categoryId(String code) {
		return jdbcTemplate.queryForObject("SELECT id FROM ingredient_categories WHERE code = ?", Long.class, code);
	}

	/**
	 * 냉장고 재료 한 건을 넣고 id를 돌려준다. 단위·양은 기본값(EA, 1)이고 생성·수정 시각은 시험용 시계를 따른다.
	 * 사용자는 createUser()로 먼저 만든다. 없는 사용자 번호면 외래 키 오류로 실패한다.
	 */
	protected Long createIngredient(Long userId, String categoryCode, String name, LocalDate expirationDate) {
		return createIngredient(userId, categoryCode, name, expirationDate, null);
	}

	protected Long createIngredient(Long userId, String categoryCode, String name, LocalDate expirationDate,
			Long price) {
		LocalDateTime now = LocalDateTime.now(clock);
		return jdbcTemplate.queryForObject("""
				INSERT INTO ingredients (user_id, category_id, name, storage, expiration_date, amount, initial_amount,
				                         unit, price, created_at, updated_at)
				VALUES (?, ?, ?, 'FRIDGE', ?, 1, 1, 'EA', ?, ?, ?)
				RETURNING id
				""", Long.class, userId, categoryId(categoryCode), name, expirationDate, price, now, now);
	}

	/**
	 * 레시피 한 건(빠른 생성, 1인분, 10분)을 넣고 id를 돌려준다. 재료·단계는 넣지 않는다.
	 * 식단 칸·장바구니·저장 목록처럼 레시피 id만 필요한 테스트가 쓴다(레시피 FK가 있어 없는 번호는 넣을 수 없다).
	 * 사용자는 createUser()로 먼저 만든다.
	 */
	protected Long createRecipe(Long userId) {
		return jdbcTemplate.queryForObject("""
				INSERT INTO recipes (user_id, source, title, servings, cook_minutes, created_at)
				VALUES (?, 'QUICK', '테스트 레시피', 1, 10, ?)
				RETURNING id
				""", Long.class, userId, LocalDateTime.now(clock));
	}

	// 출처 외래 키를 만족하도록 테스트에서 실제 영수증 품목을 만든다.
	protected Long createReceiptItem(Long userId, String name, String categoryCode) {
		LocalDateTime now = LocalDateTime.now(clock);
		Long receiptId = jdbcTemplate.queryForObject("""
				INSERT INTO receipts (user_id, purchased_on, store_name, image_key, created_at)
				VALUES (?, ?, '동네마트', 'test-image', ?) RETURNING id
				""", Long.class, userId, now.toLocalDate(), now);
		return jdbcTemplate.queryForObject("""
				INSERT INTO receipt_items (receipt_id, category_id, name, storage, created_at)
				VALUES (?, ?, ?, 'FRIDGE', ?) RETURNING id
				""", Long.class, receiptId, categoryId(categoryCode), name, now);
	}

	@AfterEach
	void cleanUp() throws IOException {
		awaitMealPlanJobs();
		awaitEventWrites();
		clock.reset();
		testDatabase.cleanUp();
		// 파일 저장 어댑터는 가짜로 바꾸지 않는다. 테스트 전용 폴더의 파일만 지운다.
		Path root = Path.of(uploadProperties.rootDirectory());
		try (var files = Files.walk(root)) {
			for (Path file : files.filter(path -> !Files.isDirectory(path, java.nio.file.LinkOption.NOFOLLOW_LINKS)).toList()) {
				Files.deleteIfExists(file);
			}
		}
	}
}
