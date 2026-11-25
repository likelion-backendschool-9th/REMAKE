package com.remake.share.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.in.GetSharedRecipePhotoUseCase;
import com.remake.share.application.port.out.LoadSharedRecipePort;
import com.remake.share.application.port.out.LoadSharedRecipePhotoPort;
import com.remake.share.application.port.out.SharedRecipePhotoContent;
import com.remake.share.domain.SharedRecipe;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
class SharedRecipePhotoQueryService implements GetSharedRecipePhotoUseCase {
	private final LoadSharedRecipePort sharedRecipes;
	private final LoadSharedRecipePhotoPort photos;

	@Override
	@Transactional(readOnly = true)
	public SharedRecipePhotoContent getPhoto(Long id) {
		SharedRecipe post = sharedRecipes.findById(id)
				.filter(p -> p.status() == SharedRecipe.Status.VISIBLE)
				.orElseThrow(() -> new BusinessException(ErrorCode.SHARED_RECIPE_NOT_FOUND));
		return photos.load(post.imageKey())
				.orElseThrow(() -> new BusinessException(ErrorCode.SHARED_RECIPE_NOT_FOUND));
	}
}
