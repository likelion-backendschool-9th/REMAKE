package com.remake.quota.application.port.out;

public interface DeleteUsageLogsForUserPort {

	void deleteAllByUserId(Long userId);
}
