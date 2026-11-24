package com.remake.cart.adapter.in.web;

import java.math.BigDecimal;

import com.remake.recipe.domain.RecipeUnit;

public record CartAmountResponse(Long id, BigDecimal amount, RecipeUnit unit) { }
