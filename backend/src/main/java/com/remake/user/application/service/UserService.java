package com.remake.user.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.user.application.port.in.FindOrCreateSocialUserUseCase;
import com.remake.user.application.port.out.LoadUserPort;
import com.remake.user.application.port.out.SaveUserPort;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements FindOrCreateSocialUserUseCase {

	private final LoadUserPort loadUserPort;
	private final SaveUserPort saveUserPort;

	@Override
	public Result findOrCreate(AuthProvider provider, String providerUserId, String email) {
		return loadUserPort.findByProviderAndProviderUserId(provider, providerUserId)
				.map(user -> new Result(user, false))
				.orElseGet(() -> new Result(saveUserPort.save(User.signUp(provider, providerUserId, email)), true));
	}
}
