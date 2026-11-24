package com.remake.share.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PublishSharedRecipeRequest(@NotNull @Positive Long recipeId, @NotBlank String title) {
}
