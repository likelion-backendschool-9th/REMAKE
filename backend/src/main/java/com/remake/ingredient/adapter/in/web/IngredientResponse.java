package com.remake.ingredient.adapter.in.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.remake.ingredient.domain.ExpiryStatus;
import com.remake.ingredient.domain.FridgeIngredient;
import com.remake.ingredient.domain.Ingredient;
import com.remake.ingredient.domain.IngredientDetail;
import com.remake.ingredient.domain.IngredientCategoryCode;
import com.remake.ingredient.domain.IngredientUnit;
import com.remake.ingredient.domain.StorageLocation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 재료 한 건 응답(API 13·15·16). 목록 한 줄(API 12)에 price를 더한 모양이다.
 * imageUrl은 카테고리 이미지이고, dDay는 오늘부터 소비기한까지 남은 일수(지났으면 음수), status는 DANGER/SOON/OK이다.
 * unitFollowsName이 true면 이름이나 카테고리를 고칠 때 단위도 다시 정해지므로 화면이 단위 조회(API 14)로 미리 볼 수 있고,
 * false면 장바구니·영수증 예외로 들어온 단위라 수정해도 지금 단위가 그대로다(#233).
 * 도메인 객체를 그대로 내보내지 않고 이 모양으로 바꾼다. enum은 이름 그대로 나간다.
 */
public record IngredientResponse(
		Long id,
		String name,
		IngredientCategoryCode categoryCode,
		String imageUrl,
		StorageLocation storage,
		BigDecimal amount,
		IngredientUnit unit,
		LocalDate expirationDate,
		long dDay,
		ExpiryStatus status,
		@Schema(nullable = true) Long price,
		boolean unitFollowsName) {

	public static IngredientResponse from(IngredientDetail detail) {
		FridgeIngredient item = detail.fridgeIngredient();
		Ingredient i = item.ingredient();
		return new IngredientResponse(i.id(), i.name(), item.category().code(), item.category().imageUrl(),
				i.storage(), i.amount(), i.unit(), i.expirationDate(), item.dDay(), item.status(), i.price(),
				detail.unitFollowsName());
	}
}
