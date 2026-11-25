package com.remake.recipe.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SavedRecipeJpaRepository extends JpaRepository<SavedRecipeJpaEntity, Long> {

	Optional<SavedRecipeJpaEntity> findByUserIdAndRecipeId(Long userId, Long recipeId);

	List<SavedRecipeJpaEntity> findByUserIdAndRecipeIdIn(Long userId, Collection<Long> recipeIds);

	long countByUserId(Long userId);

	/** 같은 회원이 동시에 저장해도 UK(user_id, recipe_id)로 한 행만 남는다. 넣었으면 1, 이미 있으면 0 */
	@Modifying
	@Query(value = """
			INSERT INTO saved_recipes (user_id, recipe_id, is_favorite, saved_at)
			VALUES (:userId, :recipeId, false, :savedAt)
			ON CONFLICT (user_id, recipe_id) DO NOTHING""", nativeQuery = true)
	int insertIfAbsent(@Param("userId") Long userId, @Param("recipeId") Long recipeId,
			@Param("savedAt") LocalDateTime savedAt);

	@Modifying
	@Query(value = """
			INSERT INTO saved_recipes (user_id, recipe_id, shared_recipe_id, is_favorite, saved_at)
			VALUES (:userId, :recipeId, :sharedRecipeId, false, :savedAt)
			ON CONFLICT (user_id, recipe_id) DO NOTHING""", nativeQuery = true)
	int insertSharedIfAbsent(@Param("userId") Long userId, @Param("recipeId") Long recipeId,
			@Param("sharedRecipeId") Long sharedRecipeId, @Param("savedAt") LocalDateTime savedAt);

	@Modifying
	@Query("delete from SavedRecipeJpaEntity s where s.userId = :userId and s.recipeId = :recipeId")
	int deleteByUserIdAndRecipeId(@Param("userId") Long userId, @Param("recipeId") Long recipeId);

	@Modifying
	@Query("update SavedRecipeJpaEntity s set s.favorite = :favorite where s.userId = :userId and s.recipeId = :recipeId")
	int updateFavorite(@Param("userId") Long userId, @Param("recipeId") Long recipeId,
			@Param("favorite") boolean favorite);

	/**
	 * 저장 목록. availableOnly면 재료 이름이 모두 names 안에 있는 레시피만 남긴다(재료가 하나도 names 밖에 없음).
	 * 정렬은 Pageable로 받는다.
	 */
	@Query("""
			select s from SavedRecipeJpaEntity s
			where s.userId = :userId
			  and (:favoriteOnly = false or s.favorite = true)
			  and (:availableOnly = false or not exists (
			        select ri.id from RecipeIngredientJpaEntity ri
			        where ri.recipeId = s.recipeId and ri.name not in :names))""")
	Page<SavedRecipeJpaEntity> findPage(@Param("userId") Long userId, @Param("favoriteOnly") boolean favoriteOnly,
			@Param("availableOnly") boolean availableOnly, @Param("names") Collection<String> names,
			Pageable pageable);
}
