package com.remake.mealplan.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.remake.mealplan.domain.MealType;

// 끼니 목록 ↔ 문자열 변환: Spring 없이 확인한다.
class MealTypesConverterTest {

	private final MealTypesConverter converter = new MealTypesConverter();

	@Test
	void 고른_순서와_관계없이_아침_점심_저녁_순으로_저장한다() {
		LinkedHashSet<MealType> picked = new LinkedHashSet<>(List.of(MealType.DINNER, MealType.BREAKFAST));

		assertThat(converter.convertToDatabaseColumn(picked)).isEqualTo("BREAKFAST,DINNER");
		assertThat(converter.convertToDatabaseColumn(EnumSet.allOf(MealType.class)))
				.isEqualTo("BREAKFAST,LUNCH,DINNER");
	}

	@Test
	void 저장한_문자열을_끼니_목록으로_되돌린다() {
		assertThat(converter.convertToEntityAttribute("LUNCH")).containsExactly(MealType.LUNCH);
		assertThat(converter.convertToEntityAttribute("BREAKFAST,DINNER"))
				.containsExactly(MealType.BREAKFAST, MealType.DINNER);
	}
}
