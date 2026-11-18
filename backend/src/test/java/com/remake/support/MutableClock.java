package com.remake.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 테스트에서 현재 시각을 바꿀 수 있는 시계. 기본은 한국 시간 2026-03-04(수) 10:00.
 * 기본값을 실제 날짜와 먼 지난 날짜로 둔다. 실제 시계를 읽는 코드가 섞이면 날짜가 몇 달씩 어긋나 테스트에서 드러난다.
 */
public class MutableClock extends Clock {

	public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
	public static final Instant DEFAULT = ZonedDateTime.of(2026, 3, 4, 10, 0, 0, 0, ZONE).toInstant();

	// withZone으로 만든 시계도 같은 시각을 보게 공유한다. 다른 스레드에서도 바뀐 값이 보인다.
	private final AtomicReference<Instant> now;
	private final ZoneId zone;

	public MutableClock() {
		this(new AtomicReference<>(DEFAULT), ZONE);
	}

	private MutableClock(AtomicReference<Instant> now, ZoneId zone) {
		this.now = now;
		this.zone = zone;
	}

	public void setNow(ZonedDateTime time) {
		now.set(time.toInstant());
	}

	public void reset() {
		now.set(DEFAULT);
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new MutableClock(now, zone);
	}

	@Override
	public Instant instant() {
		return now.get();
	}
}
