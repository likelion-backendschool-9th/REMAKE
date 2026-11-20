package com.remake.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * JSON 시각 표기. 코드(엔티티·DTO)는 LocalDateTime(한국 시간)을 쓰고, JSON에는 시간대를 붙여 2026-11-15T09:00:00+09:00으로 내보낸다.
 * 요청으로 받을 때는 시간대가 붙은 값을 한국 시간으로 바꾸고, 시간대가 없으면 한국 시간으로 본다.
 * 시간대는 ClockConfig의 Clock(Asia/Seoul)을 따른다. 날짜만 있는 LocalDate(2026-12-21)는 바꾸지 않는다.
 * Spring Boot가 JacksonModule 빈을 JSON 변환기에 자동으로 등록한다.
 */
@Configuration
public class JacksonConfig {

	@Bean
	public SimpleModule koreaTimeModule(Clock clock) {
		ZoneId zone = clock.getZone();
		return new SimpleModule("koreaTimeModule")
				.addSerializer(LocalDateTime.class, new KoreaTimeSerializer(zone))
				.addDeserializer(LocalDateTime.class, new KoreaTimeDeserializer(zone));
	}

	static class KoreaTimeSerializer extends ValueSerializer<LocalDateTime> {

		private final ZoneId zone;

		KoreaTimeSerializer(ZoneId zone) {
			this.zone = zone;
		}

		// 초 아래(DB의 마이크로초)는 버린다: 2026-11-15T09:00:00+09:00
		@Override
		public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext ctxt) {
			OffsetDateTime withOffset = value.truncatedTo(ChronoUnit.SECONDS).atZone(zone).toOffsetDateTime();
			gen.writeString(withOffset.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		}
	}

	static class KoreaTimeDeserializer extends ValueDeserializer<LocalDateTime> {

		private final ZoneId zone;

		KoreaTimeDeserializer(ZoneId zone) {
			this.zone = zone;
		}

		@Override
		public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) {
			String text = p.getValueAsString();
			if (text == null || text.isBlank()) {
				return null;
			}
			try {
				TemporalAccessor parsed = DateTimeFormatter.ISO_DATE_TIME.parseBest(text.trim(),
						OffsetDateTime::from, LocalDateTime::from);
				if (parsed instanceof OffsetDateTime withOffset) {
					return withOffset.atZoneSameInstant(zone).toLocalDateTime();
				}
				return (LocalDateTime) parsed;
			} catch (DateTimeParseException e) {
				// 형식이 틀리면 Jackson 오류로 바꿔 400 INVALID_INPUT이 되게 한다.
				return (LocalDateTime) ctxt.handleWeirdStringValue(LocalDateTime.class, text,
						"시각 형식이 아닙니다. 예: 2026-11-15T09:00:00+09:00");
			}
		}
	}
}
