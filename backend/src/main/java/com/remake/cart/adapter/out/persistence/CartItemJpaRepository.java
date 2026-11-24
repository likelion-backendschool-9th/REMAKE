package com.remake.cart.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface CartItemJpaRepository extends JpaRepository<CartItemJpaEntity, Long> {
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from CartItemJpaEntity c where c.userId = :userId and c.id = :id")
	Optional<CartItemJpaEntity> findOwnedForUpdate(@Param("userId") Long userId, @Param("id") Long id);
}
