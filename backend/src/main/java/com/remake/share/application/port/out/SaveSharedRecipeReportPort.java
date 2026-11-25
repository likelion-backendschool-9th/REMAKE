package com.remake.share.application.port.out;

import com.remake.share.domain.SharedRecipeReportReason;

public interface SaveSharedRecipeReportPort {
	void save(Long userId, Long sharedRecipeId, SharedRecipeReportReason reason);
}
