package com.remake.event.domain;

/** 이벤트 발송 주체. 정의서 Source 열과 같다. 같은 행동은 CLIENT 또는 SERVER 한쪽에만 기록한다. */
public enum EventSource {
	CLIENT,
	SERVER
}
