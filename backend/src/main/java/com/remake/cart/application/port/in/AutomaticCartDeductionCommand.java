package com.remake.cart.application.port.in;

import java.math.BigDecimal;

import com.remake.ingredient.domain.IngredientUnit;

/** 영수증 최종 등록 재료명·서버 결정 단위·실제 등록량. 대상 행은 서버가 선택한다. */
public record AutomaticCartDeductionCommand(String ingredientName, BigDecimal amount, IngredientUnit unit) {
}
