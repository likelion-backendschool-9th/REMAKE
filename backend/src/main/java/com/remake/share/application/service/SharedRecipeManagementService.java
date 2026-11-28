package com.remake.share.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.remake.global.common.PageResponse;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.share.application.port.in.DeleteSharedRecipeUseCase;
import com.remake.share.application.port.in.GetMySharedRecipesUseCase;
import com.remake.share.application.port.out.ManageSharedRecipePort;
import com.remake.share.application.port.out.SharedRecipePhotoDeletionPort;
import com.remake.share.domain.SharedRecipe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
class SharedRecipeManagementService implements DeleteSharedRecipeUseCase, GetMySharedRecipesUseCase {
	private final ManageSharedRecipePort posts;
	private final SharedRecipePhotoDeletionPort pending;
	private final SharedRecipePhotoDeletionService cleanup;
	private final Clock clock;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<Item> getMine(Long userId, int page, int size) {
		if (userId == null || userId <= 0 || page < 0 || size < 1 || size > 100) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		return PageResponse.from(posts.loadMine(userId, page, size), post -> new Item(post.id(), post.title(),
				post.status() == SharedRecipe.Status.VISIBLE ? "/api/v1/user/shared-recipe-photos/" + post.id() : null,
				post.status(), post.createdAt(), true));
	}

	@Override
	@Transactional
	public void delete(Long userId, Long id) {
		if (userId == null || userId <= 0 || id == null || id <= 0) {
			throw new BusinessException(ErrorCode.INVALID_INPUT);
		}
		posts.lockDeletionAndSettlement();
		SharedRecipe post = posts.lockOwned(userId, id)
				.orElseThrow(() -> new BusinessException(ErrorCode.SHARED_RECIPE_NOT_FOUND));
		if (post.status() == SharedRecipe.Status.DELETED) {
			return;
		}
		String key = post.imageKey();
		pending.enqueue(key);
		posts.markDeleted(id, LocalDateTime.now(clock));
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					cleanup.deleteAfterCommit(key);
				} catch (RuntimeException failure) {
					log.warn("공유 사진 정리 대기 유지: {}", key);
				}
			}
		});
	}
}
