package com.remake.event.application.port.in;

/**
 * 서버 이벤트 공통 필드. platform·appVersion은 요청 헤더(X-Client-Platform, X-App-Version)에서 채운다.
 * sessionId가 없으면 null로 저장한다.
 */
public record EventTrackCommand(
		Long userId,
		String sessionId,
		String platform,
		String appVersion,
		boolean seed) {

	public static EventTrackCommand ofUser(Long userId) {
		return new EventTrackCommand(userId, null, null, null, false);
	}

	public static EventTrackCommand of(Long userId, String sessionId, String platform, String appVersion) {
		return new EventTrackCommand(userId, sessionId, platform, appVersion, false);
	}
}
