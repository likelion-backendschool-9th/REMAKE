package com.remake.ingredient.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.remake.ingredient.domain.ExpiredIngredient;
import com.remake.ingredient.domain.ExpiredIngredientCount;
import com.remake.ingredient.domain.ExpiringIngredient;
import com.remake.ingredient.domain.StorageLocation;

/**
 * 알림 배치가 부르는 조회는 엔티티를 메모리에 올리지 않고 필요한 값만 읽는다(재료가 많아져도 열 몇 개만 읽는다).
 */
interface IngredientJpaRepository extends JpaRepository<IngredientJpaEntity, Long> {

	/** 사용자 본인의 재료만 읽는다. 다른 사용자의 재료는 없는 것과 같다. */
	Optional<IngredientJpaEntity> findByIdAndUserId(Long id, Long userId);

	@Query("""
			select i from IngredientJpaEntity i
			where i.userId = :userId
			  and (:storage is null or i.storage = :storage)
			  and (:categoryId is null or i.categoryId = :categoryId)
			order by i.expirationDate, i.id
			""")
	List<IngredientJpaEntity> search(@Param("userId") Long userId,
			@Param("storage") StorageLocation storage, @Param("categoryId") Long categoryId);

	@Query("select i.expirationDate from IngredientJpaEntity i where i.userId = :userId")
	List<LocalDate> findExpirationDates(@Param("userId") Long userId);

	@Query("""
			select new com.remake.ingredient.domain.ExpiringIngredient(i.userId, i.id, i.name, i.expirationDate)
			from IngredientJpaEntity i
			where i.expirationDate between :from and :to
			order by i.userId, i.expirationDate, i.id
			""")
	List<ExpiringIngredient> findExpiringBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query("""
			select new com.remake.ingredient.domain.ExpiredIngredientCount(i.userId, count(i))
			from IngredientJpaEntity i
			where i.expirationDate < :date
			group by i.userId
			order by i.userId
			""")
	List<ExpiredIngredientCount> countByUserExpiredBefore(@Param("date") LocalDate date);

	// 카테고리 code는 같은 기능의 기준 데이터 테이블과 묶어서 한 번에 읽는다.
	@Query("""
			select new com.remake.ingredient.domain.ExpiredIngredient(i.userId, i.id, c.code,
					case when i.price is null then false else true end)
			from IngredientJpaEntity i
			join IngredientCategoryJpaEntity c on c.id = i.categoryId
			where i.expirationDate = :date
			order by i.userId, i.id
			""")
	List<ExpiredIngredient> findExpiredOn(@Param("date") LocalDate date);
}
