package com.remake.ingredient.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface IngredientCatalogJpaRepository extends JpaRepository<IngredientCatalogJpaEntity, Long> {

	// pattern은 항상 값이 있다(검색어가 없으면 %). 이스케이프 문자는 \이다.
	@Query("""
			select c from IngredientCatalogJpaEntity c
			where lower(c.name) like :pattern escape '\\'
			order by c.name
			""")
	List<IngredientCatalogJpaEntity> searchByName(@Param("pattern") String pattern);

	Optional<IngredientCatalogJpaEntity> findByName(String name);
}
