package com.remake.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("Asia/Seoul"));
	}

	// BaseCreatedEntity·BaseTimeEntity의 생성·수정 시각 (JpaConfig의 @EnableJpaAuditing이 이 이름으로 찾는다)
	@Bean
	public DateTimeProvider auditingDateTimeProvider(Clock clock) {
		return () -> Optional.of(LocalDateTime.now(clock));
	}

	// 요청 검증의 @Future, @Past 같은 날짜 비교
	@Bean
	public ValidationConfigurationCustomizer validationClock(Clock clock) {
		return configuration -> configuration.clockProvider(() -> clock);
	}
}
