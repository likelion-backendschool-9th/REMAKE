package com.remake.global.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * GlobalExceptionHandlerTest에서만 쓰는 Controller. 테스트 클래스 경로(src/test)에만 있어서 운영 앱에는 들어가지 않는다.
 * 테스트 클래스 안에 두면 Spring 테스트가 컴포넌트 검색에서 빼므로 별도 파일로 둔다.
 */
@RestController
@RequestMapping("/test/errors")
class ErrorTestController {

	record CreateRequest(@NotBlank String name, @Min(1) int amount) {
	}

	@GetMapping("/business")
	void business() {
		throw new BusinessException(ErrorCode.CONCURRENT_UPDATE);
	}

	@GetMapping("/business-message")
	void businessMessage() {
		throw new BusinessException(ErrorCode.INVALID_INPUT, "남은 수량은 300g입니다.");
	}

	@PostMapping("/body")
	void body(@Valid @RequestBody CreateRequest request) {
	}

	@GetMapping("/path/{amount}")
	void path(@PathVariable @Min(1) int amount) {
	}

	@GetMapping("/unexpected")
	void unexpected() {
		throw new IllegalStateException("내부 정보: DB 비밀번호가 틀림");
	}

	@GetMapping("/header")
	void header(@RequestHeader("X-Required") String value) {
	}

	@PostMapping("/part")
	void part(@RequestPart("file") MultipartFile file) {
	}

	@GetMapping("/cookie")
	void cookie(@CookieValue("refresh_token") String refreshToken) {
	}

	@GetMapping("/status")
	void status() {
		throw new ResponseStatusException(HttpStatus.NOT_FOUND, "내부 사유");
	}
}
