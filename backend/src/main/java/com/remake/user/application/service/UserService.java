package com.remake.user.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.user.application.port.in.ChangeUserPlanUseCase;
import com.remake.user.application.port.in.FindOrCreateSocialUserUseCase;
import com.remake.user.application.port.in.GetUserUseCase;
import com.remake.user.application.port.out.LoadUserPort;
import com.remake.user.application.port.out.SaveUserPort;
import com.remake.user.domain.AuthProvider;
import com.remake.user.domain.User;
import com.remake.user.domain.UserPlan;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService implements FindOrCreateSocialUserUseCase, GetUserUseCase, ChangeUserPlanUseCase {

	private final LoadUserPort loadUserPort;
	private final SaveUserPort saveUserPort;

	@Override
	public Result findOrCreate(AuthProvider provider, String providerUserId, String email) {
		return loadUserPort.findByProviderAndProviderUserId(provider, providerUserId)
				.map(user -> new Result(user, false))
				.orElseGet(() -> new Result(saveUserPort.save(User.signUp(provider, providerUserId, email)), true));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> getUser(Long id) {
		return loadUserPort.findById(id);
	}

	@Override
	public void changePlan(Long userId, UserPlan plan) {
		User user = loadUserPort.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		if (user.plan() != plan) {
			saveUserPort.save(user.withPlan(plan));
		}
	}
}
