package com.remake.share.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.remake.share.domain.SharedRecipeReportReason;

import jakarta.validation.constraints.NotNull;

public record ReportSharedRecipeRequest(@NotNull SharedRecipeReportReason reason) {
	/** OTHER도 자유 입력 필드를 받지 않는다. 알 수 없는 필드는 JSON 입력 오류(400)로 처리한다. */
	@JsonAnySetter
	public void rejectExtraField(String name, Object value) {
		throw new IllegalArgumentException("신고 사유 외의 입력은 받지 않습니다.");
	}
}
