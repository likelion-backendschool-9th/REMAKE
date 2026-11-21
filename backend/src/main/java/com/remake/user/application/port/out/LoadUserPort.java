package com.remake.user.application.port.out;

import java.util.Optional;

import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.User;

public interface LoadUserPort {

	Optional<User> findById(Long id);

	Optional<User> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);
}
