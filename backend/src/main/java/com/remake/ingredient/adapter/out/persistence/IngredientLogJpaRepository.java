package com.remake.ingredient.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.remake.ingredient.domain.DiscardedIngredientLog;

interface IngredientLogJpaRepository extends JpaRepository<IngredientLogJpaEntity, Long> {

	boolean existsByIngredientId(Long ingredientId);

	void deleteByUserId(Long userId);

	// left join: 배포 중 구버전이 쓴 기록은 category_id가 비어 있을 수 있다(V22는 NOT NULL을 걸지 않음). 이런 기록도
	// 버린 목록·합계에서 빠지면 안 되므로 카테고리만 null로 돌려준다.
	@Query("""
			select new com.remake.ingredient.domain.DiscardedIngredientLog(l.name, c.code, l.createdAt, l.costAmount)
			from IngredientLogJpaEntity l
			left join IngredientCategoryJpaEntity c on c.id = l.categoryId
			where l.userId = :userId and l.type = com.remake.ingredient.domain.IngredientLogType.DISCARDED
			  and l.createdAt >= :from and l.createdAt < :to
			order by l.createdAt desc, l.id desc
			""")
	List<DiscardedIngredientLog> findDiscardedBetween(@Param("userId") Long userId, @Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);
}
