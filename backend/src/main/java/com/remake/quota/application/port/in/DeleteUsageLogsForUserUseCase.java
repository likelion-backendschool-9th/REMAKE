package com.remake.quota.application.port.in;

/** 회원 탈퇴 때 부른다. 그 회원의 생성 횟수 기록(usage_logs)을 모두 지운다. */
public interface DeleteUsageLogsForUserUseCase {

	void deleteUsageLogsForUser(Long userId);
}
