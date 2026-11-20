package com.remake.global.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** JacksonConfigTest에서만 쓰는 Controller. 테스트 클래스 경로(src/test)에만 있어 운영 앱에는 들어가지 않는다. */
@RestController
@RequestMapping("/test/time")
class TimeTestController {

	record TimeBody(LocalDateTime at, LocalDate date) {
	}

	@GetMapping
	TimeBody fixed() {
		return new TimeBody(LocalDateTime.of(2026, 11, 15, 9, 0, 0, 123_456_000), LocalDate.of(2026, 12, 21));
	}

	// 받은 값을 그대로 돌려준다
	@PostMapping
	TimeBody echo(@RequestBody TimeBody body) {
		return body;
	}
}
