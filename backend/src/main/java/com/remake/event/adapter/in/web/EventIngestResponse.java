package com.remake.event.adapter.in.web;

import com.remake.event.application.port.in.IngestClientEventsUseCase;

/** 202 응답. 수락·거절 개수만 돌려준다. */
public record EventIngestResponse(int accepted, int rejected) {

	static EventIngestResponse from(IngestClientEventsUseCase.IngestResult result) {
		return new EventIngestResponse(result.accepted(), result.rejected());
	}
}
