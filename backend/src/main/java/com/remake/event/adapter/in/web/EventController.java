package com.remake.event.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.remake.event.application.port.in.IngestClientEventsUseCase;
import com.remake.global.config.OpenApiConfig;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 프론트 이벤트 배치 수집. 로그인 사용자 API(#66 이후 Security가 userId를 채움).
 * 지금은 Security가 없어 userId를 null로 저장한다.
 */
@Tag(name = "이벤트")
@SecurityRequirement(name = OpenApiConfig.COOKIE_AUTH)
@RestController
@RequestMapping("/api/v1/user/events")
@RequiredArgsConstructor
public class EventController {

	public static final String HEADER_PLATFORM = "X-Client-Platform";
	public static final String HEADER_APP_VERSION = "X-App-Version";

	private final IngestClientEventsUseCase ingestClientEventsUseCase;

	@Operation(summary = "클라이언트 이벤트 배치 수집", description = "최대 50개. 모르는 이름은 rejected. 202 { accepted, rejected }")
	@PostMapping
	@ResponseStatus(HttpStatus.ACCEPTED)
	public EventIngestResponse ingest(
			@Valid @RequestBody EventIngestRequest request,
			@RequestHeader(value = HEADER_PLATFORM, required = false) String platform,
			@RequestHeader(value = HEADER_APP_VERSION, required = false) String appVersion) {
		var commands = request.events().stream()
				.map(item -> new IngestClientEventsUseCase.ClientEventCommand(
						item.name(),
						item.screen(),
						item.properties(),
						item.occurredAt(),
						item.sessionId()))
				.toList();
		// TODO(#66): SecurityContext에서 로그인 userId를 넣어 전달한다.
		return EventIngestResponse.from(ingestClientEventsUseCase.ingest(null, platform, appVersion, commands));
	}
}
