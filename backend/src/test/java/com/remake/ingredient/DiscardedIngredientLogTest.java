package com.remake.ingredient;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.remake.ingredient.application.port.in.ConsumeIngredientCommand;
import com.remake.ingredient.application.port.in.ConsumeIngredientUseCase;
import com.remake.ingredient.application.port.in.GetDiscardedIngredientLogsUseCase;
import com.remake.ingredient.domain.ConsumeVia;
import com.remake.ingredient.domain.DiscardedIngredientLog;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientLogType;
import com.remake.support.IntegrationTestSupport;

// 사용·폐기 기록의 카테고리 저장(#252)과 버린 기록 조회 UseCase. 오늘은 시험용 시계의 2026-03-04이다.
class DiscardedIngredientLogTest extends IntegrationTestSupport {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 4);

	private Long me;
	private Long other;

	@Autowired
	private ConsumeIngredientUseCase consumeUseCase;

	@Autowired
	private GetDiscardedIngredientLogsUseCase discardedUseCase;

	@BeforeEach
	void setUpUsers() {
		me = createUser();
		other = createUser();
	}

	private Long ingredient(Long userId, String category, String name, Long price) {
		return createIngredient(userId, category, name, TODAY.plusDays(5), price);
	}

	private void consume(Long userId, Long id, IngredientLogType type, String amount, ConsumeVia via) {
		consumeUseCase.consumeIngredient(new ConsumeIngredientCommand(userId, id, type, new BigDecimal(amount), via));
	}

	private String logCategory() {
		return jdbcTemplate.queryForObject("SELECT c.code FROM ingredient_logs l JOIN ingredient_categories c "
				+ "ON c.id = l.category_id", String.class);
	}

	@Test
	void 기록한_뒤_재료의_카테고리를_고쳐도_기록의_카테고리는_그대로다() {
		Long id = ingredient(me, "DAIRY_EGG", "우유", null);
		jdbcTemplate.update("UPDATE ingredients SET amount = 4, initial_amount = 4 WHERE id = ?", id);
		consume(me, id, IngredientLogType.USED, "1", ConsumeVia.CONSUME);

		jdbcTemplate.update("UPDATE ingredients SET category_id = ? WHERE id = ?", categoryId("BEVERAGE"), id);

		assertThat(logCategory()).isEqualTo("DAIRY_EGG");
	}

	@Test
	void 전부_버려_재료가_지워진_뒤에도_카테고리가_나온다() {
		Long id = ingredient(me, "MEAT", "삼겹살", 8000L);
		consume(me, id, IngredientLogType.DISCARDED, "1", ConsumeVia.CONSUME);

		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM ingredients WHERE id = ?", Long.class, id)).isZero();
		List<DiscardedIngredientLog> logs = discardedUseCase.getDiscardedLogs(me, TODAY.atStartOfDay(),
				TODAY.plusDays(1).atStartOfDay());
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).name()).isEqualTo("삼겹살");
		assertThat(logs.get(0).categoryCode()).isEqualTo(IngredientCategoryCode.MEAT);
		assertThat(logs.get(0).costAmount()).isEqualTo(8000L);
	}

	@Test
	void 요리_차감_기록에도_카테고리가_들어간다() {
		Long id = ingredient(me, "VEGETABLE", "양파", null);
		consume(me, id, IngredientLogType.USED, "1", ConsumeVia.COOK);

		assertThat(logCategory()).isEqualTo("VEGETABLE");
	}

	@Test
	void 가격이_없는_기록은_금액이_null이고_사용_기록과_다른_회원_기록은_빠진다() {
		Long noPrice = ingredient(me, "FRUIT", "사과", null);
		Long used = ingredient(me, "FRUIT", "배", 3000L);
		Long others = ingredient(other, "FRUIT", "귤", 3000L);
		consume(me, noPrice, IngredientLogType.DISCARDED, "1", ConsumeVia.CONSUME);
		consume(me, used, IngredientLogType.USED, "1", ConsumeVia.CONSUME);
		consume(other, others, IngredientLogType.DISCARDED, "1", ConsumeVia.CONSUME);

		List<DiscardedIngredientLog> logs = discardedUseCase.getDiscardedLogs(me, TODAY.atStartOfDay(),
				TODAY.plusDays(1).atStartOfDay());

		assertThat(logs).extracting(DiscardedIngredientLog::name).containsExactly("사과");
		assertThat(logs.get(0).costAmount()).isNull();
	}

	@Test
	void 기간은_시작_이상_끝_미만이고_최근에_버린_것이_위에_온다() {
		Long a = ingredient(me, "FRUIT", "사과", null);
		Long b = ingredient(me, "FRUIT", "배", null);
		Long c = ingredient(me, "FRUIT", "귤", null);
		Long d = ingredient(me, "FRUIT", "감", null);
		for (Long id : List.of(a, b, c, d)) {
			consume(me, id, IngredientLogType.DISCARDED, "1", ConsumeVia.CONSUME);
		}
		LocalDateTime from = LocalDate.of(2026, 3, 1).atStartOfDay();
		LocalDateTime to = LocalDate.of(2026, 4, 1).atStartOfDay();
		// 사과 = 지난달 마지막 순간(제외), 배 = 이번 달 시작 정각(포함), 귤 = 이번 달 중간, 감 = 다음 달 시작 정각(제외)
		List<LocalDateTime> times = List.of(from.minusSeconds(1), from, LocalDateTime.of(2026, 3, 15, 12, 0), to);
		List<Long> logIds = jdbcTemplate.queryForList("SELECT id FROM ingredient_logs ORDER BY id", Long.class);
		for (int i = 0; i < 4; i++) {
			jdbcTemplate.update("UPDATE ingredient_logs SET created_at = ? WHERE id = ?", times.get(i), logIds.get(i));
		}

		List<DiscardedIngredientLog> logs = discardedUseCase.getDiscardedLogs(me, from, to);

		assertThat(logs).extracting(DiscardedIngredientLog::name).containsExactly("귤", "배");
		assertThat(logs.get(0).discardedAt()).isEqualTo(LocalDateTime.of(2026, 3, 15, 12, 0));
	}

	@Test
	void 배포_중_구버전이_카테고리_없이_쓴_기록도_목록에_나오고_카테고리만_비어_있다() {
		// V22는 NOT NULL을 걸지 않는다. 구버전은 category_id 없이 INSERT한다
		jdbcTemplate.update("""
				INSERT INTO ingredient_logs (user_id, ingredient_id, type, name, amount, unit, cost_amount, created_at)
				VALUES (?, NULL, 'DISCARDED', '상추', 1, 'EA', 1500, ?)
				""", me, TODAY.atTime(9, 0));

		List<DiscardedIngredientLog> logs = discardedUseCase.getDiscardedLogs(me, TODAY.atStartOfDay(),
				TODAY.plusDays(1).atStartOfDay());

		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).name()).isEqualTo("상추");
		assertThat(logs.get(0).categoryCode()).isNull();
		assertThat(logs.get(0).costAmount()).isEqualTo(1500L);
	}
}
