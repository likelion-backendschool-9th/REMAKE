package com.remake.mealplan.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.remake.mealplan.domain.MealPlan;
import com.remake.mealplan.domain.MealPlanSlot;
import com.remake.mealplan.domain.MealPlanStatus;
import com.remake.mealplan.domain.MealPurpose;
import com.remake.mealplan.domain.MealType;
import com.remake.support.IntegrationTestSupport;

// V6 테이블과 엔티티 매핑, DB 제약을 확인한다. 저장소는 운영처럼 트랜잭션 안에서 부른다.
class MealPlanPersistenceTest extends IntegrationTestSupport {

	// 기본 시각 2026-03-04 10:00 (서울)
	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	@Autowired
	private MealPlanJpaRepository mealPlanRepository;

	@Autowired
	private MealPlanSlotJpaRepository slotRepository;

	private MealPlan request(Long userId) {
		return MealPlan.request(userId, TODAY, MealPurpose.HIGH_PROTEIN, EnumSet.of(MealType.DINNER, MealType.BREAKFAST),
				3, TODAY);
	}

	private Long saveMealPlan(MealPlan mealPlan) {
		return transactionTemplate.execute(status -> mealPlanRepository.save(MealPlanJpaEntity.from(mealPlan)).getId());
	}

	private Long saveSlot(MealPlanSlot slot) {
		return transactionTemplate.execute(status -> slotRepository.save(MealPlanSlotJpaEntity.from(slot)).getId());
	}

	@Test
	void 식단표를_저장하면_같은_값으로_읽힌다() {
		Long userId = createUser();

		Long id = saveMealPlan(request(userId));
		MealPlan loaded = transactionTemplate
				.execute(status -> mealPlanRepository.findById(id).orElseThrow().toDomain());

		assertThat(loaded.id()).isEqualTo(id);
		assertThat(loaded.userId()).isEqualTo(userId);
		assertThat(loaded.startDate()).isEqualTo(TODAY);
		assertThat(loaded.purpose()).isEqualTo(MealPurpose.HIGH_PROTEIN);
		assertThat(loaded.mealTypes()).containsExactly(MealType.BREAKFAST, MealType.DINNER);
		assertThat(loaded.servings()).isEqualTo(3);
		assertThat(loaded.generatedDays()).isNull();
		assertThat(loaded.status()).isEqualTo(MealPlanStatus.GENERATING);
		assertThat(jdbcTemplate.queryForObject("SELECT meal_types FROM meal_plans WHERE id = ?", String.class, id))
				.isEqualTo("BREAKFAST,DINNER");
		assertThat(jdbcTemplate.queryForObject("SELECT created_at FROM meal_plans WHERE id = ?",
				LocalDateTime.class, id)).isEqualTo(LocalDateTime.of(2026, 3, 4, 10, 0));
		assertThat(jdbcTemplate.queryForObject("SELECT updated_at FROM meal_plans WHERE id = ?",
				LocalDateTime.class, id)).isEqualTo(LocalDateTime.of(2026, 3, 4, 10, 0));
	}

	@Test
	void 완성하면_상태와_만든_일수가_저장된다() {
		Long id = saveMealPlan(request(createUser()));

		transactionTemplate.executeWithoutResult(status -> {
			MealPlanJpaEntity entity = mealPlanRepository.findById(id).orElseThrow();
			MealPlan mealPlan = entity.toDomain();
			mealPlan.complete(5);
			entity.apply(mealPlan);
		});

		MealPlan loaded = transactionTemplate
				.execute(status -> mealPlanRepository.findById(id).orElseThrow().toDomain());
		assertThat(loaded.status()).isEqualTo(MealPlanStatus.COMPLETED);
		assertThat(loaded.generatedDays()).isEqualTo(5);
	}

	@Test
	void 회원당_생성_중인_식단표는_하나만_저장된다() {
		Long userId = createUser();
		saveMealPlan(request(userId));

		assertThatThrownBy(() -> saveMealPlan(request(userId))).isInstanceOf(DataIntegrityViolationException.class);
		// 다른 회원은 상관없다
		saveMealPlan(request(createUser()));
	}

	@Test
	void 생성이_끝나면_같은_회원이_다시_요청할_수_있다() {
		Long userId = createUser();
		Long first = saveMealPlan(request(userId));
		jdbcTemplate.update("UPDATE meal_plans SET status = 'FAILED' WHERE id = ?", first);

		Long second = saveMealPlan(request(userId));

		assertThat(second).isNotEqualTo(first);
	}

	@Test
	void 칸을_저장하면_같은_값으로_읽히고_수정_시각이_채워진다() {
		Long userId = createUser();
		Long mealPlanId = saveMealPlan(request(userId));
		Long recipeId = createRecipe(userId);

		Long slotId = saveSlot(MealPlanSlot.create(mealPlanId, TODAY, MealType.DINNER, recipeId));
		MealPlanSlot loaded = transactionTemplate
				.execute(status -> slotRepository.findById(slotId).orElseThrow().toDomain());

		assertThat(loaded.mealPlanId()).isEqualTo(mealPlanId);
		assertThat(loaded.slotDate()).isEqualTo(TODAY);
		assertThat(loaded.mealType()).isEqualTo(MealType.DINNER);
		assertThat(loaded.recipeId()).isEqualTo(recipeId);
		assertThat(loaded.cookedAt()).isNull();
		assertThat(jdbcTemplate.queryForObject("SELECT updated_at FROM meal_plan_slots WHERE id = ?",
				LocalDateTime.class, slotId)).isEqualTo(LocalDateTime.of(2026, 3, 4, 10, 0));
	}

	@Test
	void 요리_시각이_저장된다() {
		Long userId = createUser();
		Long mealPlanId = saveMealPlan(request(userId));
		Long slotId = saveSlot(MealPlanSlot.create(mealPlanId, TODAY, MealType.DINNER, createRecipe(userId)));

		transactionTemplate.executeWithoutResult(status -> {
			MealPlanSlotJpaEntity entity = slotRepository.findById(slotId).orElseThrow();
			MealPlanSlot slot = entity.toDomain();
			slot.markCooked(TODAY.atTime(19, 0));
			entity.apply(slot);
		});

		MealPlanSlot loaded = transactionTemplate
				.execute(status -> slotRepository.findById(slotId).orElseThrow().toDomain());
		assertThat(loaded.cookedAt()).isEqualTo(TODAY.atTime(19, 0));
	}

	@Test
	void 한_식단표에서_같은_날_같은_끼니는_하나다() {
		Long userId = createUser();
		Long mealPlanId = saveMealPlan(request(userId));
		Long recipeId = createRecipe(userId);
		// 레시피 FK 때문이 아니라 칸 중복 때문에 실패하는지 보려고 실제 레시피를 하나 더 만든다
		Long otherRecipeId = createRecipe(userId);
		saveSlot(MealPlanSlot.create(mealPlanId, TODAY, MealType.DINNER, recipeId));

		assertThatThrownBy(() -> saveSlot(MealPlanSlot.create(mealPlanId, TODAY, MealType.DINNER, otherRecipeId)))
				.isInstanceOf(DataIntegrityViolationException.class);
		// 같은 날 다른 끼니, 다른 날 같은 끼니는 된다
		saveSlot(MealPlanSlot.create(mealPlanId, TODAY, MealType.BREAKFAST, recipeId));
		saveSlot(MealPlanSlot.create(mealPlanId, TODAY.plusDays(1), MealType.DINNER, recipeId));
	}

	@Test
	void 칸의_레시피는_실제로_있어야_하고_칸이_쓰는_레시피는_지울_수_없다() {
		Long userId = createUser();
		Long mealPlanId = saveMealPlan(request(userId));
		Long recipeId = createRecipe(userId);

		assertThatThrownBy(() -> saveSlot(MealPlanSlot.create(mealPlanId, TODAY, MealType.DINNER, recipeId + 1000)))
				.isInstanceOf(DataIntegrityViolationException.class);
		saveSlot(MealPlanSlot.create(mealPlanId, TODAY, MealType.DINNER, recipeId));
		assertThatThrownBy(() -> jdbcTemplate.update("DELETE FROM recipes WHERE id = ?", recipeId))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void 식단표를_지우면_칸도_같이_지워진다() {
		Long userId = createUser();
		Long mealPlanId = saveMealPlan(request(userId));
		Long recipeId = createRecipe(userId);
		saveSlot(MealPlanSlot.create(mealPlanId, TODAY, MealType.DINNER, recipeId));
		saveSlot(MealPlanSlot.create(mealPlanId, TODAY, MealType.BREAKFAST, recipeId));

		jdbcTemplate.update("DELETE FROM meal_plans WHERE id = ?", mealPlanId);

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM meal_plan_slots WHERE meal_plan_id = ?",
				Integer.class, mealPlanId)).isZero();
	}

	@Test
	void DB도_잘못된_값을_막는다() {
		Long userId = createUser();
		// 올바른 값은 저장된다
		insertCompletedPlan(userId, "BALANCED", "DINNER", 1, 1);

		// 실패마다 막은 제약 이름까지 확인한다. 인자 누락 같은 다른 이유로 실패해도 통과하지 않게 한다
		assertRejectedBy("ck_meal_plans_purpose", () -> insertCompletedPlan(userId, "KETO", "DINNER", 1, 1));
		assertRejectedBy("ck_meal_plans_meal_types", () -> insertCompletedPlan(userId, "BALANCED", "DINNER,SNACK", 1, 1));
		assertRejectedBy("ck_meal_plans_meal_types", () -> insertCompletedPlan(userId, "BALANCED", "", 1, 1));
		assertRejectedBy("ck_meal_plans_servings", () -> insertCompletedPlan(userId, "BALANCED", "DINNER", 0, 1));
		assertRejectedBy("ck_meal_plans_generated_days", () -> insertCompletedPlan(userId, "BALANCED", "DINNER", 1, 8));
		// 회원이 없으면 저장되지 않는다
		assertRejectedBy("fk_meal_plans_user", () -> insertCompletedPlan(userId + 1000, "BALANCED", "DINNER", 1, 1));
	}

	/** 완성된 식단표 한 행을 SQL로 넣는다. 시각 칸까지 모든 값을 넘겨 제약 말고는 실패할 이유가 없게 한다. */
	private void insertCompletedPlan(Long userId, String purpose, String mealTypes, int servings, int generatedDays) {
		LocalDateTime now = LocalDateTime.of(2026, 3, 4, 10, 0);
		jdbcTemplate.update("""
				INSERT INTO meal_plans (user_id, start_date, purpose, meal_types, servings, generated_days, status,
				                        created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, 'COMPLETED', ?, ?)
				""", userId, TODAY, purpose, mealTypes, servings, generatedDays, now, now);
	}

	private static void assertRejectedBy(String constraint, Runnable insert) {
		assertThatThrownBy(insert::run)
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining(constraint);
	}
}
