package com.remake.global.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.remake.auth.application.port.out.JwtTokenPort;
import com.remake.global.config.AuthProperties;
import com.remake.global.error.BusinessException;
import com.remake.global.error.ErrorCode;
import com.remake.user.domain.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider implements JwtTokenPort {

	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_TYPE = "type";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";

	private final AuthProperties authProperties;
	private final Clock clock;
	private final SecretKey secretKey;

	public JwtTokenProvider(AuthProperties authProperties, Clock clock) {
		this.authProperties = authProperties;
		this.clock = clock;
		// 키가 짧으면 여기서 WeakKeyException → 앱 시작 실패(첫 로그인 500을 막는다).
		this.secretKey = Keys.hmacShaKeyFor(authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public IssuedTokens issue(Long userId, UserRole role) {
		Instant now = clock.instant();
		Instant accessExpires = now.plusSeconds(authProperties.accessTokenTtlMinutes() * 60);
		Instant refreshExpires = now.plusSeconds(authProperties.refreshTokenTtlDays() * 24 * 60 * 60);
		String access = buildToken(userId, role, TYPE_ACCESS, now, accessExpires);
		String refresh = buildToken(userId, role, TYPE_REFRESH, now, refreshExpires);
		return new IssuedTokens(access, refresh, refreshExpires);
	}

	@Override
	public String issueAccessToken(Long userId, UserRole role) {
		Instant now = clock.instant();
		Instant accessExpires = now.plusSeconds(authProperties.accessTokenTtlMinutes() * 60);
		return buildToken(userId, role, TYPE_ACCESS, now, accessExpires);
	}

	@Override
	public AccessClaims parseAccessToken(String token) {
		Claims claims = parse(token);
		requireType(claims, TYPE_ACCESS);
		return new AccessClaims(Long.valueOf(claims.getSubject()), roleOf(claims), claims.getExpiration().toInstant());
	}

	@Override
	public Long parseRefreshUserId(String token) {
		Claims claims = parse(token);
		requireType(claims, TYPE_REFRESH);
		return Long.valueOf(claims.getSubject());
	}

	private String buildToken(Long userId, UserRole role, String type, Instant issuedAt, Instant expiresAt) {
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(String.valueOf(userId))
				.claim(CLAIM_ROLE, role.name())
				.claim(CLAIM_TYPE, type)
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.signWith(secretKey)
				.compact();
	}

	private Claims parse(String token) {
		try {
			return Jwts.parser()
					.verifyWith(secretKey)
					.clock(() -> Date.from(this.clock.instant()))
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (ExpiredJwtException ex) {
			throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
		} catch (RuntimeException ex) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}

	private void requireType(Claims claims, String expected) {
		if (!expected.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}

	private UserRole roleOf(Claims claims) {
		return UserRole.valueOf(claims.get(CLAIM_ROLE, String.class));
	}
}
