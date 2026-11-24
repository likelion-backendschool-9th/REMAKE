package com.remake.share.adapter.in.web;

import com.remake.share.domain.SharedRecipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PublishSharedRecipeRequest(
		@NotNull @Positive Long recipeId,
		@NotBlank @Size(max = SharedRecipe.TITLE_MAX_LENGTH, message = "공유 제목은 {max}자 이내로 입력해 주세요.") String title) {
}
