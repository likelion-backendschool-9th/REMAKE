package com.remake.share.application.port.in;

import com.remake.share.domain.SharedRecipeReportReason;

public interface ReportSharedRecipeUseCase {
	void report(Long userId, Long sharedRecipeId, SharedRecipeReportReason reason);
}
