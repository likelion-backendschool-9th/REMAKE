package com.remake.mealplan.adapter.out.persistence;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.remake.mealplan.domain.MealType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * meal_plans.meal_types(varchar) ↔ 끼니 목록. ERD대로 콤마로 이은 문자열 하나에 저장한다(예: BREAKFAST,DINNER).
 * 저장할 때는 항상 선언 순서(아침·점심·저녁)로 정렬해 같은 끼니 조합이 같은 문자열이 되게 한다.
 */
@Converter
class MealTypesConverter implements AttributeConverter<Set<MealType>, String> {

	private static final String SEPARATOR = ",";

	@Override
	public String convertToDatabaseColumn(Set<MealType> mealTypes) {
		if (mealTypes == null) {
			return null;
		}
		// EnumSet은 선언 순서로 돈다
		return EnumSet.copyOf(mealTypes).stream().map(Enum::name).collect(Collectors.joining(SEPARATOR));
	}

	@Override
	public Set<MealType> convertToEntityAttribute(String column) {
		if (column == null) {
			return null;
		}
		return Arrays.stream(column.split(SEPARATOR))
				.map(MealType::valueOf)
				.collect(Collectors.toCollection(() -> EnumSet.noneOf(MealType.class)));
	}
}
