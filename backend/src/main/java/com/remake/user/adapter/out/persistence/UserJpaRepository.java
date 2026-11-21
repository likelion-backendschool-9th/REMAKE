package com.remake.user.adapter.out.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.remake.user.domain.AuthProvider;

interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

	Optional<UserJpaEntity> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
