package com.remake.auth.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.remake.auth.application.port.in.LogoutUseCase;
import com.remake.auth.application.port.in.RefreshAccessTokenUseCase;
import com.remake.auth.application.port.in.SocialLoginUseCase;
import com.remake.auth.application.port.out.JwtTokenPort;
import com.remake.auth.application.port.out.LoadRefreshTokenPort;
import com.remake.auth.application.port.out.OAuthClientPort;
import com.remake.auth.application.port.out.SaveRefreshTokenPort;
import com.remake.auth.application.port.out.TokenHasherPort;
import com.remake.auth.domain.RefreshToken;
import com.remake.auth.domain.SocialProfile;
import com.remake.event.application.port.in.EventTrackCommand;
import com.remake.event.application.port.in.EventTracker;
import com.remake.event.domain.EventName;
import com.remake.event.domain.Screen;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.user.application.port.in.FindOrCreateSocialUserUseCase;
import com.remake.user.application.port.in.GetUserUseCase;
import com.remake.user.domain.User;

import lombok.RequiredArgsConstructor;

/**
 * 로그인 성공 이벤트는 커밋 뒤에 남기고, login_failed는 트랜잭션 롤백 밖에서 남긴다.
 * 리프레시 토큰은 로테이션하지 않는다. 액세스만 다시 발급하고, 폐기는 로그아웃·계정 삭제 때만 한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService implements SocialLoginUseCase, RefreshAccessTokenUseCase, LogoutUseCase {

	private final OAuthClientPort oAuthClientPort;
	private final FindOrCreateSocialUserUseCase findOrCreateSocialUserUseCase;
	private final GetUserUseCase getUserUseCase;
	private final JwtTokenPort jwtTokenPort;
	private final TokenHasherPort tokenHasherPort;
	private final LoadRefreshTokenPort loadRefreshTokenPort;
	private final SaveRefreshTokenPort saveRefreshTokenPort;
	private final EventTracker eventTracker;
	private final Clock clock;
	private final TransactionTemplate transactionTemplate;

	@Override
	public SocialLoginUseCase.Result login(SocialLoginUseCase.Command command) {
		SocialProfile profile;
		try {
			profile = oAuthClientPort.exchange(command.provider(), command.code(), command.redirectUri(),
					command.state());
		} catch (BusinessException ex) {
			trackLoginFailed(command.provider().wireName(), ex.getErrorCode().name());
			throw ex;
		} catch (RuntimeException ex) {
			trackLoginFailed(command.provider().wireName(), ErrorCode.OAUTH_FAILED.name());
			throw new BusinessException(ErrorCode.OAUTH_FAILED);
		}

		try {
			return transactionTemplate.execute(status -> completeLogin(command, profile));
		} catch (BusinessException ex) {
			trackLoginFailed(command.provider().wireName(), ex.getErrorCode().name());
			throw ex;
		}
	}

	private SocialLoginUseCase.Result completeLogin(SocialLoginUseCase.Command command, SocialProfile profile) {
		FindOrCreateSocialUserUseCase.Result userResult = findOrCreateSocialUserUseCase.findOrCreate(
				profile.provider(), profile.providerUserId(), profile.email());
		User user = userResult.user();
		if (user.isSuspended()) {
			throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
		}
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.OAUTH_FAILED);
		}

		JwtTokenPort.IssuedTokens tokens = jwtTokenPort.issue(user.id(), user.role());
		saveRefreshTokenPort.save(RefreshToken.issue(user.id(), tokenHasherPort.hash(tokens.refreshToken()),
				LocalDateTime.ofInstant(tokens.refreshExpiresAt(), clock.getZone())));

		if (userResult.newUser()) {
			eventTracker.track(EventName.SIGNUP_COMPLETED, Screen.LOGIN,
					Map.of("provider", command.provider().wireName()),
					EventTrackCommand.ofUser(user.id()));
		}
		eventTracker.track(EventName.LOGIN_SUCCEEDED, Screen.LOGIN,
				Map.of("provider", command.provider().wireName(), "is_new_user", userResult.newUser()),
				EventTrackCommand.ofUser(user.id()));

		return new SocialLoginUseCase.Result(user.onboardingCompleted(), tokens.accessToken(), tokens.refreshToken(),
				tokens.refreshExpiresAt());
	}

	@Override
	@Transactional
	public RefreshAccessTokenUseCase.Result refresh(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
		}
		Long userId;
		try {
			userId = jwtTokenPort.parseRefreshUserId(refreshToken);
		} catch (BusinessException ex) {
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
		}
		RefreshToken stored = loadRefreshTokenPort.findByTokenHash(tokenHasherPort.hash(refreshToken))
				.orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
		LocalDateTime now = LocalDateTime.now(clock);
		if (!stored.isValid(now) || !stored.userId().equals(userId)) {
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
		}
		User user = getUserUseCase.getUser(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID));
		if (user.isSuspended()) {
			throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
		}
		if (!user.isActive()) {
			throw new BusinessException(ErrorCode.REFRESH_TOKEN_INVALID);
		}
		// 로테이션하지 않음: 액세스 쿠키만 다시 발급하고 리프레시 행·쿠키는 유지한다.
		return new RefreshAccessTokenUseCase.Result(jwtTokenPort.issueAccessToken(user.id(), user.role()));
	}

	@Override
	@Transactional
	public void logout(Long userId, String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}
		loadRefreshTokenPort.findByTokenHash(tokenHasherPort.hash(refreshToken)).ifPresent(stored -> {
			if (stored.userId().equals(userId) && stored.revokedAt() == null) {
				saveRefreshTokenPort.save(stored.revoke(LocalDateTime.now(clock)));
			}
		});
	}

	private void trackLoginFailed(String provider, String errorCode) {
		eventTracker.track(EventName.LOGIN_FAILED, Screen.LOGIN,
				Map.of("provider", provider, "error_code", errorCode),
				EventTrackCommand.ofUser(null));
	}
}
